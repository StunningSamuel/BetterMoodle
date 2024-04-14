package com.example.bettermoodle

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.security.crypto.EncryptedSharedPreferences
import com.alamkanak.weekview.WeekViewEntity
import com.alamkanak.weekview.jsr310.WeekViewSimpleAdapterJsr310
import com.alamkanak.weekview.jsr310.setEndTime
import com.alamkanak.weekview.jsr310.setStartTime
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.LocalDateTime
import java.util.concurrent.ExecutionException
import kotlin.concurrent.thread

data class MyEvent(
    val id: Int,
    val title: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime
)

data class Course(
    val name: String,
    val code: String,
    val crn: String,
    val instructors: String,
    val campus: String,
    val time: String,
    val type: String,
    val location: String
)

data class Schedule(
    val courseMap: Map<String, Course> = HashMap()
)

internal class ScheduleModel(preferences: EncryptedSharedPreferences) : ViewModel() {
    private var jsonInterface: JsonInterface
    private var username: String
    private var password: String
    private var baseIP: String
    private var events: MutableLiveData<List<MyEvent>> = MutableLiveData<List<MyEvent>>()
    private var schedule: Schedule? = null
    public val eventsView: LiveData<List<MyEvent>>
        get() = events

    init {
        this.username = preferences.getString("username", "")!!
        this.password = preferences.getString("password", "")!!
        // yes we hardcoded the port, deal with it
        this.baseIP = "http://${preferences.getString("ip", "")!!}:5000/"
        val jsonInterface = JsonInterface(username, password, baseIP)
        this.jsonInterface = jsonInterface
    }

    fun populateEvents() {
        if (schedule.isNull()) {
            setSchedule()
        }
        for ((key, value) in schedule!!.courseMap) {
            Log.d("Schedule Tag", "$key : $value")
            var num = 0
            val title = "${value.name} (${value.code} - ${value.crn})"
            val now = LocalDateTime.now()
            val constructDate = { time: String ->
                val (hour, minute) = time.split(":").map { value -> value.toInt() }
                LocalDateTime.of(now.year, now.monthValue, now.dayOfMonth, hour, minute)
            }
            val (start, end) = value.time.split("\\d+:\\d+".toRegex())
            @Suppress("UNUSED_CHANGED_VALUE") val event =
                MyEvent(num++, title, constructDate(start), constructDate(end))
            this.events.value?.plus(event)
        }

    }


    @OptIn(ExperimentalStdlibApi::class)
    private fun setSchedule() {

        val scheduleResponseFuture = jsonInterface.connectToApi(this.baseIP + "schedule")
        var schedule = Schedule()
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val scheduleAdapter: JsonAdapter<Schedule> = moshi.adapter<Schedule>()
        thread(start = true) {
            try {
                val scheduleResponse = scheduleResponseFuture.get()
                schedule = scheduleAdapter.fromJson(scheduleResponse.body!!.string())!!
            } catch (e: ExecutionException) {
                // If we reach this block, all shit has hit the fan or someone fucked up the url
                logStackTrace("HTTP Tag", e)
            }
        }

        this.schedule = schedule
    }

}


class ScheduleAdapter() : WeekViewSimpleAdapterJsr310<MyEvent>() {


    override fun onCreateEntity(item: MyEvent): WeekViewEntity {
        return WeekViewEntity.Event.Builder(item)
            .setId(item.id.toLong())
            .setTitle(item.title)
            .setStartTime(item.startTime)
            .setEndTime(item.endTime)
            .build()
    }


}