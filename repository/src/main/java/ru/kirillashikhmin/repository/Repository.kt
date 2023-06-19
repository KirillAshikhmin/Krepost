package ru.kirillashikhmin.repository

import kotlinx.coroutines.delay
import kotlinx.serialization.ExperimentalSerializationApi
import ru.kirillashikhmin.krepost.CacheStrategy
import ru.kirillashikhmin.krepost.IKrepostError
import ru.kirillashikhmin.krepost.Krepost
import ru.kirillashikhmin.krepost.RequestResult
import ru.kirillashikhmin.krepost.RequestStatus
import ru.kirillashikhmin.repository.dto.ProductsDto

@ExperimentalSerializationApi
class Repository {

    private var service: DummyJsonService =
        RepositoryCore.createService("https://dummyjson.com/", DummyJsonService::class.java)

    suspend fun getProducts(): RequestResult<ProductsDto> {
        return  try {
            val result = service.getProducts()
            RequestResult.Success.Value(result)
        } catch (t : Throwable) {
            RequestResult.Failure.Error(RequestStatus.Unprocessable, "Error", t)
        }
    }

    val krepost = Krepost.initialize {  }

    class MyKrepostError : IKrepostError {
        override val errorMessage: String
            get() = "I'm error"
    }

    suspend fun fetchData1(): RequestResult<Int> =
        krepost.fetchDataMappedErrorMapped<String, Int, MyKrepostError, MyKrepostError> {
            action { getData() }
            cache {
                name = "aaa"
                strategy = CacheStrategy.IfExist
                arguments(1)
            }
            mapper { data -> data.toInt() }
        }

    suspend fun fetchData2(): RequestResult<Int> {
        val result2 = krepost.fetchDataMapped<String, Int> {
            action { getData2() }
            cache {
                name = "bbb"
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
            cache {
                name = "ccc"
                strategy = CacheStrategy.IfNotAvailable
                arguments(3)
            }
        }

        return result3
    }

    suspend fun getData() : String {
        delay(100)
        return "1"
    }

    suspend fun getData2() : String {
        delay(100)
        return "2"
    }

    suspend fun getData3() : String {
        delay(100)
        return "3"
    }
}
