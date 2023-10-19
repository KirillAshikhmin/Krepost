package ru.kirillashikhmin.krepost

interface IKrepostError {
    val errorMessage: String?
}

class BaseError(override val errorMessage: String?) : IKrepostError

