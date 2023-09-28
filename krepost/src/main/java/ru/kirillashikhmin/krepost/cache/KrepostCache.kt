package  ru.kirillashikhmin.krepost.cache

interface KrepostCache {

    fun <T : Any> get(
        key: String,
        keyArguments: String,
        cacheTime: Long?,
        deleteIfOutdated: Boolean = true,
    ): Pair<T?, Long>

    fun <T : Any> write(
        key: String,
        keyArguments: String,
        data: T,
    )

    fun delete(
        key: String,
        keyArguments: String
    )
}
