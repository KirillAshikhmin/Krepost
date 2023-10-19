package ru.kirillashikhmin.krepost

import ru.kirillashikhmin.krepost.errorMappers.ErrorData

class KrepostRequestException(val innerException: Throwable, val errorData: ErrorData) : Exception()
class KrepostCacheException(val innerException: Throwable, val write: Boolean) : Exception()
class KrepostSerializeException(val innerException: Throwable) : Exception()
