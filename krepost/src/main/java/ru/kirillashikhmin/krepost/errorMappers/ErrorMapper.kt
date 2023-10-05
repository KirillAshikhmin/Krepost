package ru.kirillashikhmin.krepost.errorMappers

interface ErrorMapper {

    /**
     * Should return Request status from throwable,
     * otherwise null if an unknown throwable was received as input,
     * or you won't process this throwable.
     */
    fun getErrorDataFromThrowable(throwable: Throwable, isGetResponseFromException: Boolean) : ErrorData?
}
