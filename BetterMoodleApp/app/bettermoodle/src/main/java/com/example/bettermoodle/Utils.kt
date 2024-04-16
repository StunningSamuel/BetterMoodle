package com.example.bettermoodle

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


class JsonInterface(
    username: String,
    password: String,
    private val baseIP: String

) {
    private var lastResponse = JSONObject()
    private var sesskey: String? = null;
    private var userid: String? = null;
    private val timeoutSeconds = 30L
    private var client = OkHttpClient.Builder()
        .addInterceptor(BasicAuthInterceptor(username, password))
        //.retryOnConnectionFailure(true)
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()

    enum class HTTPMethod {
        GET, POST
    }

    fun addCacheToClient(username: String, password: String, cacheDirectory: String) {
        this.client = OkHttpClient.Builder()
            .addInterceptor(BasicAuthInterceptor(username, password))
            //.retryOnConnectionFailure(true)
            .cache(
                Cache(
                    directory = File(cacheDirectory),
                    maxSize = 10L * 1024L * 1024L
                )
            )
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build()

    }

    @JvmOverloads
    fun connectToApi(
        url: String,
        method: HTTPMethod = HTTPMethod.GET,
        body: String? = null
    ): CompletableFuture<Response> {
        // error handling is done by caller
        // just return response
        // keep in mind future needs to block to get result
        val responseFuture = CompletableFuture<Response>()
        val request = Request.Builder()
            .url(url)
            .request(method, body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logStackTrace("HTTP Tag", e)
                responseFuture.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d(
                    "HTTP Tag",
                    "We got this response: ${
                        response.peekBody(Long.MAX_VALUE).string()
                    }" // prevent logging from closing the connection yet

                )
                Log.d("HTTP Tag", "We got this code " + response.code)
                responseFuture.complete(response)
            }
        })

        return responseFuture;


    }


    fun tagNotifications(x: Any): Unit {

    }

    fun getCalendarEvents(x: Any): Unit {

    }

    fun refreshTokens(x: Any): Unit {

    }

}

@Suppress("SameParameterValue")
fun logStackTrace(tag: String, e: Exception) {
    Log.e(tag, e.cause.toString())
}

fun getPrefs(context: Context): EncryptedSharedPreferences? {
    var masterKeyAlias: String? = null
    try {
        masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    } catch (e: Exception) {
        logStackTrace("Security Tag", e)
    }
    var sharedPreferences: EncryptedSharedPreferences? = null
    try {
        sharedPreferences =
            EncryptedSharedPreferences.create( // passing a file name to share a preferences
                "preferences",
                masterKeyAlias!!,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences
    } catch (e: GeneralSecurityException) {
        logStackTrace("Security Tag", e)
    } catch (e: IOException) {
        logStackTrace("Security Tag", e)
    }
    return sharedPreferences
}

private fun Request.Builder.request(
    method: JsonInterface.HTTPMethod = JsonInterface.HTTPMethod.GET,
    body: String? = null
): Request.Builder {
    return if (method == JsonInterface.HTTPMethod.GET) {
        this
    } else {
        val jsonBody = body!!.toRequestBody("application/json".toMediaType())
        this.post(jsonBody)
    }
}

fun Any?.isNull(): Boolean {
    return this == null
}
