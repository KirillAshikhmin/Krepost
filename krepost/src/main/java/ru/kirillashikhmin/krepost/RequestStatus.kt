package  ru.kirillashikhmin.krepost


@Suppress("unused")
open class RequestStatus(val code: Int) {
    object Unknown : RequestStatus(0)
    object Ok : RequestStatus(200)
    object NotModified : RequestStatus(304)
    object BadRequest : RequestStatus(400)
    object Unauthorized : RequestStatus(401)
    object Forbidden : RequestStatus(403)
    object NotFound : RequestStatus(404)
    object NotAcceptable : RequestStatus(406)
    object RequestEntityTooLarge : RequestStatus(413)
    object Unprocessable : RequestStatus(422)
    object InternalServerError : RequestStatus(500)
    object NotImplemented : RequestStatus(501)
    object ServiceUnavailable : RequestStatus(503)

    // Krepost codes
    object Canceled : RequestStatus(1001)
    object InvalidRequest : RequestStatus(1002)
    object SerializationError : RequestStatus(1003)
    object CacheError : RequestStatus(1004)
    object NoInternet : RequestStatus(1100)


    companion object {

        fun addStatus(vararg status: RequestStatus) {
            status.forEach {
                statuses[it.code] = it
            }
        }

        fun fromCode(code: Int): RequestStatus {
            return statuses[code] ?: RequestStatus.Unknown
        }

    private val statuses = mutableMapOf(
        Unknown.code to Unknown,
        Ok.code to Ok,
        NotModified.code to NotModified,
        BadRequest.code to BadRequest,
        Unauthorized.code to Unauthorized,
        Forbidden.code to Forbidden,
        NotFound.code to NotFound,
        NotAcceptable.code to NotAcceptable,
        RequestEntityTooLarge.code to RequestEntityTooLarge,
        Unprocessable.code to Unprocessable,
        InternalServerError.code to InternalServerError,
        NotImplemented.code to NotImplemented,
        ServiceUnavailable.code to ServiceUnavailable,
        Canceled.code to Canceled,
        InvalidRequest.code to InvalidRequest,
        SerializationError.code to SerializationError,
        CacheError.code to CacheError,
        NoInternet.code to NoInternet,
    )
    }
}
