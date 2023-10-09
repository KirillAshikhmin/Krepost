package ru.kirillashikhmin.krepost.serializator

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Response
import java.lang.reflect.Type
import kotlin.reflect.full.createType
import kotlin.reflect.javaType

object KotlinXSerializer : KrepostSerializer {


    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val contentType = MediaType.get("application/json")
    val factory = json.asConverterFactory(contentType)

    override fun serialize(data: Any): String {
        return json.encodeToString(Data(data))
    }

    override fun <T : Any> deserialize(str: String, type: Type): T {
        val response = ResponseBody.create(contentType, str)
        val loader = serializer(type, Json)
        val converter = DeserializationStrategyConverter(loader, Json)
        val result = converter.convert(response)
        return result as T

//        val data : Data<T> = json.decodeFromString(str)
//        return data.value
    }
    private class Data<T: Any>(val value : T)
    fun serializer(type: Type, format: StringFormat): KSerializer<Any> = format.serializersModule.serializer(type)

    class DeserializationStrategyConverter<T>(
        private val loader: DeserializationStrategy<T>,
        private val format: StringFormat
    ) : Converter<ResponseBody, T> {
        override fun convert(value: ResponseBody) = fromResponseBody(loader, value)

        private fun <T> fromResponseBody(loader: DeserializationStrategy<T>, body: ResponseBody): T {
            val string = body.string()
            return format.decodeFromString(loader, string)
        }
    }
}