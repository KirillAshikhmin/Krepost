@file:Suppress("unused")

package ru.kirillashikhmin.krepost

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


sealed class RequestResult<out T> {

    open val status: RequestStatus = RequestStatus.Unknown//(0)

    class Empty<T>(val message: String? = null) : Success<T>() {

        override val status: RequestStatus
            get() = RequestStatus.Ok

        override fun toString() = "Empty ($message)"

        override val value: T
            get() = error("Empty result doesn't contain value")
    }

//    class EmptyResult : Success<Nothing?>() {
//        override val value: Nothing? = null
//    }

    sealed class Success<T> : RequestResult<T>() {

        abstract val value: T

        override fun toString() = "Success ($value)"

        class Value<T>(override val value: T) : Success<T>() {
            override val status: RequestStatus
                get() = RequestStatus.Ok
        }

        class ValueWithStatus<T>(override val value: T, override val status: RequestStatus) :
            Success<T>()
    }

    sealed class Cached<T>(override val value: T, open val cacheTime: Long) : Success<T>() {

        override fun toString() = "Cached ($value)"

        override val status: RequestStatus
            get() = RequestStatus.Ok

        class Value<T>(override val value: T, override val cacheTime: Long) :
            Cached<T>(value, cacheTime)
    }

    class Outdated<T>(override val value: T,override val cacheTime: Long) : Cached<T>(value, cacheTime)

    sealed class Failure<E>(open val error: E? = null, open val message: String? = null) :
        RequestResult<Nothing>() {

        override fun toString() = "Failure ($message $error $throwable)"
        open val throwable: Throwable? = null

        class Error(
            override val status: RequestStatus,
            override val message: String? = null,
            override val throwable: Throwable? = null,
        ) : Failure<Nothing>(message = message)

        class ErrorWithData<E>(
            override val error: E,
            override val status: RequestStatus,
            override val message: String? = null,
            override val throwable: Throwable? = null,
        ) : Failure<E>(error, message)
    }
}


@OptIn(ExperimentalContracts::class)
fun <T> RequestResult<T>.isSuccess(): Boolean {
    contract {
        returns(true) implies (this@isSuccess is RequestResult.Success<T>)
    }
    return this is RequestResult.Success<T>
}

@OptIn(ExperimentalContracts::class)
fun <T> RequestResult<T>.isCached(): Boolean {
    contract {
        returns(true) implies (this@isCached is RequestResult.Cached<T>)
    }
    return this is RequestResult.Cached<T>
}

@OptIn(ExperimentalContracts::class)
fun <T> RequestResult<T>.isEmpty(): Boolean {
    contract {
        returns(true) implies (this@isEmpty is RequestResult.Empty<T>)
    }
    return this is RequestResult.Empty<T>
}

@OptIn(ExperimentalContracts::class)
fun <T> RequestResult<T>.isOutdated(): Boolean {
    contract {
        returns(true) implies (this@isOutdated is RequestResult.Outdated<T>)
    }
    return this is RequestResult.Outdated<T>
}

@OptIn(ExperimentalContracts::class)
fun <T> RequestResult<T>.isFailure(): Boolean {
    contract {
        returns(true) implies (this@isFailure is RequestResult.Failure<*>)
    }
    return this is RequestResult.Failure<*>
}


fun <T> RequestResult<T>.asFailure(): RequestResult.Failure<*> {
    return this as RequestResult.Failure<*>
}

fun <T> RequestResult<T>.asSuccess(): RequestResult.Success<T> {
    return this as RequestResult.Success<T>
}

fun <T> RequestResult<T>.asEmpty(): RequestResult.Empty<T> {
    return this as RequestResult.Empty<T>
}

fun <T> RequestResult<T>.asCached(): RequestResult.Cached<T> {
    return this as RequestResult.Cached<T>
}

