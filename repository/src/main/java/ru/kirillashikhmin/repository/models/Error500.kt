package ru.kirillashikhmin.repository.models

import ru.kirillashikhmin.krepost.IKrepostError

data class Error500(override val errorMessage: String, val supportPhone : String) : IKrepostError
