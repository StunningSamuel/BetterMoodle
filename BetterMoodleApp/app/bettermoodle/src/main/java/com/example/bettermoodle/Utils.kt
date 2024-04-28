package com.example.bettermoodle

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit


interface JsonObserver {
    fun onJsonReady(json: String)
}

class JsonInterface(
    context: Context,
    endpoint: String
) {
    private var endpoint: String
    private val context: Context
    private var liveData: MutableLiveData<String>
    var preferences: EncryptedSharedPreferences
        private set
    var username: String
        private set
    var password: String
        private set
    private var ip: String

    init {
        this.endpoint = endpoint
        this.context = context
        this.preferences = getPrefs(context)!!
        this.username = preferences.getString("username", "")!!
        this.password = preferences.getString("password", "")!!
        this.ip = "http://${preferences.getString("ip", "")!!}:5000/$endpoint"
        this.liveData = MutableLiveData<String>()
    }

    private val timeoutSeconds = 30L
    private var client = OkHttpClient.Builder()
        .addInterceptor(BasicAuthInterceptor(username, password))
        .cache(
            Cache(
                directory = context.cacheDir,
                maxSize = 10L * 1024L * 1024L
            )
        )
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()

    enum class HTTPMethod {
        GET, POST;

    }


    @JvmOverloads
    fun connectToApi(
        method: HTTPMethod = HTTPMethod.GET,
        body: String? = null
    ): CompletableFuture<Response> {
        // error handling is done by caller
        // just return response
        // keep in mind future needs to block to get result
        val responseFuture = CompletableFuture<Response>()
        val request = Request.Builder()
            .url(this.ip)
            .request(method, body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logStackTrace("HTTP Tag", e)
                responseFuture.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("HTTP Tag", "We got this code " + response.code)
                responseFuture.complete(response)
            }
        })

        return responseFuture;


    }


    @JvmOverloads
    fun postResponseToListener(forceNetwork: Boolean = false): MutableLiveData<String> {
        if (getCachedJson() == "" || forceNetwork) {
            val future = connectToApi()
            future.whenComplete { t, _ ->
                try {
                    val jsonString = t.body!!.string()
                    serializeToPreferencesIfNotFound(
                        preferences,
                        this.endpoint,
                        jsonString,
                        true
                    )
                    liveData.postValue(jsonString)
                } catch (e: ExecutionException) {
                    logStackTrace("HTTP Tag", e)
                } catch (e: InterruptedException) {
                    logStackTrace("HTTP Tag", e)
                }

            }
        } else {
            // if we already have a response, just return from cache
            // THIS CLASS IS NOT RESPONSIBLE FOR REFRESHING DATA
            // MAKE SURE ANOTHER CLASS DOES
            liveData.postValue(getCachedJson())
        }
        return liveData
    }


    fun getCachedJson(): String {
        return preferences.getString(endpoint, "")!!
    }

    private fun getCachedJsonTimestamp(): Duration? {
        val expires: Long = JSONObject(getCachedJson()).getLong("expires")
        val expiresDateTime =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(expires), ZoneId.systemDefault())
        val now = LocalDateTime.now()
        return Duration.between(now, expiresDateTime)
    }

    fun moodleInvalidator(): Boolean {
        return getCachedJsonTimestamp()!!.toHours() >= 8
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

@JvmOverloads
fun serializeToPreferencesIfNotFound(
    preferences: EncryptedSharedPreferences,
    key: String,
    value: String,
    overwrite: Boolean = false
) {
    if (!preferences.contains(key) || overwrite)
        preferences.edit().putString(key, value)
            .apply()
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


fun JSONArray.toList(): ArrayList<Any> {
    val tempList = arrayListOf<Any>();
    for (i in 0..<this.length()) {
        tempList.add(this[i])
    }

    return tempList
}
