package com.example.bettermoodle

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.FixedColorProvider

/**
 * Implementation of App Widget functionality.
 */


class MyAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}

private object DarkColor : WidgetColor {
    override val backgroundColor: Color
        get() = Color.Black
    override val onBackgroundColor: Color
        get() = Color.Black

}

private object LightColor : WidgetColor {
    override val backgroundColor: Color
        get() = Color.White
    override val onBackgroundColor: Color
        get() = Color.White

}

private interface WidgetColor {
    val backgroundColor: Color
    val onBackgroundColor: Color
}

class ScheduleWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val scheduleArray = scheduleToArrayList(context)
        provideContent {
            Box(
                modifier = GlanceModifier.fillMaxSize().background(Color(0xfffff8dc)),
                Alignment.Center
            ) {
                LazyColumn(modifier = GlanceModifier.fillMaxWidth().padding(20.dp)) {
                    items(scheduleArray.size) { index ->
//                        val insertSpacer =
//                            if (index < scheduleArray.size && scheduleArray[(index + 1)] in listOf(
//                                    "Monday",
//                                    "Tuesday",
//                                    "Wednesday",
//                                    "Thursday"
//                                )
//                            ) {
//                            } else {
//                            }
                        Text(
                            style = TextStyle(
                                FixedColorProvider(Color.Black),
                                fontSize = 20.sp,
                                fontFamily = FontFamily("@font/comfortaacegular"),
                            ),
                            text = scheduleArray[index] + "\n"
                        )
                    }


                }
            }
        }
    }
}

