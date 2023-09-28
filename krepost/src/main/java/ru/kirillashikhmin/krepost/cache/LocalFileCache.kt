package ru.kirillashikhmin.krepost.cache

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kirillashikhmin.krepost.serializator.KrepostSerializer
import java.io.File
import java.io.OutputStreamWriter
import java.math.BigInteger
import java.net.URLEncoder
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*


@Suppress("unused")
class LocalFileCache(cachePath: String, val serializer: KrepostSerializer) : KrepostCache {

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

    private class Cache<T: Any>(val data : T)


    override fun <T : Any> get(
        key: String,
        keyArguments: String,
        cacheTime: Long?,
        deleteIfOutdated: Boolean,
    ): Pair<T?, Long> {
        try {
            val cacheKeyArguments = keyArguments.md5()
            val cacheKey = "$key:$cacheKeyArguments"
            val fileName = getFileNameByKey(cacheKey) ?: return Pair(null, 0)
            val date = getDateFromFile(cacheKey, fileName)
            val valid =
                checkItemValid(fileName, date + (cacheTime ?: Long.MAX_VALUE), deleteIfOutdated)
            Log.d(TAG, "Get cache: $fileName isValid:$valid")
            var value: T? = null
            var valueString = ""
            if (valid) {
                try {
                    val file = File(cacheDir, fileName)
                    valueString = file.readText(Charset.forName("UTF-8"))
                } catch (e: Throwable) {
                    Log.e(TAG, "Unable read cache file for key: $key", e)
                }
                try {
                    value = serializer.deserialize(valueString)
//                    val zzz : Cache<T> = json.decodeFromString(valueString)
//                    value = zzz.data
                } catch (e: Throwable) {
                    Log.e(TAG, "Unable decode cache for key: $key", e)
                }
            }
            return Pair(value, date)
        } catch (e: Throwable) {
            Log.e(TAG, "Unable get cache for key: $key", e)
            return Pair(null, 0)
        }
    }

    override fun <T : Any> write(
        key: String,
        keyArguments: String,
        data: T,
    ) {
        try {
            val cacheKeyArguments = keyArguments.md5()
            val cacheKey = "$key:$cacheKeyArguments"
            val currentFile = getFileNameByKey(cacheKey)
            if (currentFile != null) deleteFile(currentFile)
            val fileName = "${cacheKey}#${Date().time}${ext}"

            Log.d(TAG, "Write cache: $fileName")
            cacheDir.mkdirs()
            val file = File(cacheDir, fileName)

            val json = serializer.serialize(data)
//            val json = json.encodeToString(Cache(data))

            val outputStream = file.outputStream()
            val osw = OutputStreamWriter(outputStream)
            osw.write(json)
            osw.close()

        } catch (e: Throwable) {
            Log.e(TAG, "Unable write cache for key: $key", e)
        }
    }

    override fun delete(key: String, keyArguments: String) {
        val cacheKeyArguments = keyArguments.md5()
        val cacheKey = "$key:$cacheKeyArguments"
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

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(this.toByteArray())).toString(16).padStart(32, '0')
    }

}
