package ru.kirillashikhmin.repository

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import retrofit2.Retrofit

@ExperimentalSerializationApi
object RepositoryCore {

    private val contentType = MediaType.get("application/json")

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun <S> createService(url: String, serviceClass: Class<S>): S {
        return Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(serviceClass)
    }
}
