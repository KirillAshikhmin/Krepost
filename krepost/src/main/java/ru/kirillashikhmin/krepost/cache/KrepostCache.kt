package  ru.kirillashikhmin.krepost.cache

import kotlin.reflect.KType

interface KrepostCache {

    fun <T> get(
        type: KType,
        key: String,
        keyArguments: String,
        cacheTime: Long?,
        deleteIfOutdated: Boolean = true,
    ): CacheResult<T>

    fun <T> write(
        type: KType,
        key: String,
        keyArguments: String,
        data: T,
    )

    fun delete(
        key: String,
        keyArguments: String
    )
}

data class CacheResult<T>(val value: T?, val cacheTime: Long = 0L, val outdated: Boolean = false)
