package ru.kirillashikhmin.repository

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit


@ExperimentalSerializationApi
object RepositoryCore {

    private val contentType = "application/json".toMediaType()

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun <S> createService(url: String, serviceClass: Class<S>): S {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC)
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .client(client)
            .baseUrl(url)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(serviceClass)
    }
}
