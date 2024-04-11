package com.example.bettermoodle

import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.WeekViewEntity
import java.util.Calendar

data class MyEvent(
    val id: Long,
    val title: String,
    val startTime: Calendar,
    val endTime: Calendar
)

class ScheduleAdapter() : WeekView.SimpleAdapter<MyEvent>() {


    override fun onCreateEntity(item: MyEvent): WeekViewEntity {
        return WeekViewEntity.Event.Builder(item)
            .setId(item.id)
            .setTitle(item.title)
            .setStartTime(item.startTime)
            .setEndTime(item.endTime)
            .build()
    }
}