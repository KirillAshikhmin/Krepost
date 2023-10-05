package ru.kirillashikhmin.krepost.errorMappers

import kotlinx.serialization.SerializationException
import ru.kirillashikhmin.krepost.RequestStatus

object KotlinXSerializationErrorMapper : ErrorMapper {
    override fun getErrorDataFromThrowable(
        throwable: Throwable,
        isGetResponseFromException: Boolean
    ): ErrorData? {
        if (throwable !is SerializationException) return null
        return ErrorData(RequestStatus.SerializationError, null)
    }
}
