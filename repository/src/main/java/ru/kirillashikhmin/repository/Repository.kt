package ru.kirillashikhmin.repository

import kotlinx.coroutines.delay
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import ru.kirillashikhmin.krepost.CacheStrategy
import ru.kirillashikhmin.krepost.Krepost
import ru.kirillashikhmin.krepost.KrepostConfig
import ru.kirillashikhmin.krepost.RequestResult
import ru.kirillashikhmin.krepost.ValidateResult
import ru.kirillashikhmin.krepost.cache.LocalFileCache
import ru.kirillashikhmin.krepost.errorMappers.KotlinXSerializationErrorMapper
import ru.kirillashikhmin.krepost.errorMappers.RetrofitErrorMapper
import ru.kirillashikhmin.krepost.serializator.KotlinXSerializer
import ru.kirillashikhmin.repository.dto.ErrorDto
import ru.kirillashikhmin.repository.dto.FetchDataDto
import ru.kirillashikhmin.repository.dto.ProductDto
import ru.kirillashikhmin.repository.dto.ProductsDto
import ru.kirillashikhmin.repository.models.DummyError
import ru.kirillashikhmin.repository.models.Product
import kotlin.random.Random


@ExperimentalSerializationApi
@Suppress("MagicNumber")
class Repository(cacheDir: String) {

    private var service: DummyJsonService =
        RepositoryCore.createService("https://dummyjson.com/", DummyJsonService::class.java)

    private val localFileCache = LocalFileCache(cacheDir, KotlinXSerializer)

    private val krepost = Krepost {
        errorMappers = listOf(
            KotlinXSerializationErrorMapper, RetrofitErrorMapper
        )
        serializer = KotlinXSerializer
        cacher = localFileCache
        config = KrepostConfig(retryCount = 1)
    }

    init {
        localFileCache.clear()
    }

    suspend fun fetchProducts(invalidateCache: Boolean): RequestResult<List<Product>> =
        krepost.fetchDataMapped<ProductsDto, List<Product>> {
            action { service.getProducts() }
            cache("products") {
                strategy = CacheStrategy.IfExist
                invalidate = invalidateCache
            }
            validator(Validators::validateProducts)
            mapper(Mappers::mapProducts)
        }

    suspend fun fetchProduct(id: Int, invalidateCache: Boolean): RequestResult<Product> =
        krepost.fetchDataMappedAndErrorMapped<ProductDto, Product, ErrorDto, DummyError> {
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
        val result2 = krepost.fetchWithErrorMapped<FetchDataDto, ErrorDto, DummyError> {
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
            errorMapper { error -> DummyError(error.message) }
        }
        return result2
    }


    private suspend fun <T> getData(): FetchDataDto {
        delay(Random.nextLong(2000))
        return if (Random.nextBoolean()) {
            throw when (Random.nextInt(2)) {
                0 -> SerializationException("Test serialization exception")
                1 -> HttpException(
                    Response.error<T>(
                        500,
                        ResponseBody.create(
                            MediaType.parse("application/json"),
                            "{\"message\": \"Test 500 exception\"}"
                        )
                    )
                )

                else -> error("Other error")
            }
        } else
            FetchDataDto(if (Random.nextBoolean()) "Successful response" else "")
    }
}

