package ru.kirillashikhmin.krepost.serializator

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object KotlinXSerializer : KrepostSerializer {


    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun serialize(data: Any): String {
        return json.encodeToString(Data(data))
    }

    override fun <T : Any> deserialize(str: String): T {
        val data : Data<T> = json.decodeFromString(str)
        return data.value
    }
    private class Data<T: Any>(val value : T)
}