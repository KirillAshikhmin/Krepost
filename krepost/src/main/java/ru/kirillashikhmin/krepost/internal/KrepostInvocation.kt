package ru.kirillashikhmin.krepost.internal

import android.util.Log
import kotlinx.coroutines.delay
import ru.kirillashikhmin.krepost.CacheStrategy
import ru.kirillashikhmin.krepost.CacheStrategy.Companion.needGetCachedResultBeforeLoad
import ru.kirillashikhmin.krepost.Krepost
import ru.kirillashikhmin.krepost.KrepostCacheException
import ru.kirillashikhmin.krepost.KrepostConfig
import ru.kirillashikhmin.krepost.KrepostRequestException
import ru.kirillashikhmin.krepost.KrepostSerializeException
import ru.kirillashikhmin.krepost.RequestResult
import ru.kirillashikhmin.krepost.RequestStatus
import ru.kirillashikhmin.krepost.ValidateResult
import ru.kirillashikhmin.krepost.cache.CacheResult
import ru.kirillashikhmin.krepost.cache.KrepostCache
import ru.kirillashikhmin.krepost.dsl.CacheDSL
import ru.kirillashikhmin.krepost.dsl.FetchDSL
import ru.kirillashikhmin.krepost.errorMappers.ErrorData
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Suppress("TooGenericExceptionCaught", "TooManyFunctions", "ReturnCount")
object KrepostInvocation {

    suspend inline fun <Dto, reified Model> fetchDataInvocation(
        fetchDsl: FetchDSL<Dto, Model>,
        krepost: Krepost,
    ): RequestResult<Model> {
        return try {
            val modelType = typeOf<Model>()
            invoke(krepost, fetchDsl, modelType)
        } catch (t: Throwable) {
            Log.d("Krepost", "KrepostInternalError top", t)
            returnThrowableRequestResult(t)
        }
    }

    suspend fun <Dto, Model> invoke(
        krepost: Krepost,
        fetchDsl: FetchDSL<Dto, Model>,
        modelType: KType,
    ): RequestResult<Model> = try {
        getCacheResultBeforeLoad(modelType, fetchDsl, krepost)
            ?: invoke(modelType, fetchDsl, krepost)
    } catch (requestException: KrepostRequestException) {
        try {
            getCacheResultAfterException(krepost.cacheManager, modelType, fetchDsl.cache, krepost)
                ?: processRequestException(requestException, fetchDsl, krepost)
        } catch (t: Throwable) {
            returnThrowableRequestResult(t)
        }
    } catch (cacheException: KrepostCacheException) {
        if (cacheException.write.not()) deleteCache(fetchDsl.cache, krepost)
        val exception = cacheException.innerException
        RequestResult.Failure.Error(
            RequestStatus.CacheError,
            message = exception.localizedMessage,
            throwable = exception
        )
    } catch (t: Throwable) {
        Log.d("Krepost", "KrepostInternalError", t)
        returnThrowableRequestResult(t)
    }

    fun <Model> returnThrowableRequestResult(t: Throwable): RequestResult<Model> =
        RequestResult.Failure.Error(
            status = RequestStatus.KrepostInternalError,
            message = t.localizedMessage,
            throwable = t
        )

