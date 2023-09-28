package ru.kirillashikhmin.krepost.errorMappers

import retrofit2.HttpException
import ru.kirillashikhmin.krepost.RequestStatus

object RetrofitErrorMapper : ErrorMapper {
    override fun getErrorDataFromThrowable(throwable: Throwable): ErrorData? {
        if (throwable !is HttpException) return null
        val status = when (throwable.code()) {
            200 -> RequestStatus.Ok
            304 -> RequestStatus.NotModified
            400 -> RequestStatus.BadRequest
            401 -> RequestStatus.Unauthorized
            403 -> RequestStatus.Forbidden
            404 -> RequestStatus.NotFound
            406 -> RequestStatus.NotAcceptable
            413 -> RequestStatus.RequestEntityTooLarge
            418 -> RequestStatus.IAmATeapot
            422 -> RequestStatus.Unprocessable
            500 -> RequestStatus.InternalServerError
            501 -> RequestStatus.NotImplemented
            503 -> RequestStatus.ServiceUnavailable
            else -> return null
        }
        return ErrorData(status, throwable.response()?.errorBody()?.string())
    }
}
