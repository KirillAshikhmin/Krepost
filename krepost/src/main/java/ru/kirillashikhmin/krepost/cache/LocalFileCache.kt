package ru.kirillashikhmin.krepost.cache

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ru.kirillashikhmin.krepost.KrepostCacheException
import ru.kirillashikhmin.krepost.serializers.KrepostSerializer
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
            val valid = checkItemValid(
                fileName = fileName,
                validUntil = date + (cacheTime ?: Long.MAX_VALUE),
                deleteIfOutdated = deleteIfOutdated
            )
            var value: T? = null
            if (valid || !deleteIfOutdated) {
                val file = File(cacheDir, fileName)
                val valueString = file.readText(Charset.forName("UTF-8"))
                value = serializer.deserialize(valueString, type)
            }
            return CacheResult(value, date, !valid)
        } catch (e: Throwable) {
            Log.e(TAG, "Unable get cache for key: $key", e)
            throw KrepostCacheException(e, write = false)
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
            cacheDir.mkdirs()
            val file = File(cacheDir, fileName)

            val json = serializer.serialize(data, type)

            val outputStream = file.outputStream()
            val osw = OutputStreamWriter(outputStream)
            osw.write(json)
            osw.close()

        } catch (e: Throwable) {
            Log.e(TAG, "Unable write cache for key: $key", e)
            throw KrepostCacheException(e, write = true)
        }
    }

    override fun delete(key: String, keyArguments: String) {
        try {
            val cacheKey = "$key:$keyArguments"
            val currentFile = getFileNameByKey(cacheKey)
            if (currentFile != null) deleteFile(currentFile)
        } catch (e: Throwable) {
            Log.e(TAG, "Unable write cache for key: $key", e)
            throw KrepostCacheException(e, write = false)
        }
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