    @Suppress("UNCHECKED_CAST")
    suspend fun <Dto, Model> invoke(
        modelType: KType,
        fetchDsl: FetchDSL<Dto, Model>,
        krepost: Krepost
    ): RequestResult<Model> {
        val validator = fetchDsl.validator
        val mapper = fetchDsl.mapper
        val cache = fetchDsl.cache
        val cacheManager = krepost.cacheManager

        val isGetResponseFromException = fetchDsl.errorMappersDsl?.mappers?.any() ?: false

        val invokeResult = invokeWithRetry(fetchDsl, krepost, isGetResponseFromException)

        val validationResult = validator?.invoke(invokeResult) ?: ValidateResult.Ok

        if (validationResult is ValidateResult.Empty) {
            return RequestResult.Empty(validationResult.message)
        }

        if (invokeResult == null) {
            deleteCache(cache, krepost)
            return RequestResult.Empty()
        }

        return if (mapper != null) {
            val result = mapper.invoke(invokeResult)
            if (result == null) {
                deleteCache(cache, krepost)
                return RequestResult.Empty()
            }
            writeCacheIfNeeded(modelType, cache, cacheManager, krepost.config, result)
            RequestResult.Success.Value(result as Model)
        } else {
            writeCacheIfNeeded(modelType, cache, cacheManager, krepost.config, invokeResult)
            RequestResult.Success.Value(invokeResult as Model)
        }
    }


    private fun <Dto, Model> processRequestException(
        requestException: KrepostRequestException,
        fetchDsl: FetchDSL<Dto, Model>,
        krepost: Krepost
    ): RequestResult<Model> {

        fun makeKrepostRequestException(requestException: KrepostRequestException): RequestResult.Failure.Error {
            return RequestResult.Failure.Error(
                requestException.errorData.status,
                requestException.errorData.response,
                throwable = requestException.innerException
            )
        }

        val errorStatus = requestException.errorData.status
        val errorResponse = requestException.errorData.response
        val serializer = krepost.serializer ?: return makeKrepostRequestException(requestException)

        if (errorResponse != null) {
            val errorMapper = fetchDsl.getErrorMapper(errorStatus)
                ?: return makeKrepostRequestException(requestException)

            return try {
                if (errorMapper.mapped) {
                    val errorResult = errorMapper.processErrorMapped(errorResponse, serializer)
                    RequestResult.Failure.ErrorWithData(
                        errorResult,
                        errorStatus,
                        throwable = requestException.innerException
                    )
                } else {
                    val errorResult = errorMapper.processError(errorResponse, serializer)
                    RequestResult.Failure.ErrorWithData(
                        errorResult,
                        errorStatus,
                        throwable = requestException.innerException
                    )
                }
            } catch (serializeException: KrepostSerializeException) {
                RequestResult.Failure.Error(
                    RequestStatus.SerializationError,
                    "Unable to serialize error response",
                    serializeException.innerException
                )
            } catch (throwable: Throwable) {
                RequestResult.Failure.Error(
                    RequestStatus.MappingError,
                    "Unable to mapping error response",
                    throwable
                )
            }
        }
        return makeKrepostRequestException(requestException)
    }


    private fun <Dto, Model> getCacheResultBeforeLoad(
        modelType: KType,
        fetchDsl: FetchDSL<Dto, Model>,
        krepost: Krepost
    ): RequestResult<Model>? {
        try {
            val cacheManager = krepost.cacheManager
            val cache = fetchDsl.cache

            if (cache?.invalidate == true) {
                deleteCache(cache, krepost)
                return null
            }
            if (cacheManager != null && cache != null && cache.strategy.needGetCachedResultBeforeLoad) {
                val result = getCacheResult<Model>(cacheManager, modelType, cache, krepost)
                if (result != null) return result
            }
            if (cache?.strategy == CacheStrategy.Only) {
                return RequestResult.Empty()
            }
            return null
        } catch (cacheException: KrepostCacheException) {
            if (cacheException.write.not()) deleteCache(fetchDsl.cache, krepost)
            Log.d("Krepost", "KrepostCacheException", cacheException)
        }
        return null
    }


    private fun <Model> getCacheResultAfterException(
        cacheManager: KrepostCache?,
        modelType: KType,
        cache: CacheDSL?,
        krepost: Krepost
    ): RequestResult<Model>? {
        return if (cache != null && cacheManager != null && cache.strategy == CacheStrategy.IfNotAvailable) {
            try {
                getCacheResult(cacheManager, modelType, cache, krepost)
            } catch (cacheException: KrepostCacheException) {
                if (cacheException.write.not()) deleteCache(cache, krepost)
                Log.d("Krepost", "KrepostCacheException", cacheException)
                null
            }
        } else null
    }

