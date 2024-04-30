package com.example.bettermoodle

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.security.crypto.EncryptedSharedPreferences
import com.islandparadise14.mintable.model.ScheduleDay
import com.islandparadise14.mintable.model.ScheduleEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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


internal class ScheduleModel(context: Context) : ViewModel() {
    private var preferences: EncryptedSharedPreferences
    private var jsonInterface: JsonInterface
    private var username: String
    private var password: String
    var events: MutableLiveData<ArrayList<ScheduleEntity>> = MutableLiveData()
        private set

    init {
        this.jsonInterface = JsonInterface(context, "schedule")
        this.username = jsonInterface.username
        this.preferences = jsonInterface.preferences
        this.password = jsonInterface.password
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
        val eventsArray = ArrayList<ScheduleEntity>()
        val colors = listOf(
            "#b1e6ff",
            "#ffafa0",
            "#ffdf9f",
            "#a6ff98",
            "#acb2cb",
            "#e1ffb3",
            "#c9ccff",
            "#75f3ff",
            "#f9c064",
            "#c3baab",
            "#d4a373",
            "#e9ff70"
        )
        val colorMap: MutableMap<String, String> = if (preferences.contains("colorMap")) {
            Json.decodeFromString(preferences.getString("colorMap", "")!!)
        } else {
            HashMap()
        }
        val coursesArray = courseArrayList(jsonString)
        for ((num, course) in coursesArray.withIndex()) {
            if (!colorMap.containsKey(course.name)) {
                colorMap[course.name] = colors.random()
            }
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
                ScheduleEntity(
                    num,
                    title,
                    course.location,
                    course.day,
                    start,
                    end,
                    colorMap[course.name]!!,
                    "#000000"
                )
            eventsArray.add(event)
        }
        // serialize the entire schedule and color map into encrypted preferences when we're done
        // if we haven't already
        serializeToPreferencesIfNotFound(preferences, "schedule", jsonString)
        serializeToPreferencesIfNotFound(preferences, "colorMap", Json.encodeToString(colorMap))
        return eventsArray
    }


    @JvmOverloads
    fun setSchedule(refreshSchedule: Boolean = false, refreshColors: Boolean = false) {
        val refreshValue = { key: String, refresh: Boolean ->
            if (refresh)
                preferences.edit().remove(key).apply()
        }
        refreshValue("schedule", refreshSchedule)
        refreshValue("colorMap", refreshColors)

        thread(start = true) {
            try {
                val scheduleResponse = if (!preferences.contains("schedule")) {
                    val scheduleResponseFuture =
                        jsonInterface.connectToApi()
                    scheduleResponseFuture.get().body!!.string()
                } else {
                    preferences.getString("schedule", "")!!
                }
                val eventsList = populateEvents(scheduleResponse)
                events.postValue(eventsList)
            } catch (e: ExecutionException) {
                // If we reach this block, all shit has hit the fan or someone fucked up the url
                logStackTrace("HTTP Tag", e)
            }
        }
    }


}

fun scheduleToArrayList(context: Context): ArrayList<String>? {
    val preferences = getPrefs(context)
    val cachedScheduleString = preferences!!.getString("schedule", "")
    if (cachedScheduleString.isNullOrBlank())
        return null
    val scheduleToArray = { json: String ->
        val coursesList = ArrayList<String>()
        val schedule = JSONObject(json)
        val days = listOf(schedule["M"], schedule["T"], schedule["W"], schedule["R"])
        val daysMap = hashMapOf(
            Pair(0, "Monday"),
            Pair(1, "Tuesday"),
            Pair(2, "Wednesday"),
            Pair(3, "Thursday"),
        )
        days.forEachIndexed { index, day ->
            daysMap[index]?.let { coursesList.add(it) }
            val courseNames = ArrayList<String>()
            for (i in 0..<(day as JSONArray).length()) {
                val obj = day[i] as JSONObject
                courseNames.add("${obj["name"]}\n${obj["time"]} (${obj["location"]})")
            }
            coursesList.addAll(courseNames)
        }

        coursesList
    }

    return scheduleToArray(cachedScheduleString)

}



