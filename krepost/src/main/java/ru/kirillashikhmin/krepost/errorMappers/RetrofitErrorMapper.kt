package ru.kirillashikhmin.krepost.errorMappers

import retrofit2.HttpException
import ru.kirillashikhmin.krepost.RequestStatus

open class RetrofitErrorMapper : ErrorMapper {

    @Suppress("ReturnCount")
    override fun getErrorDataFromThrowable(
        throwable: Throwable,
        isGetResponseFromException: Boolean
    ): ErrorData? {
        if (throwable !is HttpException) return null
        val status = getStatusFromCode(throwable.code()) ?: return null
        val response =
            if (isGetResponseFromException) throwable.response()?.errorBody()?.string() else null
        return ErrorData(status, response)
    }

    open fun getStatusFromCode(code: Int): RequestStatus? {
        return RequestStatus.fromCode(code)
    }
}

