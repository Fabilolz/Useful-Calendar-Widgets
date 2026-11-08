package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalendarEvent
import com.example.model.WidgetCustomization
import com.example.ui.theme.PlusJakartaSans
import java.util.Calendar

/**
 * Express Dual Schedule: Morning vs Afternoon split layout with dynamic font scaling, Surface borders, and themes
 */
@Composable
fun ExpressSplitWidgetView(
    events: List<CalendarEvent>,
    targetDay: Calendar = Calendar.getInstance(),
    customization: WidgetCustomization = WidgetCustomization(),
    onEventClick: (CalendarEvent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accentColor = customization.getPrimaryAccent()
    val secondaryColor = customization.getSecondaryAccent()
    val textColor = customization.getPrimaryTextColor()
    val subTextColor = customization.getSecondaryTextColor()
    val shape = RoundedCornerShape(customization.cornerRadiusDp.dp)

    val fontScale = customization.fontScale

    val noonCal = targetDay.clone() as Calendar
    noonCal.set(Calendar.HOUR_OF_DAY, 13)
    noonCal.set(Calendar.MINUTE, 0)
    val noonMillis = noonCal.timeInMillis

    val now = System.currentTimeMillis()
    val candidateEvents = if (customization.hidePastEvents) {
        events.filter { it.endMillis >= now }
    } else {
        events
    }

    val morningEvents = candidateEvents.filter { it.startMillis < noonMillis }
    val afternoonEvents = candidateEvents.filter { it.startMillis >= noonMillis }

    Surface(
        modifier = modifier
            .clip(shape)
            .testTag("express_split_widget"),
        shape = shape,
        color = customization.getResolvedBackgroundColor(),
        border = BorderStroke(1.2.dp, customization.getResolvedBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "DAY FOCUS & SPLIT",
                color = accentColor,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = (12f * fontScale).sp,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Morning Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (customization.darkTheme) Color(0xFF212226) else Color(0xFFEFF1F5))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "MORNING",
                        color = Color(0xFFE65100),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = (10f * fontScale).sp,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (morningEvents.isEmpty()) {
                        Text(
                            text = "No morning events",
                            color = subTextColor,
                            fontFamily = PlusJakartaSans,
                            fontSize = (11f * fontScale).sp
                        )
                    } else {
                        morningEvents.take(2).forEach { ev ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .clickable { onEventClick(ev) }
                            ) {
                                Text(
                                    text = ev.title,
                                    color = textColor,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = (12f * fontScale).sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = ev.formatTimeRange(androidx.compose.ui.platform.LocalContext.current),
                                    color = Color(0xFFE65100),
                                    fontFamily = PlusJakartaSans,
                                    fontSize = (10.5f * fontScale).sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Afternoon & Evening Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (customization.darkTheme) Color(0xFF2D1829) else Color(0xFFF6ECF5))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "AFTERNOON / EVE",
                        color = secondaryColor,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = (10f * fontScale).sp,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (afternoonEvents.isEmpty()) {
                        Text(
                            text = "No afternoon events",
                            color = subTextColor,
                            fontFamily = PlusJakartaSans,
                            fontSize = (11f * fontScale).sp
                        )
                    } else {
                        afternoonEvents.take(2).forEach { ev ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .clickable { onEventClick(ev) }
                            ) {
                                Text(
                                    text = ev.title,
                                    color = if (customization.darkTheme) Color(0xFFFF80AB) else Color(0xFF880E4F),
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = (12f * fontScale).sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = ev.formatTimeRange(androidx.compose.ui.platform.LocalContext.current),
                                    color = secondaryColor,
                                    fontFamily = PlusJakartaSans,
                                    fontSize = (10.5f * fontScale).sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
