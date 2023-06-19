package ru.kirillashikhmin.krepost.cache

import ru.kirillashikhmin.krepost.interfaces.IKrepostCache

class LruCache : IKrepostCache {

    private val cache = mutableMapOf<String, Any>()

    override fun <T : Any> get(
        key: String,
        keyArguments: String,
        cacheTime: Long?,
        deleteIfOutdated: Boolean,
    ): Pair<T?, Long> {
        return Pair(cache.getOrDefault(key + keyArguments, null) as T?, 0L)
    }

    override fun <T : Any> write(key: String, keyArguments: String, data: T) {
        cache[key + keyArguments] = data
    }
}
