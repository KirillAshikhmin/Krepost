package ru.kirillashikhmin.krepost.internal

enum class FetchType(val type: Int) {
    ModelMappedErrorMapped(4),
    ModelMappedError(3),
    ModelMapped(2),
    Dto(1),
}
