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
    var cache: FetchDataCacheDSL? = null
    var mapper: ((Dto) -> Model)? = null

    fun action(block: suspend () -> Dto) {
        action = block
    }

    fun cache(cacheDsl: FetchDataCacheDSL.() -> Unit) {
        val cache = FetchDataCacheDSL()
        cache.cacheDsl()
        this.cache = cache
    }

    fun mapper(block: (Dto) -> Model) {
        mapper = block
    }
}

class FetchDataCacheDSL {
    var name: String? = null
    var strategy: CacheStrategy? = null

    private var arguments: List<String>? = null

    fun arguments(vararg arguments: Any?) {
        this.arguments = arguments.mapNotNull { it?.toString() }
    }
}

enum class CacheStrategy {
    /** If the data already exists in the cache, return it */
    IfExist,

    /** If unable to load data, return it from cache if it exists; otherwise, return null */
    IfNotAvailable,

    /** If the data is existed in the cache, return it, then load data and return */
    CachedThenLoad
}
