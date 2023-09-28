package ru.kirillashikhmin.krepost.internal

import kotlinx.coroutines.delay
import ru.kirillashikhmin.krepost.CacheStrategy.Companion.needGetCachedResultBeforeLoad
import ru.kirillashikhmin.krepost.FetchDataCacheDSL
import ru.kirillashikhmin.krepost.FetchDataDSL
import ru.kirillashikhmin.krepost.IKrepostError
import ru.kirillashikhmin.krepost.Krepost
import ru.kirillashikhmin.krepost.KrepostRequestException
import ru.kirillashikhmin.krepost.RequestResult
import ru.kirillashikhmin.krepost.RequestStatus
import ru.kirillashikhmin.krepost.ValidateResult
import ru.kirillashikhmin.krepost.cache.KrepostCache

object KrepostInvocation {

    suspend fun <Dto, Model, ErrorDto, ErrorModel : IKrepostError> fetchDataInvocation(
        fetchDsl: FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>,
        type: FetchType,
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

        val validator = fetchDsl.validator
        val mapper = fetchDsl.mapper

        try {

            val invokeResult = invokeWithRetry(fetchDsl, krepost)


            val validationResult = validator?.invoke(invokeResult) ?: ValidateResult.Ok

            if (validationResult is ValidateResult.Empty) {
                return RequestResult.EmptyResult(validationResult.message)
            }
            if (invokeResult == null) {
                return RequestResult.EmptyResult()
            }

            val result = mapper?.invoke(invokeResult) ?: invokeResult

            val cacheKeyArguments = getCacheKeyArguments(cache)

            if (result == null) {
                if (cache != null) cacheManager?.delete(cache.name, cacheKeyArguments)
                return RequestResult.EmptyResult()
            }

            val cacheTime = cache?.cacheTimeMilliseconds
            if (result != null && cacheManager != null && cacheTime != null && cacheTime > 0L) {
                cacheManager.write(
                    key = cache.name,
                    keyArguments = cacheKeyArguments,
                    data = result
                )
            }

            @Suppress("UNCHECKED_CAST")
            return RequestResult.Success.Value(result as Model)

        } catch (requestException: KrepostRequestException) {
            if (cache != null) cacheManager?.delete(cache.name, getCacheKeyArguments(cache))
            val status = requestException.status ?: RequestStatus.UnknownRequestError

            return RequestResult.Failure.Error(status)
        } catch (t: Throwable) {
            return RequestResult.Failure.Error(
                status = RequestStatus.KrepostInternalError,
                message = t.localizedMessage,
                throwable = t
            )
        }
    }

    private fun getCacheKeyArguments(cache: FetchDataCacheDSL?) =
        cache?.arguments?.joinToString().hashCode().toString()

    private suspend fun <Dto, Model, ErrorDto, ErrorModel : IKrepostError> invokeWithRetry(
        fetchDsl: FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>,
        krepost: Krepost,
    ): Dto? {
        val retryCount = fetchDsl.config?.retryCount ?: krepost.config.retryCount
        val retryDelay =
            fetchDsl.config?.retryDelayMilliseconds ?: krepost.config.retryDelayMilliseconds
        val noRetryStatuses = fetchDsl.config?.noRetryStatuses ?: krepost.config.noRetryStatuses

        var exception: Throwable?
        var result: Dto? = null
        var retryRemained = retryCount
        var status: RequestStatus? = null
        do {
            try {
                result = fetchDsl.action()
                exception = null
            } catch (e: Throwable) {
                exception = e
                status = statusFromException(krepost, e)
                if (noRetryStatuses.contains(status))
                    retryRemained = 0
                else delay(retryDelay)
            }
            retryRemained--
        } while (exception != null && retryRemained > 0)

        if (exception != null) {
            throw KrepostRequestException(exception, status)
        }

        return result
    }

    private fun statusFromException(
        krepost: Krepost,
        e: Throwable,
    ) = (krepost.errorMappers?.firstNotNullOf { it.getErrorDataFromThrowable(e) }
        ?: RequestStatus.UnknownRequestError)

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
