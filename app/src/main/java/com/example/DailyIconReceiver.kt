package com.example

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.util.Calendar

class DailyIconReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        update(context)
    }

    companion object {
        internal fun update(context: Context) {
            val calendar = Calendar.getInstance()
            val today = calendar.get(Calendar.DAY_OF_MONTH)
            val packageManager = context.packageManager

            // Enable today's shortcut before disabling yesterday's on older Android versions.
            val days = listOf(today) + (1..31).filter { it != today }
            val changes = days.mapNotNull { day ->
                val suffix = day.toString().padStart(2, '0')
                val component = ComponentName(context.packageName, "com.example.LauncherDay$suffix")
                val state = packageManager.getComponentEnabledSetting(component)
                val enabled = state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                    (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && day == 1)
                val shouldEnable = day == today
                if (enabled == shouldEnable) null else component to if (shouldEnable) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
            }
            if (changes.isNotEmpty()) {
                val flags = PackageManager.DONT_KILL_APP
                if (Build.VERSION.SDK_INT >= 33) {
                    packageManager.setComponentEnabledSettings(changes.map { (component, state) ->
                        PackageManager.ComponentEnabledSetting(component, state, flags)
                    })
                } else {
                    changes.forEach { (component, state) ->
                        packageManager.setComponentEnabledSetting(component, state, flags)
                    }
                }
            }

            calendar.add(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val alarm = PendingIntent.getBroadcast(
                context, 0, Intent(context, DailyIconReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            context.getSystemService(AlarmManager::class.java)
                .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarm)
        }
    }
}
