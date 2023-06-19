package ru.kirillashikhmin.krepost

data class KrepostConfig(
    /**
     * Количество повторов выполнения запроса, если он завершился с ошибкой.
     * Повторяет запрос, только если его статус не в списке [NoRetryStatuses].
     */
    val RetryCount: Int = defaultRetryCount,
    /**
     * Задержка между повторами запросов.
     */
    val RetryDelayMilliseconds: Long = defaultRetryDelayMilliseconds,
    val CacheTimeMilliseconds: Long = defaultCacheTime,
    val DeleteCacheIfOutdated: Boolean = defaultDeleteCacheIfOutdated,
    val NoRetryStatuses: List<RequestStatus> = defaultNoRetryStatuses,
) {
    companion object {
        const val defaultCacheTime = 7 * 24 * 60 * 60 * 1000L // 7 days
        const val defaultDeleteCacheIfOutdated = true
        const val defaultRetryCount = 3
        const val defaultRetryDelayMilliseconds = 300L
        val defaultNoRetryStatuses = listOf(
            RequestStatus.NotFound,
            RequestStatus.Unprocessable,
            RequestStatus.Forbidden,
            RequestStatus.Unauthorized,
            RequestStatus.BadRequest,
            RequestStatus.SerializationError,
            RequestStatus.NotImplemented,
        )
    }
}