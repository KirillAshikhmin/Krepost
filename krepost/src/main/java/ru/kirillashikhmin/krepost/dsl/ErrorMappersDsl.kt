package ru.kirillashikhmin.krepost.dsl

import ru.kirillashikhmin.krepost.BaseError
import ru.kirillashikhmin.krepost.IKrepostError
import ru.kirillashikhmin.krepost.RequestStatus
import ru.kirillashikhmin.krepost.serializers.KrepostSerializer
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class ErrorMappersDsl {

    internal val mappers = mutableListOf<IErrorMapper<*, *>>()

    inline fun <reified ErrorDto> any() {
        add<ErrorDto, BaseError>(RequestStatus.Any, typeOf<ErrorDto>(), null)
    }

    inline fun <reified ErrorDto, ErrorModel : IKrepostError> any(noinline block: (ErrorDto) -> ErrorModel) {
        add(RequestStatus.Any, typeOf<ErrorDto>(), block)
    }

    inline fun <reified ErrorDto, ErrorModel : IKrepostError> status(
        status: RequestStatus, noinline block: (ErrorDto) -> ErrorModel
    ) {
        add<ErrorDto, ErrorModel>(status, typeOf<ErrorDto>(), block)
    }

    inline fun <reified ErrorDto> status(status: RequestStatus) {
        add<ErrorDto, BaseError>(status, typeOf<ErrorDto>(), null)
    }

    fun <ErrorDto, ErrorModel : IKrepostError> add(
        status: RequestStatus, dtoType: KType, block: ((ErrorDto) -> ErrorModel)?
    ) {
        mappers.add(ErrorMapper<ErrorDto, ErrorModel>(status, dtoType, block))
    }

    class ErrorMapper<ErrorDto, ErrorModel : IKrepostError>(
        override val status: RequestStatus,
        private val type: KType,
        private val mapper: ((ErrorDto) -> ErrorModel)?
    ) : IErrorMapper<ErrorDto, ErrorModel> {
        override val mapped: Boolean
            get() = mapper != null

        override fun processError(json: String, serializer: KrepostSerializer): ErrorDto {
            return serializer.deserialize(json, type)
        }

        override fun processErrorMapped(json: String, serializer: KrepostSerializer): ErrorModel {
            val dto = serializer.deserialize<ErrorDto>(json, type)
            checkNotNull(mapper) { "Mapped should be defined" }
            return mapper.invoke(dto)
        }
    }

    interface IErrorMapper<ErrorDto, ErrorModel : IKrepostError> {
        val status: RequestStatus
        val mapped: Boolean

        fun processError(json: String, serializer: KrepostSerializer): ErrorDto
        fun processErrorMapped(json: String, serializer: KrepostSerializer): ErrorModel
    }

}
