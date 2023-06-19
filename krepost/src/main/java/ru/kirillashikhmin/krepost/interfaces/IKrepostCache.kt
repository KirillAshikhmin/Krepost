package  ru.kirillashikhmin.krepost.interfaces

interface IKrepostCache {
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
}
