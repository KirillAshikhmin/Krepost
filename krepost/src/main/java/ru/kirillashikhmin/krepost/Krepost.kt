package ru.kirillashikhmin.krepost

import ru.kirillashikhmin.krepost.internal.FetchType
import ru.kirillashikhmin.krepost.internal.KrepostInvocation


class KrepostDSL {
    var config : KrepostConfig = KrepostConfig()

    var name: String? = null
    var strategy: CacheStrategy? = null

    private var arguments: List<String>? = null

    fun arguments(vararg arguments: Any?) {
        this.arguments = arguments.mapNotNull { it?.toString() }
    }
}


class Krepost private constructor() {

    private lateinit var config: KrepostConfig

    companion object {
        fun initialize(config: KrepostConfig) : Krepost {
            return Krepost().setConfig(config)
        }

        fun initialize(block: KrepostDSL.()->Unit) : Krepost {
            val dsl = KrepostDSL()
            dsl.block()
            return Krepost()
        }

    }



    fun setConfig(config : KrepostConfig): Krepost {
        this.config = config
        return this
    }

    suspend fun <Dto, Model, ErrorDto, ErrorModel : IKrepostError> fetchDataMappedErrorMapped(block: FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>.() -> Unit): RequestResult<Model> {
        val dsl = FetchDataDSL<Dto, Model, ErrorDto, ErrorModel>().apply(block)
        return KrepostInvocation.fetchDataInvocation(dsl, FetchType.ModelMappedErrorMapped, config)
    }

    suspend fun <Dto, Model, ErrorDto> fetchDataMappedError(block: FetchDataDSL<Dto, Model, ErrorDto, Nothing>.() -> Unit): RequestResult<Model> {
        val dsl = FetchDataDSL<Dto, Model, ErrorDto, Nothing>().apply(block)
        return KrepostInvocation.fetchDataInvocation(dsl, FetchType.ModelMappedError, config)
    }

    suspend fun <Dto, Model> fetchDataMapped(block: FetchDataDSL<Dto, Model, Nothing, Nothing>.() -> Unit): RequestResult<Model> {
        val dsl = FetchDataDSL<Dto, Model, Nothing, Nothing>().apply(block)
        return KrepostInvocation.fetchDataInvocation(dsl, FetchType.ModelMapped, config)
    }

    suspend fun <Dto> fetchData(block: FetchDataDSL<Dto, Dto, Nothing, Nothing>.() -> Unit): RequestResult<Dto> {
        val dsl = FetchDataDSL<Dto, Dto, Nothing, Nothing>().apply(block)
        return KrepostInvocation.fetchDataInvocation(dsl, FetchType.Dto, config)
    }

}
