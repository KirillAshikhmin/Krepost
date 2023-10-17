package ru.kirillashikhmin.krepost.internal

import android.util.Log
import kotlinx.coroutines.delay
import ru.kirillashikhmin.krepost.CacheStrategy
import ru.kirillashikhmin.krepost.CacheStrategy.Companion.needGetCachedResultBeforeLoad
import ru.kirillashikhmin.krepost.FetchDataCacheDSL
import ru.kirillashikhmin.krepost.FetchDataDSL
import ru.kirillashikhmin.krepost.IKrepostError
import ru.kirillashikhmin.krepost.Krepost
import ru.kirillashikhmin.krepost.KrepostCacheException
import ru.kirillashikhmin.krepost.KrepostConfig
import ru.kirillashikhmin.krepost.KrepostRequestException
import ru.kirillashikhmin.krepost.RequestResult
import ru.kirillashikhmin.krepost.RequestStatus
import ru.kirillashikhmin.krepost.ValidateResult
import ru.kirillashikhmin.krepost.cache.CacheResult
import ru.kirillashikhmin.krepost.cache.KrepostCache
import ru.kirillashikhmin.krepost.errorMappers.ErrorData
import kotlin.reflect.KType
import kotlin.reflect.typeOf


@Suppress("TooGenericExceptionCaught", "TooManyFunctions", "ReturnCount")
object KrepostInvocation {

    suspend inline fun <Dto, reified Model, reified ErrorDto, ErrorModel : IKrepostError> fetchDataInvocation(
        fetchDsl: FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>,
        fetchType: FetchType,
        krepost: Krepost,
    ): RequestResult<Model> {
        val modelType = typeOf<Model>()
        val errorDtoType = typeOf<ErrorDto>()
        return invoke(krepost, fetchDsl, modelType, fetchType, errorDtoType)
    }

