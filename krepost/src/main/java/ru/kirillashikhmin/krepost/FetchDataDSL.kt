package ru.kirillashikhmin.krepost

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

class FetchDataDSL<Dto, Model, ErrorDto, ErrorModel : IKrepostError> {
    lateinit var action: suspend () -> Dto
    internal var cache: FetchDataCacheDSL? = null
    internal var mapper: ((Dto) -> Model)? = null
    internal var validator: ((Dto?) -> ValidateResult)? = null

    var config : KrepostConfig? = null

    fun action(block: suspend () -> Dto) {
        action = block
    }

    fun cache(name: String, cacheDsl: FetchDataCacheDSL.() -> Unit) {
        val cache = FetchDataCacheDSL(name)
        cache.cacheDsl()
        this.cache = cache
    }

    fun mapper(block: (Dto) -> Model) {
        mapper = block
    }

    fun validator(block: (Dto?) -> ValidateResult) {
        validator = block
    }
}

class FetchDataCacheDSL(val name: String) {
    var strategy: CacheStrategy = CacheStrategy.IfExist
    var cacheTimeMilliseconds: Long? = null
    var deleteIfOutdated: Boolean? = false

    var arguments: List<String>? = null

    fun arguments(vararg arguments: Any?) {
        this.arguments = arguments.mapNotNull { it?.toString() }
    }
}

