package ru.kirillashikhmin.repository

import ru.kirillashikhmin.repository.dto.Error500Dto
import ru.kirillashikhmin.repository.dto.ErrorDto
import ru.kirillashikhmin.repository.dto.ProductDto
import ru.kirillashikhmin.repository.dto.ProductsDto
import ru.kirillashikhmin.repository.models.DummyError
import ru.kirillashikhmin.repository.models.Error500
import ru.kirillashikhmin.repository.models.Product

object Mappers {

    fun mapProducts(products: ProductsDto): List<Product> = products.products.map(::mapProduct)

    fun mapProduct(product: ProductDto) = Product(
        product.id,
        product.title,
        product.description,
        product.price,
        product.brand,
        product.thumbnail
    )

    fun mapError(errorDto: ErrorDto) = DummyError(errorDto.message)
    fun map500Error(errorDto: Error500Dto) =
        Error500(errorDto.message, errorDto.supportPhone)
}
