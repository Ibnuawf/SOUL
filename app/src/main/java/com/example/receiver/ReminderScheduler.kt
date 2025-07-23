package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.SoulDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"
    const val REQUEST_CODE = 4242

    fun scheduleNextAlarm(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SoulDatabase.getDatabase(context)
                val dao = db.journalDao()
                
                val now = Calendar.getInstance()
                val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
                val todayEntry = dao.getEntryByDate(todayDateString)
                val isTodayCompleted = todayEntry?.isCompleted == true

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, ReminderReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Cancel any existing alarm first
                alarmManager.cancel(pendingIntent)

                val targetTime = Calendar.getInstance()
                
                if (isTodayCompleted) {
                    // Today is complete! Schedule tomorrow at 9:00 AM
                    targetTime.add(Calendar.DAY_OF_YEAR, 1)
                    targetTime.set(Calendar.HOUR_OF_DAY, 9)
                    targetTime.set(Calendar.MINUTE, 0)
                    targetTime.set(Calendar.SECOND, 0)
                    targetTime.set(Calendar.MILLISECOND, 0)
                    Log.d(TAG, "Today completed. Scheduling tomorrow at 9:00 AM: ${targetTime.time}")
                } else {
                    // Today is NOT complete.
                    val nineAMToday = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 9)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    if (now.before(nineAMToday)) {
                        // It's before 9:00 AM. Schedule for 9:00 AM today!
                        targetTime.set(Calendar.HOUR_OF_DAY, 9)
                        targetTime.set(Calendar.MINUTE, 0)
                        targetTime.set(Calendar.SECOND, 0)
                        targetTime.set(Calendar.MILLISECOND, 0)
                        Log.d(TAG, "Before 9 AM. Scheduling today at 9:00 AM: ${targetTime.time}")
                    } else {
                        // It's after 9:00 AM today and not completed. Schedule in 2 hours!
                        targetTime.add(Calendar.HOUR_OF_DAY, 2)
                        Log.d(TAG, "After 9 AM and incomplete. Scheduling in 2 hours: ${targetTime.time}")
                    }
                }

                val triggerTime = targetTime.timeInMillis

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling alarm", e)
            }
        }
    }
}
