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
import ru.kirillashikhmin.krepost.KrepostRequestException
import ru.kirillashikhmin.krepost.RequestResult
import ru.kirillashikhmin.krepost.RequestStatus
import ru.kirillashikhmin.krepost.ValidateResult
import ru.kirillashikhmin.krepost.cache.KrepostCache
import ru.kirillashikhmin.krepost.errorMappers.ErrorData

object KrepostInvocation {

    suspend fun <Dto, Model, ErrorDto : Any, ErrorModel : IKrepostError> fetchDataInvocation(
        fetchDsl: FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>,
        fetchType: FetchType,
        krepost: Krepost,
    ): RequestResult<Model> {


        /*
         val useCache = ConfigUtils.isUseCache(config, cache)
        if (useCache && cache?.strategy == CacheStrategyVariant.Priority) {
            val cacheResult = cacheImpl?.get(
                cache.cacheName,
                cache.arguments,
                cache.cacheTime,
                cache.deleteIfOutdated ?: true
            )
            if (cacheResult?.first != null)
                return RequestResult.Cached.Value(cacheResult.first, cacheResult.second)
        }

        return try {

            val invokeResult = invokeWithRetry(request, config, retry)

            @Suppress("IfThenToElvis")
            val result = if (converter == null) invokeResult else converter.invoke(invokeResult)

            if (result != null && useCache && cache?.cacheTime != null && cache.cacheTime > 0) {
                cacheImpl?.write(
                    cache.cacheName,
                    cache.arguments,
                    result
                )
            }
            RequestResult.Success.Value(result)
        } catch (e: Throwable) {
            if (useCache) {
                val status = statusFromException(e)
                if (cache != null && status == RequestStatus.Unauthorized) {
                    cacheImpl?.delete(cache.cacheName, cache.arguments)
                } else {
                    if (cache?.strategy == CacheStrategyVariant.IfNotAvailable) {
                        val cacheResult = cacheImpl?.get(
                            cache.cacheName,
                            cache.arguments,
                            cache.cacheTime,
                            cache.deleteIfOutdated ?: true
                        )

                        if (cacheResult?.first != null)
                            return RequestResult.Cached.Value(cacheResult.first, cacheResult.second)
                    }
                }
            }
            val errorResult = if (e is HttpException) {

                if (errorConverter != null)
                    getErrorResult(e, errorConverter)
                else
                    getErrorResult(e)
            } else {
                getErrorResult(e)
            }
            if (errorResult.status == RequestStatus.NotFound && useCache && cache != null)
                cacheImpl?.delete(cache.cacheName, cache.arguments)
            return errorResult
        }
         */


        val cacheManager = krepost.cacheManager
        val cache = fetchDsl.cache
        if (cacheManager != null && cache != null && cache.strategy.needGetCachedResultBeforeLoad) {
            val cacheResult: Pair<Model?, Long> = getCache<Model>(cacheManager, cache, krepost)
            val data = cacheResult.first
            if (data != null)
                return RequestResult.Cached.Value(data, cacheResult.second)
        }
        if (cache?.strategy == CacheStrategy.Only) {
            return RequestResult.Empty()
        }

        val validator = fetchDsl.validator
        val mapper = fetchDsl.mapper

        try {

            val isGetResponseFromException = fetchType.type >= FetchType.ModelMappedError.type

            val invokeResult = invokeWithRetry(fetchDsl, krepost, isGetResponseFromException)


            val validationResult = validator?.invoke(invokeResult) ?: ValidateResult.Ok

            if (validationResult is ValidateResult.Empty) {
                return RequestResult.Empty(validationResult.message)
            }
            if (invokeResult == null) {
                return RequestResult.Empty()
            }

            val result = if (fetchType.type >= FetchType.ModelMapped.type) {
                mapper?.invoke(invokeResult) ?: invokeResult
            } else invokeResult

            val cacheKeyArguments = getCacheKeyArguments(cache)

            if (result == null) {
                if (cache != null) cacheManager?.delete(cache.name, cacheKeyArguments)
                return RequestResult.Empty()
            }

            writeCacheIfNeeded(cache, cacheManager, result)

            @Suppress("UNCHECKED_CAST")
            return RequestResult.Success.Value(result as Model)

        } catch (requestException: KrepostRequestException) {
            val errorData = requestException.errorData
            val errorResponse = errorData.response
            if (errorResponse != null) {
                val error = try {
                    krepost.serializer?.deserialize<ErrorDto>(errorResponse)
                } catch (t: Throwable) {
                    return RequestResult.Failure.Error(
                        RequestStatus.SerializationError,
                        "Unable to deserialize error response",
                        t
                    )
                }
                if (error != null) {
                    if (fetchType.type >= FetchType.ModelMappedErrorMapped.type) {
                        return try {
                            val mapperError = fetchDsl.errorMapper?.invoke(error)
                            RequestResult.Failure.ErrorWithData(
                                mapperError as ErrorModel,
                                errorData.status
                            )
                        } catch (t: Throwable) {
                            RequestResult.Failure.Error(
                                RequestStatus.MappingError,
                                "Unable to mapping error response",
                                t
                            )
                        }
                    }
                }
            }
            return RequestResult.Failure.Error(errorData.status)
        } catch (cacheException: KrepostCacheException) {
            if (cacheException.write.not()) deleteCache(cacheManager, cache)
            return RequestResult.Failure.Error(RequestStatus.CacheError)
        } catch (t: Throwable) {
            Log.d("Krepost", "KrepostInternalError", t)
            return RequestResult.Failure.Error(
                status = RequestStatus.KrepostInternalError,
                message = t.localizedMessage,
                throwable = t
            )
        }
    }

    private fun deleteCache(cacheManager: KrepostCache?, cache: FetchDataCacheDSL?) {
        if (cache != null) {
            val arguments = getCacheKeyArguments(cache)
            try {
                cacheManager?.delete(cache.name, arguments)
            } catch (t: Throwable) {
                Log.d("Krepost", "Unable to delete cache: ${cache.name}#$arguments", t)
            }
        }
    }

    private fun <T : Any> writeCacheIfNeeded(
        cache: FetchDataCacheDSL?,
        cacheManager: KrepostCache?,
        result: T?,
    ) {
        if (cache == null || cacheManager == null || cache.strategy != CacheStrategy.Never) return
        try {
            val cacheKeyArguments = getCacheKeyArguments(cache)
            val cacheTime = cache.cacheTimeMilliseconds
            if (result != null && cacheTime != null && cacheTime > 0L) {
                cacheManager.write(
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

    private fun <Model> getCache(
        cacheManager: KrepostCache,
        cache: FetchDataCacheDSL,
        krepost: Krepost,
    ): Pair<Model?, Long> = cacheManager.get(
        key = cache.name,
        keyArguments = cache.arguments?.joinToString().hashCode().toString(),
        cacheTime = cache.cacheTimeMilliseconds ?: krepost.config.cacheTimeMilliseconds,
        deleteIfOutdated = cache.deleteIfOutdated ?: krepost.config.deleteCacheIfOutdated
    )

}
