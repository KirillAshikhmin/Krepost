package ru.kirillashikhmin.repository

import retrofit2.http.GET
import retrofit2.http.Path
import ru.kirillashikhmin.repository.dto.ProductsDto


interface DummyJsonService {

    @GET("products")
    suspend fun getProducts(): ProductsDto
}