    private fun <Model> getCacheResult(
        cacheManager: KrepostCache,
        modelType: KType,
        cache: CacheDSL,
        krepost: Krepost
    ): RequestResult<Model>? {
        val cacheResult: CacheResult<Model> = cacheManager.get(
            modelType,
            key = cache.name,
            keyArguments = cache.arguments?.joinToString().hashCode().toString(),
            cacheTime = cache.cacheTimeMilliseconds ?: krepost.config.cacheTimeMilliseconds,
            deleteIfOutdated = cache.deleteIfOutdated ?: krepost.config.deleteCacheIfOutdated
        )
        val data = cacheResult.value
        return if (data != null) {
            if (cacheResult.outdated)
                RequestResult.Outdated(data, cacheResult.cacheTime)
            else
                RequestResult.Cached.Value(data, cacheResult.cacheTime)

        } else null
    }

    private fun deleteCache(
        cache: CacheDSL?,
        krepost: Krepost
    ) {
        if (cache == null) return
        val cacheManager = krepost.cacheManager ?: return
        val arguments = getCacheKeyArguments(cache)
        try {
            cacheManager.delete(cache.name, arguments)
        } catch (t: Throwable) {
            Log.d("Krepost", "Unable to delete cache: ${cache.name}#$arguments", t)
        }
    }

    private fun <T : Any> writeCacheIfNeeded(
        modelType: KType,
        cache: CacheDSL?,
        cacheManager: KrepostCache?,
        config: KrepostConfig,
        result: T?
    ) {
        if (cache == null || cacheManager == null || cache.strategy == CacheStrategy.Never) return
        try {
            val cacheKeyArguments = getCacheKeyArguments(cache)
            val cacheTime = cache.cacheTimeMilliseconds ?: config.cacheTimeMilliseconds
            if (result != null && cacheTime > 0L) {
                cacheManager.write(
                    modelType,
                    key = cache.name,
                    keyArguments = cacheKeyArguments,
                    data = result
                )
            }
        } catch (t: Throwable) {
            throw KrepostCacheException(t, write = true)
        }
    }

    private fun getCacheKeyArguments(cache: CacheDSL?) =
        cache?.arguments?.joinToString().hashCode().toString()

    private suspend fun <Dto, Model> invokeWithRetry(
        fetchDsl: FetchDSL<Dto, Model>,
        krepost: Krepost,
        isGetResponseFromException: Boolean
    ): Dto? {
        val retryCount = fetchDsl.config?.retryCount ?: krepost.config.retryCount
        val retryDelay =
            fetchDsl.config?.retryDelayMilliseconds ?: krepost.config.retryDelayMilliseconds
        val noRetryStatuses = fetchDsl.config?.noRetryStatuses ?: krepost.config.noRetryStatuses

        var exception: Throwable?
        var result: Dto? = null
        var retryRemained = retryCount
        var errorData: ErrorData? = null
        do {
            try {
                result = fetchDsl.action()
                exception = null
            } catch (e: Throwable) {
                exception = e
                errorData = statusFromException(krepost, e, isGetResponseFromException)
                if (noRetryStatuses.contains(errorData.status))
                    retryRemained = 0
                else delay(retryDelay)
            }
            retryRemained--
        } while (exception != null && retryRemained > 0)

        if (exception != null && errorData != null) {
            throw KrepostRequestException(exception, errorData)
        }

        return result
    }

    private fun statusFromException(
        krepost: Krepost,
        e: Throwable,
        isGetResponseFromException: Boolean
    ): ErrorData = krepost.errorMappers?.firstNotNullOfOrNull {
        it.getErrorDataFromThrowable(e, isGetResponseFromException)
    }
        ?: ErrorData(RequestStatus.Unknown, null)
}
