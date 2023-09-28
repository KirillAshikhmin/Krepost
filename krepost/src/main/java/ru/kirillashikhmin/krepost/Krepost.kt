package ru.kirillashikhmin.krepost

import ru.kirillashikhmin.krepost.errorMappers.ErrorMapper
import ru.kirillashikhmin.krepost.cache.KrepostCache
import ru.kirillashikhmin.krepost.internal.FetchType
import ru.kirillashikhmin.krepost.internal.KrepostInvocation
import ru.kirillashikhmin.krepost.serializator.KrepostSerializer


/**
 * TODO:
 * 1. Validator - validate response, should return Ok, Error, Empty (sealed class, contain optional message)
 */

class KrepostDSL {
    var config: KrepostConfig? = null

    var cacher: KrepostCache? = null

    var serializer: KrepostSerializer? = null

    var errorMappers: List<ErrorMapper>? = null
}


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

    /*
    constructor(config: KrepostConfig) : this() {
        setConfig(config.copy())
    }
    constructor(block: KrepostConfig.() -> Unit = {}) : this() {
        val config = KrepostConfig()
        config.block()
        setConfig(config.copy())
    }*/

    fun setConfig(config: KrepostConfig): Krepost {
        this.config = config
        return this
    }

    suspend fun <Dto, Model, ErrorDto, ErrorModel : IKrepostError> fetchDataMappedErrorMapped(block: FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>.() -> Unit): RequestResult<Model> {
        val dsl = FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>().apply(block)
        return KrepostInvocation.fetchDataInvocation(dsl, FetchType.ModelMappedErrorMapped, this)
    }

    suspend fun <Dto, Model, ErrorDto> fetchDataMappedError(block: FetchDataDSL<Dto, Model, ErrorDto, Nothing>.() -> Unit): RequestResult<Model> {
        val dsl = FetchDataDSL<Dto, Model, ErrorDto, Nothing>().apply(block)
        return KrepostInvocation.fetchDataInvocation(dsl, FetchType.ModelMappedError, this)
    }

    suspend fun <Dto, Model> fetchDataMapped(block: FetchDataDSL<Dto, Model, Nothing, Nothing>.() -> Unit): RequestResult<Model> {
        val dsl = FetchDataDSL<Dto, Model, Nothing, Nothing>().apply(block)
        return KrepostInvocation.fetchDataInvocation(dsl, FetchType.ModelMapped, this)
    }

    suspend fun <Dto> fetchData(block: FetchDataDSL<Dto, Dto, Nothing, Nothing>.() -> Unit): RequestResult<Dto> {
        val dsl = FetchDataDSL<Dto, Dto, Nothing, Nothing>().apply(block)
        return KrepostInvocation.fetchDataInvocation(dsl, FetchType.Dto, this)
    }

}
