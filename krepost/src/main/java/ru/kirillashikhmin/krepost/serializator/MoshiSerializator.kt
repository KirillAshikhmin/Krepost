package ru.kirillashikhmin.krepost.serializator

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.lang.reflect.Type

object MoshiSerializer : KrepostSerializer {


    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    override fun serialize(data: Any): String {
        TODO()
//        return json.encodeToString(Data(data))
    }

    override fun <T : Any> deserialize(str: String, type: Type): T {
//        val jsonAdapter: JsonAdapter<T> = moshi.adapter<T>()
//
//        return jsonAdapter.fromJson(str)
        TODO()
    }

    inline fun <T : Any> deserialize2(str: String): T? {


        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

//        val jsonAdapter: JsonAdapter<T> = moshi.adapter<T>()

//        return jsonAdapter.fromJson(str)
        TODO()
    }
    private class Data<T: Any>(val value : T)
}