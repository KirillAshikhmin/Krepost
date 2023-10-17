package ru.kirillashikhmin.krepost.cache

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ru.kirillashikhmin.krepost.serializator.KrepostSerializer
import java.io.File
import java.io.OutputStreamWriter
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.*
import kotlin.reflect.KType


@Suppress("unused", "TooGenericExceptionCaught")
class LocalFileCache(cachePath: String, private val serializer: KrepostSerializer) : KrepostCache {

    companion object {
        const val TAG = "LocalFileCache"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    var cacheDir: File = File(cachePath, "responses")
    val ext = ".json"

    init {
        cacheDir.mkdirs()
    }

    private class Cache(val data: Any)


    @Suppress("ReturnCount")
    override fun <T> get(
        type: KType,
        key: String,
        keyArguments: String,
        cacheTime: Long?,
        deleteIfOutdated: Boolean,
    ): CacheResult<T> {
        try {
            val cacheKey = "$key:$keyArguments"
            val fileName = getFileNameByKey(cacheKey) ?: return CacheResult(null)
            val date = getDateFromFile(cacheKey, fileName)
            val valid =
                checkItemValid(fileName, date + (cacheTime ?: Long.MAX_VALUE), deleteIfOutdated)
            Log.d(TAG, "Get cache: $fileName isValid:$valid")
            var value: T? = null
            var valueString = ""
            if (valid || !deleteIfOutdated) {
                try {
                    val file = File(cacheDir, fileName)
                    valueString = file.readText(Charset.forName("UTF-8"))
                } catch (e: Throwable) {
                    Log.e(TAG, "Unable read cache file for key: $key", e)
                }
                try {
                    value = serializer.deserialize(valueString, type)
                } catch (e: Throwable) {
                    Log.e(TAG, "Unable decode cache for key: $key", e)
                }
            }
            return CacheResult(value, date, !valid)
        } catch (e: Throwable) {
            Log.e(TAG, "Unable get cache for key: $key", e)
            return CacheResult(null)
        }
    }

    override fun <T> write(
        type: KType,
        key: String,
        keyArguments: String,
        data: T,
    ) {
        try {
            val cacheKey = "$key:$keyArguments"
            val currentFile = getFileNameByKey(cacheKey)
            if (currentFile != null) deleteFile(currentFile)
            val fileName = "${cacheKey}#${Date().time}${ext}"

            Log.d(TAG, "Write cache: $fileName")
            cacheDir.mkdirs()
            val file = File(cacheDir, fileName)

            val json = serializer.serialize(data, type)

            val outputStream = file.outputStream()
            val osw = OutputStreamWriter(outputStream)
            osw.write(json)
            osw.close()

        } catch (e: Throwable) {
            Log.e(TAG, "Unable write cache for key: $key", e)
        }
    }

    override fun delete(key: String, keyArguments: String) {
        val cacheKey = "$key:$keyArguments"
        val currentFile = getFileNameByKey(cacheKey)
        if (currentFile != null) deleteFile(currentFile)
    }


    private fun deleteFile(fileName: String) {
        if (fileName.isNotBlank()) {
            val file = File(cacheDir, fileName)
            if (file.exists()) file.delete()
        }
    }

    private fun getValidUntil(key: String): Long {
        val cacheKey = URLEncoder.encode(key, "UTF-8")
        val fileName = getFileNameByKey(cacheKey) ?: return 0
        return getDateFromFile(cacheKey, fileName)
    }

    @Suppress("SwallowedException")
    private fun getDateFromFile(key: String, fileName: String): Long {
        try {
            val time = fileName.replace(ext, "").replace(key, "").replace("#", "")
            if (time.isNotBlank()) {
                val validUntilLong = time.toLongOrNull()
                if (validUntilLong != null) return validUntilLong
            }
        } catch (e: Throwable) {
            //ignored
        }

        return Long.MIN_VALUE
    }


    private fun checkItemValid(
        fileName: String,
        validUntil: Long,
        deleteIfOutdated: Boolean = true,
    ): Boolean {
        if (validUntil >= Date().time)
            return true
        if (deleteIfOutdated) deleteFile(fileName)
        return false
    }

    private fun getFileNameByKey(
        key: String,
    ): String? = cacheDir.listFiles()?.firstOrNull { it.name.startsWith("$key#") }?.name


    fun clear() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            cacheDir.deleteRecursively()
        }
    }

}
