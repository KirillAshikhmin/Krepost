package ru.kirillashikhmin.repository.dto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    @SerialName("id")
    val id: Int, // 1
    @SerialName("title")
    val title: String, // iPhone 9
    @SerialName("description")
    val description: String, // An apple mobile which is nothing like apple
    @SerialName("price")
    val price: Int, // 549
    @SerialName("discountPercentage")
    val discountPercentage: Double, // 12.96
    @SerialName("rating")
    val rating: Double, // 4.69
    @SerialName("stock")
    val stock: Int, // 94
    @SerialName("brand")
    val brand: String, // Apple
    @SerialName("category")
    val category: String, // smartphones
    @SerialName("thumbnail")
    val thumbnail: String, // https://i.dummyjson.com/data/products/1/thumbnail.jpg
    @SerialName("images")
    val images: List<String>
)
