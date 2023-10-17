package ru.kirillashikhmin.repository.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    @SerialName("id")
    val id: Int, // 1
    @SerialName("title")
    val title: String, // iPhone 9
    @SerialName("description")
    val description: String, // An apple mobile which is nothing like apple
    @SerialName("price")
    val price: Int, // 549
    @SerialName("brand")
    val brand: String, // Apple
    @SerialName("thumbnail")
    val thumbnail: String, // https://i.dummyjson.com/data/products/1/thumbnail.jpg
)
