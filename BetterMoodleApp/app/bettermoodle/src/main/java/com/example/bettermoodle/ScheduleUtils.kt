package com.example.bettermoodle

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.security.crypto.EncryptedSharedPreferences
import com.islandparadise14.mintable.MinTimeTableView
import com.islandparadise14.mintable.model.ScheduleDay
import com.islandparadise14.mintable.model.ScheduleEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ExecutionException
import kotlin.concurrent.thread

data class Course(
    val day: Int,
    val name: String,
    val code: String,
    val crn: String,
    val instructors: String,
    val campus: String,
    val time: String,
    val type: String,
    val location: String,

    )


internal class ScheduleModel(preferences: EncryptedSharedPreferences) : ViewModel() {
    private var jsonInterface: JsonInterface
    private var username: String
    private var password: String
    private var baseIP: String
    var events: MutableLiveData<ArrayList<ScheduleEntity>> = MutableLiveData()
        private set

    init {
        this.username = preferences.getString("username", "")!!
        this.password = preferences.getString("password", "")!!
        // yes we hardcoded the port, deal with it
        this.baseIP = "http://${preferences.getString("ip", "")!!}:5000/"
        val jsonInterface = JsonInterface(username, password, baseIP)
        this.jsonInterface = jsonInterface
    }


    private fun courseFromJson(x: JSONObject, day: Int): Course {
        return Course(
            day,
            x["name"] as String,
            x["code"] as String,
            x["crn"] as String,
            x["instructors"] as String,
            x["campus"] as String,
            x["time"] as String,
            x["type"] as String,
            x["location"] as String,
        )
    }

    private fun courseArrayList(jsonString: String): ArrayList<Course> {
        val scheduleObj = JSONObject(jsonString)
        val names = scheduleObj.names()!!.toList().filter { elem ->
            elem.toString().length == 1
        }
        val namesMap = mapOf(
            Pair("M", ScheduleDay.MONDAY),
            Pair("T", ScheduleDay.TUESDAY),
            Pair("W", ScheduleDay.WEDNESDAY),
            Pair("R", ScheduleDay.THURSDAY)
        )
        val courses = ArrayList<Course>()
        for (i in names) {
            val coursesInDay = scheduleObj.get(i.toString()) as JSONArray
            for (course in 0..<coursesInDay.length()) {
                val courseJSONObject = coursesInDay[course] as JSONObject
                courses.add(courseFromJson(courseJSONObject, namesMap[i]!!))
            }
        }
        return courses
    }

    private fun populateEvents(jsonString: String): ArrayList<ScheduleEntity> {
        val coursesArray = courseArrayList(jsonString)
        val eventsArray = ArrayList<ScheduleEntity>()
        for ((num, course) in coursesArray.withIndex()) {
            val title = "${course.name} (${course.code} - ${course.crn})"
            var (start, end) = course.time.split("-").map { it.trim() }
            val to24Hour = { time: String ->
                val hour = time.split(":")[0].toInt()
                val (minute, time12hrMarker) = time.split(":")[1].split(" ")
                if (hour < 12 && time12hrMarker == "pm") {
                    "${hour + 12}:$minute"
                } else {
                    "$hour:$minute"
                }
            }
            start = to24Hour(start)
            end = to24Hour(end)
            val event =
                ScheduleEntity(num, title, course.location, course.day, start, end)
            eventsArray.add(event)
        }
        return eventsArray
    }


    fun setSchedule(table: MinTimeTableView) {
        val scheduleResponseFuture = jsonInterface.connectToApi(this.baseIP + "schedule")
        var scheduleResponse: String
        thread(start = true) {
            try {
                scheduleResponse = scheduleResponseFuture.get().body!!.string()
                val eventsList = populateEvents(scheduleResponse)
                events.postValue(eventsList)
            } catch (e: ExecutionException) {
                // If we reach this block, all shit has hit the fan or someone fucked up the url
                logStackTrace("HTTP Tag", e)
            }
        }
    }

}


