package ru.kirillashikhmin.repository

import ru.kirillashikhmin.krepost.ValidateResult
import ru.kirillashikhmin.repository.dto.ProductsDto

object Validators {

    fun validateProducts(productsDto: ProductsDto?): ValidateResult =
        when {
            productsDto == null -> ValidateResult.Error()
            productsDto.products.isEmpty() -> ValidateResult.Empty()
            else -> ValidateResult.Ok
        }
}
