package ru.kirillashikhmin.repository

import kotlinx.coroutines.delay
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import ru.kirillashikhmin.krepost.CacheStrategy
import ru.kirillashikhmin.krepost.Krepost
import ru.kirillashikhmin.krepost.KrepostConfig
import ru.kirillashikhmin.krepost.RequestResult
import ru.kirillashikhmin.krepost.RequestStatus
import ru.kirillashikhmin.krepost.ValidateResult
import ru.kirillashikhmin.krepost.cache.LocalFileCache
import ru.kirillashikhmin.krepost.errorMappers.KotlinXSerializationErrorMapper
import ru.kirillashikhmin.krepost.errorMappers.RetrofitErrorMapper
import ru.kirillashikhmin.krepost.serializers.KotlinXSerializer
import ru.kirillashikhmin.repository.dto.Error500Dto
import ru.kirillashikhmin.repository.dto.ErrorDto
import ru.kirillashikhmin.repository.dto.FetchDataDto
import ru.kirillashikhmin.repository.dto.ProductDto
import ru.kirillashikhmin.repository.dto.ProductsDto
import ru.kirillashikhmin.repository.models.DummyError
import ru.kirillashikhmin.repository.models.Error500
import ru.kirillashikhmin.repository.models.Product
import kotlin.random.Random

@ExperimentalSerializationApi
@Suppress("MagicNumber")
class Repository(cacheDir: String) {

    private var service: DummyJsonService =
        RepositoryCore.createService("https://dummyjson.com/", DummyJsonService::class.java)

    val kotlinXSerializer = KotlinXSerializer(RepositoryCore.json)
    private val localFileCache = LocalFileCache(cacheDir, kotlinXSerializer)

    private val krepost = Krepost {
        errorMappers = listOf(
            KotlinXSerializationErrorMapper, RetrofitErrorMapper()
        )
        serializer = kotlinXSerializer
        cacher = localFileCache
        config = KrepostConfig(retryCount = 1)
    }

    init {
        localFileCache.clear()
    }

    suspend fun fetchProducts(invalidateCache: Boolean): RequestResult<List<Product>> =
        krepost.fetchMapped<ProductsDto, List<Product>> {
            action { service.getProducts() }
            cache("products") {
                strategy = CacheStrategy.IfExist
                invalidate = invalidateCache
            }
            validator(Validators::validateProducts)
            mapper(Mappers::mapProducts)
        }

    suspend fun sendWithoutResponse(): RequestResult<Unit> =
        krepost.fetch {
            action { service.send() }
            errorMapper(Mappers::mapError)
        }

    suspend fun fetchProduct(id: Int, invalidateCache: Boolean): RequestResult<Product> =
        krepost.fetchMapped<ProductDto, Product> {
            action { service.getProduct(id) }
            cache("product") {
                strategy = CacheStrategy.IfExist
                invalidate = invalidateCache
                arguments(id)
            }
            mapper(Mappers::mapProduct)
            errorMapper(Mappers::mapError)
        }

    suspend fun fetchData(invalidateCache: Boolean): RequestResult<FetchDataDto> {
        val result2 = krepost.fetch<FetchDataDto> {
            action { getData<FetchDataDto>() }
            cache("data") {
                strategy = CacheStrategy.IfNotAvailable
                deleteIfOutdated = false
                cacheTimeMilliseconds = 10_000 // 10 sec cache valid
                invalidate = invalidateCache
            }
            validator { data ->
                if (data?.message == null) ValidateResult.Error()
                else if (data.message.isBlank()) ValidateResult.Empty()
                else ValidateResult.Ok
            }
            errorMappers {
                any<ErrorDto, DummyError> { error -> DummyError(error.message) }
                status<Error500Dto, Error500>(
                    RequestStatus.InternalServerError,
                    Mappers::map500Error
                )
            }
        }
        return result2
    }


    private suspend fun <T> getData(): FetchDataDto {
        delay(Random.nextLong(1000))
        return if (Random.nextBoolean()) {
            throw when (Random.nextInt(3)) {
                0 -> SerializationException("Test serialization exception")
                1 -> HttpException(
                    Response.error<T>(
                        500,
                        "{\"message\": \"Test 500 exception\", \"supportPhone\": \"88005553535\"}"
                            .toResponseBody("application/json".toMediaType())
                    )
                )

                2 -> HttpException(
                    Response.error<T>(
                        404,
                        "{\"message\": \"Item not found\"}"
                            .toResponseBody("application/json".toMediaType())
                    )
                )

                else -> error("Other error")
            }
        } else
            FetchDataDto(if (Random.nextBoolean()) "Successful response" else "")
    }
}

