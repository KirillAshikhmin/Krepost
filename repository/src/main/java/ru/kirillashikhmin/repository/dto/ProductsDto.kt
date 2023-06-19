package ru.kirillashikhmin.repository.dto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductsDto(
    @SerialName("products")
    val products: List<ProductDto>,
    @SerialName("total")
    val total: Int, // 100
    @SerialName("skip")
    val skip: Int, // 0
    @SerialName("limit")
    val limit: Int // 30
)
