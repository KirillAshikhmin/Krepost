package ru.kirillashikhmin.repository

import kotlinx.coroutines.delay
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import ru.kirillashikhmin.krepost.CacheStrategy
import ru.kirillashikhmin.krepost.IKrepostError
import ru.kirillashikhmin.krepost.Krepost
import ru.kirillashikhmin.krepost.KrepostConfig
import ru.kirillashikhmin.krepost.RequestResult
import ru.kirillashikhmin.krepost.RequestStatus
import ru.kirillashikhmin.krepost.ValidateResult
import ru.kirillashikhmin.krepost.cache.InMemoryCache
import ru.kirillashikhmin.krepost.errorMappers.KotlinXSerializationErrorMapper
import ru.kirillashikhmin.krepost.errorMappers.RetrofitErrorMapper
import ru.kirillashikhmin.krepost.serializator.KotlinXSerializer
import ru.kirillashikhmin.repository.dto.ProductsDto
import kotlin.random.Random


@ExperimentalSerializationApi
class Repository {

    private var service: DummyJsonService =
        RepositoryCore.createService("https://dummyjson.com/", DummyJsonService::class.java)

    suspend fun getProducts(): RequestResult<ProductsDto> {
        return try {
            val result = service.getProducts()
            RequestResult.Success.Value(result)
        } catch (t: Throwable) {
            RequestResult.Failure.Error(RequestStatus.Unprocessable, "Error", t)
        }
    }

    val krepost = Krepost {
        errorMappers = listOf(
            KotlinXSerializationErrorMapper, RetrofitErrorMapper
        )
        serializer = KotlinXSerializer
        cacher = InMemoryCache()//LocalFileCache(".", KotlinXSerializer)
        config = KrepostConfig(retryCount = 1)
    }

    suspend fun fetchData1(): RequestResult<Int> = krepost.fetchDataMapped<String, Int> {
        action { getData() }
        cache("fetchString") {
            strategy = CacheStrategy.IfExist
            arguments("string", 1)
        }
        mapper { data -> data.toInt() }
    }

    suspend fun fetchData2(): RequestResult<Int> {
        val result2 = krepost.fetchDataMapped<String, Int> {
            action { getData() }
            cache("bbb") {
                strategy = CacheStrategy.IfNotAvailable
                arguments(2)
            }
            mapper { data -> data.toInt() }
        }
        return result2
    }

    suspend fun fetchData3(): RequestResult<String> {
        val result3 = krepost.fetchData<String> {
            action { getData3() }
            cache("ccc") {
                strategy = CacheStrategy.IfNotAvailable
                arguments(3)
            }
        }

        return result3
    }

    suspend fun fetchData4(): RequestResult<Int> {
        val result2 = krepost.fetchDataMappedAndErrorMapped<String, Int, ErrorDto, MyKrepostError> {
            action { getData2<String>() }
            cache("bbb") {
                strategy = CacheStrategy.IfNotAvailable
                arguments(2)
            }
            mapper { data -> data.toInt() }
            validator { data ->
                if (data == null) ValidateResult.Error()
                else if (data.isBlank()) ValidateResult.Empty()
                else ValidateResult.Ok
            }
            errorMapper { error -> MyKrepostError(error.error) }
        }
        return result2
    }

    suspend fun getData(): String {
        delay(100)
        return "1"
    }

    suspend fun <T> getData2(): String {
        delay(100)
        return if (Random.nextBoolean()) {
            throw when (Random.nextInt(2)) {
                0 -> SerializationException("Test serialization exception")
                1 -> HttpException(Response.error<T>(500, ResponseBody.create(MediaType.parse("application/json"), "{\"error\": \"Test\"}")))
                else -> error("Other error")
            }
        } else
            if (Random.nextBoolean())  "42" else ""
    }


    suspend fun getData3(): String {
        delay(100)
        return "3"
    }

    data class ErrorDto(val error: String)

    class MyKrepostError(val message : String? = null) : IKrepostError {
        override val errorMessage: String
            get() = message ?: "I'm stub error"
    }

}
