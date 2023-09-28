package ru.kirillashikhmin.krepost.errorFetcher

import ru.kirillashikhmin.krepost.RequestStatus

interface ErrorFetcher {


    /***
     * Should return Request status from throwable,
     * otherwise null if an unknown throwable was received as input,
     * or you won't process this throwable.
     */
    fun getErrorDataFromThrowable(throwable : Throwable) : RequestStatus?
}