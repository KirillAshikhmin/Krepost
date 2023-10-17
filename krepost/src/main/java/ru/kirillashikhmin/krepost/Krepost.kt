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

    suspend inline fun <reified Dto, reified ErrorDto> fetchWithError(
        block: FetchDataDSL<Dto, Dto, ErrorDto, Nothing>.() -> Unit
    ): RequestResult<Dto> {
        val dsl = FetchDataDSL<Dto, Dto, ErrorDto, Nothing>().apply(block)
        return KrepostInvocation.fetchDataInvocation(dsl, FetchType(error = true), this)
    }

    suspend inline fun <reified Dto, reified ErrorDto, ErrorModel : IKrepostError> fetchWithErrorMapped(
        block: FetchDataDSL<Dto, Dto, ErrorDto, ErrorModel>.() -> Unit
    ): RequestResult<Dto> {
        val dsl = FetchDataDSL<Dto, Dto, ErrorDto, ErrorModel>().apply(block)
        return KrepostInvocation.fetchDataInvocation(
            dsl, FetchType(error = true, errorMapped = true), this
        )
    }

    suspend inline fun <Dto, reified Model, reified ErrorDto, ErrorModel : IKrepostError> fetchDataMappedAndErrorMapped(
        block: FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>.() -> Unit
    ): RequestResult<Model> {
        val dsl = FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>().apply(block)
        return KrepostInvocation.fetchDataInvocation(
            dsl, FetchType(mapped = true, error = true, errorMapped = true), this
        )
    }

    suspend inline fun <Dto, reified Model, reified ErrorDto> fetchDataMappedAndError(
        block: FetchDataDSL<Dto, Model, ErrorDto, Nothing>.() -> Unit
    ): RequestResult<Model> {
        val dsl = FetchDataDSL<Dto, Model, ErrorDto, Nothing>().apply(block)
        return KrepostInvocation.fetchDataInvocation(
            dsl, FetchType(mapped = true, error = true, errorMapped = false), this
        )
    }

    suspend inline fun <Dto, reified Model> fetchDataMapped(
        block: FetchDataDSL<Dto, Model, Any, Nothing>.() -> Unit
    ): RequestResult<Model> {
        val dsl = FetchDataDSL<Dto, Model, Any, Nothing>().apply(block)
        return KrepostInvocation.fetchDataInvocation(
            dsl, FetchType(mapped = true, error = false, errorMapped = false), this
        )
    }

    suspend inline fun <reified Dto> fetchData(
        block: FetchDataDSL<Dto, Dto, Any, Nothing>.() -> Unit
    ): RequestResult<Dto> {
        val dsl = FetchDataDSL<Dto, Dto, Any, Nothing>().apply(block)
        return KrepostInvocation.fetchDataInvocation(
            dsl, FetchType(mapped = false, error = false, errorMapped = false), this
        )
    }

}
