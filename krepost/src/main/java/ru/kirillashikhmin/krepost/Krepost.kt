package ru.kirillashikhmin.krepost

import ru.kirillashikhmin.krepost.errorMappers.ErrorMapper
import ru.kirillashikhmin.krepost.cache.KrepostCache
import ru.kirillashikhmin.krepost.internal.FetchType
import ru.kirillashikhmin.krepost.internal.KrepostInvocation
import ru.kirillashikhmin.krepost.serializator.KrepostSerializer


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

    suspend fun <Dto, Model, ErrorDto : Any, ErrorModel : IKrepostError> fetchDataMappedAndErrorMapped(
        block: FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>.() -> Unit
    ): RequestResult<Model> {
        val dsl = FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>().apply(block)
        return KrepostInvocation.fetchDataInvocation(dsl, FetchType.ModelMappedErrorMapped, this)
    }

    suspend fun <Dto, Model, ErrorDto : Any> fetchDataMappedAndError(
        block: FetchDataDSL<Dto, Model, ErrorDto, Nothing>.() -> Unit
    ): RequestResult<Model> {
        val dsl = FetchDataDSL<Dto, Model, ErrorDto, Nothing>().apply(block)
        return KrepostInvocation.fetchDataInvocation(dsl, FetchType.ModelMappedError, this)
    }

    suspend fun <Dto, Model> fetchDataMapped(
        block: FetchDataDSL<Dto, Model, Nothing, Nothing>.() -> Unit
    ): RequestResult<Model> {
        val dsl = FetchDataDSL<Dto, Model, Nothing, Nothing>().apply(block)
        return KrepostInvocation.fetchDataInvocation(dsl, FetchType.ModelMapped, this)
    }

    suspend fun <Dto> fetchData(
        block: FetchDataDSL<Dto, Dto, Nothing, Nothing>.() -> Unit
    ): RequestResult<Dto> {
        val dsl = FetchDataDSL<Dto, Dto, Nothing, Nothing>().apply(block)
        return KrepostInvocation.fetchDataInvocation(dsl, FetchType.Dto, this)
    }

}
