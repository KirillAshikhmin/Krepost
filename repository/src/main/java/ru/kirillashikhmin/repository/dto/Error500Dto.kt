package ru.kirillashikhmin.repository.dto

import kotlinx.serialization.Serializable

@Serializable
data class Error500Dto(val message: String, val supportPhone : String)
