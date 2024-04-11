package com.example.bettermoodle

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


internal class ScheduleModel : ViewModel() {
    private val events = MutableLiveData<List<MyEvent>>()
    val eventsView: LiveData<List<MyEvent>> = events
}

class JsonInterface(
    private var username: String,
    private var password: String,
    private var activity: Activity

) {
    private var sesskey: String? = null;
    private var userid: String? = null;
    private val timeoutSeconds = 5L
    private val client = OkHttpClient.Builder()
        .addInterceptor(BasicAuthInterceptor(username, password))
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()


    fun connectToApi(url: String): CompletableFuture<Response> {
        // error handling is done by caller
        val responseFuture = CompletableFuture<Response>();
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logStackTrace("HTTP Tag", e)
                responseFuture.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body!!.string()
                Log.d(
                    "HTTP Tag",
                    "We got this response: $responseBody"

                )
                Log.d("HTTP Tag", "We got this code " + response.code)
                responseFuture.complete(response)
            }
        })

        return responseFuture;


    }

    @Suppress("SameParameterValue")
    private fun logStackTrace(tag: String, e: IOException) {
        Log.e(tag, e.cause.toString())
    }

    fun getSchedule(scheduleJson: String): Unit {
        var events = mutableListOf<MyEvent>()
        val json = JSONObject(scheduleJson)
    }

    fun tagNotifications(x: Any): Unit {

    }

    fun getCalendarEvents(x: Any): Unit {

    }

    fun refreshTokens(x: Any): Unit {

    }

}