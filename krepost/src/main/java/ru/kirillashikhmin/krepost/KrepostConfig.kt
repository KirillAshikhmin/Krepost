package ru.kirillashikhmin.krepost

data class KrepostConfig(
    /**
     * Количество повторов выполнения запроса, если он завершился с ошибкой.
     * Повторяет запрос, только если его статус не в списке [noRetryStatuses].
     */
    var retryCount: Int = defaultRetryCount,
    /**
     * Задержка между повторами запросов.
     */
    var retryDelayMilliseconds: Long = defaultRetryDelayMilliseconds,
    /**
     * Сколько времени валиден кэш.
     */
    var cacheTimeMilliseconds: Long = defaultCacheTime,
    /**
     * Удалять ли устаревший кэш при запросе (true)
     * или возвращать Outdated результат (false).
     */
    var deleteCacheIfOutdated: Boolean = defaultDeleteCacheIfOutdated,
    /**
     * Список статусов при которых не происходит повторного запроса
     * при ошибке и retryCount > 1.
     */
    var noRetryStatuses: List<RequestStatus> = defaultNoRetryStatuses,
) {
    companion object {
        const val defaultCacheTime = 24 * 60 * 60 * 1000L // 24 hours
        const val defaultDeleteCacheIfOutdated = true
        const val defaultRetryCount = 3
        const val defaultRetryDelayMilliseconds = 300L
        val defaultNoRetryStatuses = listOf<RequestStatus>(
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
