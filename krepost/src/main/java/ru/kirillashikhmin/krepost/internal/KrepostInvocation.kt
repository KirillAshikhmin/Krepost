package ru.kirillashikhmin.krepost.internal

import ru.kirillashikhmin.krepost.CacheStrategy.Companion.needGetCachedResultBeforeLoad
import ru.kirillashikhmin.krepost.FetchDataDSL
import ru.kirillashikhmin.krepost.IKrepostError
import ru.kirillashikhmin.krepost.Krepost
import ru.kirillashikhmin.krepost.RequestResult
import ru.kirillashikhmin.krepost.RequestStatus

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
            val cacheResult : Pair<Model?, Long> = cacheManager.get(
                key = cache.name,
                keyArguments = cache.arguments?.joinToString().hashCode().toString(),
                cacheTime = cache.cacheTimeMilliseconds ?: krepost.config.cacheTimeMilliseconds,
                deleteIfOutdated = cache.deleteIfOutdated ?: krepost.config.deleteCacheIfOutdated
            )
            val data = cacheResult.first
            if (data != null)
                return RequestResult.Cached.Value(data, cacheResult.second)
        }


        try {

        } catch (t: Throwable) {

        }
        val data = fetchDsl.action()

        @Suppress("UNCHECKED_CAST")
        val model =
            if (type == FetchType.Dto)
                data as Model
            else
                fetchDsl.mapper?.invoke(data)


        @Suppress("UNCHECKED_CAST")
//        val mappedResult = mapper?.invoke(data) ?: (data as Model)
        return RequestResult.Failure.Error(RequestStatus.BadRequest)//.Success.Value<Model>(mappedResult)
    }

}


