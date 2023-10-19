package ru.kirillashikhmin.krepost.dsl

import ru.kirillashikhmin.krepost.CacheStrategy

class CacheDSL(val name: String) {
    var strategy: CacheStrategy = CacheStrategy.IfExist
    var cacheTimeMilliseconds: Long? = null
    var deleteIfOutdated: Boolean? = false
    var invalidate: Boolean? = false

    var arguments: List<String>? = null

    fun arguments(vararg arguments: Any?) {
        this.arguments = arguments.mapNotNull { it?.toString() }
    }
}
