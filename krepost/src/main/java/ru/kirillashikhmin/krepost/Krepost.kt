package ru.kirillashikhmin.krepost

import ru.kirillashikhmin.krepost.cache.KrepostCache
import ru.kirillashikhmin.krepost.dsl.FetchDSL
import ru.kirillashikhmin.krepost.errorMappers.ErrorMapper
import ru.kirillashikhmin.krepost.internal.KrepostInvocation
import ru.kirillashikhmin.krepost.serializers.KrepostSerializer


class KrepostDSL {
    var config: KrepostConfig? = null

    var cacher: KrepostCache? = null

    var serializer: KrepostSerializer? = null

    var errorMappers: List<ErrorMapper>? = null
}


@Suppress("unused")
class Krepost(blockDsl: KrepostDSL.() -> Unit = {}) {

    internal var config: KrepostConfig
    internal var cacheManager: KrepostCache? = null
    internal var serializer: KrepostSerializer? = null
    internal var errorMappers: List<ErrorMapper>? = null

    init {
        RequestStatus.initialize()
        val dsl = KrepostDSL().apply(blockDsl)
        config = dsl.config ?: KrepostConfig()
        cacheManager = dsl.cacher
        serializer = dsl.serializer
        errorMappers = dsl.errorMappers
    }

    fun setConfig(config: KrepostConfig): Krepost {
        this.config = config
        return this
    }

    suspend inline fun <Dto, reified Model> fetchMapped(
        block: FetchDSL<Dto, Model>.() -> Unit
    ): RequestResult<Model> {
        val dsl = FetchDSL<Dto, Model>().apply(block)
        return KrepostInvocation.fetchDataInvocation(dsl, this)
    }

    suspend inline fun <reified Dto> fetch(
        block: FetchDSL<Dto, Dto>.() -> Unit
    ): RequestResult<Dto> {
        val dsl = FetchDSL<Dto, Dto>().apply(block)
        return KrepostInvocation.fetchDataInvocation(dsl, this)
    }

}
