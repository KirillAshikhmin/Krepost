package ru.kirillashikhmin.krepost

import ru.kirillashikhmin.krepost.interfaces.IKrepostCache
import ru.kirillashikhmin.krepost.internal.FetchType
import ru.kirillashikhmin.krepost.internal.KrepostInvocation


class KrepostDSL {
    var config: KrepostConfig? = null

    var cacher: IKrepostCache? = null

    private var arguments: List<String>? = null

    fun arguments(vararg arguments: Any?) {
        this.arguments = arguments.mapNotNull { it?.toString() }
    }
}


class Krepost(blockDsl: KrepostDSL.() -> Unit = {}) {

    var config: KrepostConfig
    var cacheManager: IKrepostCache? = null

    init {
        val dsl = KrepostDSL().apply(blockDsl)
        config = dsl.config ?: KrepostConfig()
        cacheManager = dsl.cacher
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