    suspend fun <Dto, Model, ErrorDto, ErrorModel : IKrepostError> invoke(
        krepost: Krepost,
        fetchDsl: FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>,
        modelType: KType,
        fetchType: FetchType,
        errorDtoType: KType
    ): RequestResult<Model> = try {
        val result =
            getCacheResultBeforeLoad(modelType, fetchDsl, krepost)
                ?: invoke(modelType, fetchType, fetchDsl, krepost)
        result
    } catch (requestException: KrepostRequestException) {
        try {
            val cache = fetchDsl.cache
            val cacheManager = krepost.cacheManager
            val cachedResult =
                if (cache != null && cacheManager != null && cache.strategy == CacheStrategy.IfNotAvailable) {
                    getCacheResult<Model>(cacheManager, modelType, cache, krepost)
                } else null
            cachedResult ?: processRequestException(
                requestException,
                errorDtoType,
                fetchType,
                fetchDsl,
                krepost
            )
        } catch (t: Throwable) {
            Log.d("Krepost", "KrepostInternalError", t)
            RequestResult.Empty(t.localizedMessage)
        }
    } catch (cacheException: KrepostCacheException) {
        try {
            if (cacheException.write.not()) deleteCache(fetchDsl.cache, krepost)
            val exception = cacheException.innerException
            RequestResult.Failure.Error(
                RequestStatus.CacheError,
                message = exception.localizedMessage,
                throwable = exception
            )
        } catch (t: Throwable) {
            Log.d("Krepost", "KrepostInternalError", t)
            RequestResult.Empty(t.localizedMessage)
        }
    } catch (t: Throwable) {
        Log.d("Krepost", "KrepostInternalError", t)
        RequestResult.Failure.Error(
            status = RequestStatus.KrepostInternalError,
            message = t.localizedMessage,
            throwable = t
        )
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <Dto, Model, ErrorDto, ErrorModel : IKrepostError> invoke(
        modelType: KType,
        fetchType: FetchType,
        fetchDsl: FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>,
        krepost: Krepost
    ): RequestResult<Model> {
        val validator = fetchDsl.validator
        val mapper = fetchDsl.mapper
        val cache = fetchDsl.cache
        val cacheManager = krepost.cacheManager

        val isGetResponseFromException = fetchType.error

        val invokeResult = invokeWithRetry(fetchDsl, krepost, isGetResponseFromException)

        val validationResult = validator?.invoke(invokeResult) ?: ValidateResult.Ok

        if (validationResult is ValidateResult.Empty) {
            return RequestResult.Empty(validationResult.message)
        }

        val cacheKeyArguments = getCacheKeyArguments(cache)

        if (invokeResult == null) {
            if (cache != null) cacheManager?.delete(cache.name, cacheKeyArguments)
            return RequestResult.Empty()
        }

        return if (fetchType.mapped) {
            val result = mapper?.invoke(invokeResult)
            if (result == null) {
                if (cache != null) cacheManager?.delete(cache.name, cacheKeyArguments)
                return RequestResult.Empty()
            }
            writeCacheIfNeeded(modelType, cache, cacheManager, krepost.config, result)
            RequestResult.Success.Value(result as Model)
        } else {
            writeCacheIfNeeded(modelType, cache, cacheManager, krepost.config, invokeResult)
            RequestResult.Success.Value(invokeResult as Model)
        }
    }

    private fun <Dto, Model, ErrorDto, ErrorModel : IKrepostError> processRequestException(
        requestException: KrepostRequestException,
        errorDtoType: KType,
        fetchType: FetchType,
        fetchDsl: FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>,
        krepost: Krepost
    ): RequestResult<Model> {
        val errorData = requestException.errorData
        val errorResponse = requestException.errorData.response
        if (errorResponse != null) {
            val error = try {
                krepost.serializer?.deserialize<ErrorDto>(errorResponse, errorDtoType)
            } catch (t: Throwable) {
                return RequestResult.Failure.Error(
                    RequestStatus.SerializationError,
                    "Unable to deserialize error response",
                    t
                )
            }
            val result = returnErrorRequest(
                fetchDsl,
                error,
                fetchType,
                errorData,
                requestException.innerException
            )
            if (result != null) return result
        }
        return RequestResult.Failure.Error(
            errorData.status,
            throwable = requestException.innerException
        )
    }

    private fun <Dto, Model, ErrorDto, ErrorModel : IKrepostError> returnErrorRequest(
        fetchDsl: FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>,
        error: ErrorDto?,
        fetchType: FetchType,
        errorData: ErrorData,
        innerException: Throwable
    ): RequestResult<Model>? {
        if (error == null) return null

        if (fetchType.errorMapped) {
            return try {
                val mapperError = fetchDsl.errorMapper?.invoke(error)
                RequestResult.Failure.ErrorWithData(
                    mapperError as ErrorModel,
                    errorData.status,
                    mapperError.errorMessage,
                    innerException
                )
            } catch (t: Throwable) {
                RequestResult.Failure.Error(
                    RequestStatus.MappingError,
                    "Unable to mapping error response",
                    t
                )
            }
        } else {
            @Suppress("UNCHECKED_CAST")
            RequestResult.Failure.ErrorWithData(
                error as ErrorDto,
                errorData.status,
                throwable = innerException
            )
        }
        return null
    }

    private fun <Dto, Model, ErrorDto, ErrorModel : IKrepostError> getCacheResultBeforeLoad(
        modelType: KType,
        fetchDsl: FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>,
        krepost: Krepost
    ): RequestResult<Model>? {
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
    }

    private fun <Model> getCacheResult(
        cacheManager: KrepostCache,
        modelType: KType,
        cache: FetchDataCacheDSL,
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
        cache: FetchDataCacheDSL?,
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
        cache: FetchDataCacheDSL?,
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

    private fun getCacheKeyArguments(cache: FetchDataCacheDSL?) =
        cache?.arguments?.joinToString().hashCode().toString()

    private suspend fun <Dto, Model, ErrorDto, ErrorModel : IKrepostError> invokeWithRetry(
        fetchDsl: FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>,
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
