package  ru.kirillashikhmin.krepost

data class RequestStatusTest(val code: Int) : RequestStatus(code)
 class RequestStatusTest2 : RequestStatus(0)

@Suppress("unused", "MagicNumber")
sealed class RequestStatus(val codeTest: Int) {

    // That fix java.lang.ExceptionInInitializerError
//    open val code : Int = -1

    data object Unknown : RequestStatus(0) {
//        override val code = 0
    }

    //Http codes
    data object Ok : RequestStatus(200) {
//        override val code = 200
    }
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

        fun initialize() {
           val init = RequestStatusTest2()
        }

        fun addStatus(vararg status: RequestStatus) {
            status.forEach {
                statuses[it.codeTest] = it
            }
        }

        fun fromCode(code: Int): RequestStatus {
            return statuses[code] ?: Unknown
        }

        private val statuses = mutableMapOf(
            Unknown.codeTest to Unknown,
            Ok.codeTest to Ok,
            NotModified.codeTest to NotModified,
            BadRequest.codeTest to BadRequest,
            Unauthorized.codeTest to Unauthorized,
            Forbidden.codeTest to Forbidden,
            NotFound.codeTest to NotFound,
            NotAcceptable.codeTest to NotAcceptable,
            RequestEntityTooLarge.codeTest to RequestEntityTooLarge,
            Unprocessable.codeTest to Unprocessable,
            InternalServerError.codeTest to InternalServerError,
            NotImplemented.codeTest to NotImplemented,
            ServiceUnavailable.codeTest to ServiceUnavailable,
            Canceled.codeTest to Canceled,
            InvalidRequest.codeTest to InvalidRequest,
            SerializationError.codeTest to SerializationError,
            CacheError.codeTest to CacheError,
            NoInternet.codeTest to NoInternet,
        )
    }
}
