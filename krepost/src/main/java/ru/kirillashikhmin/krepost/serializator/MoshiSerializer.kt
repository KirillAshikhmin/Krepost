package ru.kirillashikhmin.krepost.serializator

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlin.reflect.KType

@Suppress("unused")
object MoshiSerializer : KrepostSerializer {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @OptIn(ExperimentalStdlibApi::class)
    override fun <T> serialize(data: T, type: KType): String {
        val jsonAdapter: JsonAdapter<T> = moshi.adapter(type)
        return jsonAdapter.toJson(data)
    }

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalStdlibApi::class)
    override fun <T> deserialize(str: String, type: KType): T {
        val jsonAdapter: JsonAdapter<T> = moshi.adapter(type)
        return jsonAdapter.fromJson(str) as T
    }

}
