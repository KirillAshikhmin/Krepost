package ru.kirillashikhmin.krepost.errorMappers

import ru.kirillashikhmin.krepost.RequestStatus

data class ErrorData(val status: RequestStatus, val response: String?)
