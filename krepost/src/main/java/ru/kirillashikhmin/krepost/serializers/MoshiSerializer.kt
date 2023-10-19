package ru.kirillashikhmin.krepost.serializers

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.serialization.SerializationException
import ru.kirillashikhmin.krepost.KrepostSerializeException
import kotlin.reflect.KType

@Suppress("unused")
object MoshiSerializer : KrepostSerializer {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @OptIn(ExperimentalStdlibApi::class)
    override fun <T> serialize(data: T, type: KType): String {
        try {
            val jsonAdapter: JsonAdapter<T> = moshi.adapter(type)
            return jsonAdapter.toJson(data)
        } catch (t: SerializationException) {
            throw KrepostSerializeException(t)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalStdlibApi::class)
    override fun <T> deserialize(str: String, type: KType): T {
        try {
            val jsonAdapter: JsonAdapter<T> = moshi.adapter(type)
            return jsonAdapter.fromJson(str) as T
        } catch (t: SerializationException) {
            throw KrepostSerializeException(t)
        }
    }

}
