package ru.kirillashikhmin.krepost.internal

import ru.kirillashikhmin.krepost.FetchDataDSL
import ru.kirillashikhmin.krepost.IKrepostError
import ru.kirillashikhmin.krepost.KrepostConfig
import ru.kirillashikhmin.krepost.RequestResult

object KrepostInvocation {

    suspend fun <Dto, Model, ErrorDto, ErrorModel : IKrepostError> fetchDataInvocation(
        fetchDsl: FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>,
        type: FetchType,
        config: KrepostConfig,
    ): RequestResult<Model> {

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


        try {

        } catch (t : Throwable) {

        }
        val data = fetchDsl.action()

        @Suppress("UNCHECKED_CAST")
        val model =
            if (type == FetchType.Dto)
                data as Model
            else
                fetchDsl.mapper?.invoke(data)



        @Suppress("UNCHECKED_CAST")
        val mappedResult = mapper?.invoke(data) ?: (data as Model)
        return RequestResult.Success.Value<Model>(mappedResult)
    }

}


