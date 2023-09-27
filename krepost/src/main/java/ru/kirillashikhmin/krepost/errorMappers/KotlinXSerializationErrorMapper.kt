package ru.kirillashikhmin.krepost.errorMappers

import kotlinx.serialization.SerializationException
import ru.kirillashikhmin.krepost.RequestStatus

object KotlinXSerializationErrorMapper : ErrorMapper {
    override fun getRequestStatusFromThrowable(throwable: Throwable): RequestStatus? {
        if (throwable !is SerializationException) return null
        return RequestStatus.SerializationError
    }
}
