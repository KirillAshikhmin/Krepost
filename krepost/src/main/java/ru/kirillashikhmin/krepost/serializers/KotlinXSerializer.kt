package ru.kirillashikhmin.krepost.serializers

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import ru.kirillashikhmin.krepost.KrepostSerializeException
import kotlin.reflect.KType

@Suppress("unused")
class KotlinXSerializer(json: Json? = null) : KrepostSerializer {

    private val json = json ?: Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun <T> serialize(data: T, type: KType): String {
        try {
            return json.encodeToString(Json.serializersModule.serializer(type), data)
        } catch (t: SerializationException) {
            throw KrepostSerializeException(t)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> deserialize(str: String, type: KType): T {
        try {
            return json.decodeFromString(Json.serializersModule.serializer(type), str) as T
        } catch (t: SerializationException) {
            throw KrepostSerializeException(t)
        }
    }
}
