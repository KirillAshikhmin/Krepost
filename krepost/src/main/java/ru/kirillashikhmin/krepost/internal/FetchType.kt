package ru.kirillashikhmin.krepost.internal

data class FetchType(
    val mapped: Boolean = false,
    val error: Boolean = false,
    val errorMapped: Boolean = false
)
