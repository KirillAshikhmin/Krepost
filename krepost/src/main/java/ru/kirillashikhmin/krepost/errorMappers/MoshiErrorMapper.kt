package ru.kirillashikhmin.krepost.errorMappers

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import ru.kirillashikhmin.krepost.RequestStatus

@Suppress("unused")
object MoshiErrorMapper : ErrorMapper {
    override fun getErrorDataFromThrowable(
        throwable: Throwable,
        isGetResponseFromException: Boolean
    ): ErrorData? {
        if (
            throwable !is JsonEncodingException &&
            throwable !is JsonDataException
        ) return null

        return ErrorData(RequestStatus.SerializationError, null)
    }
}
