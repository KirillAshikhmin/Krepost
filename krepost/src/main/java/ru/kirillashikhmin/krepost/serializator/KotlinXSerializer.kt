package ru.kirillashikhmin.krepost.serializator

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType

@Suppress("unused")
object KotlinXSerializer : KrepostSerializer {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun <T> serialize(data: T, type: KType): String {
        return json.encodeToString(Json.serializersModule.serializer(type), data)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> deserialize(str: String, type: KType): T {
        return json.decodeFromString(Json.serializersModule.serializer(type), str) as T
    }
}
