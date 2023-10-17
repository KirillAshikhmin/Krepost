package ru.kirillashikhmin.repo.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import ru.kirillashikhmin.krepost.RequestResult
import ru.kirillashikhmin.krepost.asSuccess
import ru.kirillashikhmin.krepost.isCached
import ru.kirillashikhmin.krepost.isEmpty
import ru.kirillashikhmin.krepost.isFailure
import ru.kirillashikhmin.krepost.isOutdated
import ru.kirillashikhmin.krepost.isSuccess
import ru.kirillashikhmin.repository.Repository
import ru.kirillashikhmin.repository.dto.FetchDataDto
import ru.kirillashikhmin.repository.models.Product
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
class MainViewModel : ViewModel() {

    var invalidateCache: Boolean = false
    var cacheDir: String = "."
    private val _resultLiveData = MutableLiveData<String>()
    val resultLiveData: LiveData<String> = _resultLiveData
    private val _statusLiveData = MutableLiveData<String>()
    val statusLiveData: LiveData<String> = _statusLiveData

    private val repository = lazy { Repository(cacheDir) }

    fun fetchData() {
        viewModelScope.launch {
            setLoadingState()
            val fetchResult: RequestResult<FetchDataDto>
            val elapsed = measureTimeMillis {
                fetchResult = repository.value.fetchData(invalidateCache)
            }
            setFetchingStatus(fetchResult, elapsed)
        }
    }

    fun fetchProducts() {
        viewModelScope.launch {
            setLoadingState()
            val fetchResult: RequestResult<List<Product>>
            val elapsed = measureTimeMillis {
                fetchResult = repository.value.fetchProducts(invalidateCache)
            }
            setFetchingStatus(fetchResult, elapsed)
        }
    }

    fun fetchProduct(text: String?) {
        viewModelScope.launch {
            val id = text?.toIntOrNull()

            if (id == null) {
                setErrorState()
                return@launch
            }

            setLoadingState()
            val fetchResult: RequestResult<Product>
            val elapsed = measureTimeMillis {
                fetchResult = repository.value.fetchProduct(id, invalidateCache)
            }
            setFetchingStatus(fetchResult, elapsed)
        }
    }


    private fun setLoadingState() {
        _statusLiveData.value = "Loading..."
        _resultLiveData.value = ""
    }

    private fun setErrorState() {
        _statusLiveData.value = "Error!"
        _resultLiveData.value = ""
    }

    private fun setFetchingStatus(result: RequestResult<*>, elapsed: Long) {
        val statusName = result.status::class.java.simpleName
        val code = result.status.code
        when {
            result.isSuccess() -> {
                val empty = result.isEmpty()
                val emptyText = if (empty) "(empty)" else ""

                var resultText = if (result.isOutdated())
                    "Outdated $statusName ($code). CacheTime: ${result.cacheTime}"
                else if (result.isCached())
                    "Cached $statusName ($code). CacheTime: ${result.cacheTime}"
                else "Success $statusName ($code) $emptyText"

                resultText += "\nTime: $elapsed"
                _statusLiveData.value = resultText

                _resultLiveData.value =
                    if (result.isEmpty()) "Empty body"
                    else result.asSuccess().value.toString().toIndentString()
            }

            result.isFailure() -> {
                _statusLiveData.value = "Failure $statusName ($code)\nTime: $elapsed"
                _resultLiveData.value = (result.message ?: "¯\\_(ツ)_/¯") + "\n" +
                        result.error?.toString() + "\n" +
                        result.throwable?.toString()
            }
        }
    }


    /**
     * Just reformat data-class toString() for good output
     * Thanks to https://gist.github.com/bnorm/71c7973b4b3f928e855a183a3e56c791
     */
    private fun String.toIndentString(): String = buildString(length) {
        var indent = 0

        fun line() {
            appendLine()
            repeat(2 * indent) { append(' ') }
        }

        this@toIndentString.forEach { char ->
            when (char) {
                ')', ']', '}' -> {
                    indent--
                    line()
                    append(char)
                }

                '=' -> append(" = ")
                '(', '[', '{', ',' -> {
                    append(char)
                    if (char != ',') indent++
                    line()
                }

                else -> append(char)
            }
        }
    }

}
