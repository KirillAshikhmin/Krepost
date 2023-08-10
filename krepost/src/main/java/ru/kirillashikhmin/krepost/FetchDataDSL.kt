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

    fun cache(name: String, cacheDsl: FetchDataCacheDSL.() -> Unit) {
        val cache = FetchDataCacheDSL(name)
        cache.cacheDsl()
        this.cache = cache
    }

    fun mapper(block: (Dto) -> Model) {
        mapper = block
    }
}

class FetchDataCacheDSL(val name: String) {
    var strategy: CacheStrategy = CacheStrategy.FromCacheIfExist
    var cacheTimeMilliseconds: Long? = null
    var deleteIfOutdated: Boolean? = false

    var arguments: List<String>? = null

    fun arguments(vararg arguments: Any?) {
        this.arguments = arguments.mapNotNull { it?.toString() }
    }
}

enum class CacheStrategy {
    /** If the data already exists in the cache, return it */
    FromCacheIfExist,

    /** If unable to load data, return it from cache if it exists; otherwise, return null */
    FromCacheIfNotAvailable,

    /** If the data is existed in the cache, return it, then load data and return */
    CachedThenLoad;

    companion object {
        val CacheStrategy.needGetCachedResultBeforeLoad
            get() = this == FromCacheIfExist || this == CachedThenLoad
    }
}
