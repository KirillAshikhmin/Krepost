package  ru.kirillashikhmin.krepost

@Suppress("unused", "MagicNumber")
sealed class RequestStatus(val code: Int) {
    class Initial(code : Int) : RequestStatus(code)

    data object Any : RequestStatus(-1)
    data object Unknown : RequestStatus(0)

    //Http codes
    data object Ok : RequestStatus(200)
    data object NotModified : RequestStatus(304)
    data object BadRequest : RequestStatus(400)
    data object Unauthorized : RequestStatus(401)
    data object Forbidden : RequestStatus(403)
    data object NotFound : RequestStatus(404)
    data object NotAcceptable : RequestStatus(406)
    data object RequestEntityTooLarge : RequestStatus(413)
    data object IAmATeapot : RequestStatus(418)
    data object Unprocessable : RequestStatus(422)
    data object InternalServerError : RequestStatus(500)
    data object NotImplemented : RequestStatus(501)
    data object ServiceUnavailable : RequestStatus(503)


    // Krepost codes
    data object KrepostInternalError : RequestStatus(1000)
    data object Canceled : RequestStatus(1001)
    data object InvalidRequest : RequestStatus(1002)
    data object SerializationError : RequestStatus(1003)
    data object CacheError : RequestStatus(1004)
    data object MappingError : RequestStatus(1005)
    data object UnknownRequestError : RequestStatus(1006)
    data object NoInternet : RequestStatus(1100)


    companion object {

        @Suppress("UNUSED_VARIABLE")
        fun initialize() {
           val init = Initial(0)
        }

        fun addStatus(vararg status: RequestStatus) {
            status.forEach {
                statuses[it.code] = it
            }
        }

        fun fromCode(code: Int): RequestStatus? {
            return statuses[code]
        }

        private val statuses = mutableMapOf(
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
