package ru.kirillashikhmin.repo.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import ru.kirillashikhmin.krepost.*
import ru.kirillashikhmin.repository.Repository

@ExperimentalSerializationApi
class MainViewModel : ViewModel() {

    private val _resultLiveData = MutableLiveData<String>()
    val resultLiveData: LiveData<String> = _resultLiveData
    private val _statusLiveData = MutableLiveData<String>()
    val statusLiveData: LiveData<String> = _statusLiveData

    private val repository = Repository()

    fun fetchData() {
        viewModelScope.launch {
            setLoadingState()
            val fetchResult = repository.fetchData4()
            setFetchingStatus(fetchResult)
        }
    }

    fun fetchWithError() {
        Krepost()
    }

    fun invokeAction() {
        TODO("Not yet implemented")
    }

    fun fetchWithoutFlow() {
        TODO("Not yet implemented")
    }

    private fun setLoadingState() {
        _statusLiveData.value = "Loading..."
        _resultLiveData.value = ""
    }

    private fun setFetchingStatus(result: RequestResult<*>) {
        val code = "(${result.status::class.java.simpleName})"
        if (result.isSuccess()) {
            if (result.isCached()) _statusLiveData.value =
                "Cached ${code}. CacheTime: ${result.cacheTime}"
            else if (result.isOutdated()) _statusLiveData.value =
                "Outdated ${code}. CacheTime: ${result.cacheTime}"
            else _statusLiveData.value = "Success $code"

            _resultLiveData.value = if (result.isEmpty()) "empty" else  result.asSuccess().value.toString().toIndentString()
        } else if (result.isFailure()) {
            _statusLiveData.value = "Failure $code"
            _resultLiveData.value = result.message + "\n" +
                    result.error?.toString()?.toIndentString() +"\n" +
                    result.throwable?.toString()?.toIndentString()
        } else {
            _statusLiveData.value = "Invalid state $code"
            _resultLiveData.value = "¯\\_(ツ)_/¯"
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

        this@toIndentString.filter { it != ' ' }.forEach { char ->
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
