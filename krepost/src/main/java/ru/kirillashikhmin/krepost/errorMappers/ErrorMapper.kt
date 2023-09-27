package ru.kirillashikhmin.krepost.errorMappers

import ru.kirillashikhmin.krepost.RequestStatus

interface ErrorMapper {

    /***
     * Should return Request status from throwable,
     * otherwise null if an unknown throwable was received as input,
     * or you won't process this throwable.
     */
    fun getRequestStatusFromThrowable(throwable : Throwable) : RequestStatus?
}
