package ru.kirillashikhmin.krepost.cache

import kotlin.reflect.KType

@Suppress("unused")
class InMemoryCache : KrepostCache {

    private val cache = mutableMapOf<String, Any>()

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(
        type: KType,
        key: String,
        keyArguments: String,
        cacheTime: Long?,
        deleteIfOutdated: Boolean,
    ): CacheResult<T> = CacheResult(cache.getOrDefault(key + keyArguments, null) as? T)

    override fun <T> write(
        type: KType,
        key: String,
        keyArguments: String,
        data: T
    ) {
        if (data is Any) {
            cache[key + keyArguments] = data as Any
        } else {
            cache.remove(key + keyArguments)
        }
    }

    override fun delete(key: String, keyArguments: String) {
        cache.remove(key + keyArguments)
    }
}
