package ru.kirillashikhmin.repository.models

import ru.kirillashikhmin.krepost.IKrepostError

data class DummyError(override val errorMessage: String) : IKrepostError
