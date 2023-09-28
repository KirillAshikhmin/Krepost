package ru.kirillashikhmin.krepost

class KrepostRequestException(val innerException: Throwable, val status: RequestStatus?) : Exception()