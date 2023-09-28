package ru.kirillashikhmin.krepost.cache

class InMemoryCache : KrepostCache {

    private val cache = mutableMapOf<String, Any>()

    override fun <T : Any> get(
        key: String,
        keyArguments: String,
        cacheTime: Long?,
        deleteIfOutdated: Boolean,
    ): Pair<T?, Long> = Pair(cache.getOrDefault(key + keyArguments, null) as T?, 0L)

    override fun <T : Any> write(key: String, keyArguments: String, data: T) {
        cache[key + keyArguments] = data
    }

    override fun delete(key: String, keyArguments: String) {
        cache.remove(key + keyArguments)
    }
}
