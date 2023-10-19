package ru.kirillashikhmin.krepost.dsl

import ru.kirillashikhmin.krepost.IKrepostError
import ru.kirillashikhmin.krepost.KrepostConfig
import ru.kirillashikhmin.krepost.RequestStatus
import ru.kirillashikhmin.krepost.ValidateResult

/**
 * Sample:
 * ```
 * val result = fetchData {
action { myApi.getData() }
cacheName("myCache")
cacheDate(Date())
mapper { data -> data.filter { it.isActive } }
}
```
 */

@Suppress("unused")
class FetchDSL<Dto, Model> {
    lateinit var action: suspend () -> Dto
    internal var cache: CacheDSL? = null
    internal var errorMappersDsl: ErrorMappersDsl? = null
    internal var mapper: ((Dto) -> Model)? = null
    internal var validator: ((Dto?) -> ValidateResult)? = null

    var config: KrepostConfig? = null

    fun action(block: suspend () -> Dto) {
        action = block
    }

    fun cache(name: String, cacheDsl: CacheDSL.() -> Unit) {
        val cache = CacheDSL(name)
        cache.cacheDsl()
        this.cache = cache
    }

    fun mapper(block: (Dto) -> Model) {
        mapper = block
    }

    fun validator(block: (Dto?) -> ValidateResult) {
        validator = block
    }

    inline fun <reified ErrorDto, ErrorModel : IKrepostError> errorMapper(noinline block: (ErrorDto) -> ErrorModel) {
        errorMappers { any(block) }
    }

    inline fun <reified ErrorDto> errorMapper() {
        errorMappers { any<ErrorDto>() }
    }

    fun errorMappers(cacheMappersDsl: ErrorMappersDsl.() -> Unit) {
        require(errorMappersDsl == null)
        { "You're should call only one function: errorMapper or errorMappers" }

        val errorMappersDsl = ErrorMappersDsl()
        errorMappersDsl.cacheMappersDsl()
        this.errorMappersDsl = errorMappersDsl
    }

    internal fun getErrorMapper(errorStatus: RequestStatus): ErrorMappersDsl.IErrorMapper<*, *>? {
        return errorMappersDsl?.mappers?.firstOrNull { it.status == errorStatus }
            ?: errorMappersDsl?.mappers?.firstOrNull { it.status == RequestStatus.Any }
    }
}

