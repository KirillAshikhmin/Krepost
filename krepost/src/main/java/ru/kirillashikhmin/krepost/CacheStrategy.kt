package ru.kirillashikhmin.krepost

enum class CacheStrategy {
    /** If the data already exists in the cache, return it */
    IfExist,

    /** If unable to load data, return it from cache if it exists; otherwise, return null */
    IfNotAvailable,

    /** If the data is existed in the cache, return it, then load data and return */
    CachedThenLoad,

    /** Never use cache */
    Never,

    /** Return only from cache */
    Only;

    companion object {
        val CacheStrategy.needGetCachedResultBeforeLoad
            get() = this == IfExist || this == CachedThenLoad || this == Only
    }
}
