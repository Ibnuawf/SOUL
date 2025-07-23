package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.SoulDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        private const val CHANNEL_ID = "soul_reminders_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "Alarm received!")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SoulDatabase.getDatabase(context)
                val dao = db.journalDao()

                val now = Calendar.getInstance()
                val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
                val todayEntry = dao.getEntryByDate(todayDateString)
                
                if (todayEntry?.isCompleted == true) {
                    ReminderScheduler.scheduleNextAlarm(context)
                    return@launch
                }

                val oneYearAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -365)
                }
                val oneYearAgoDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(oneYearAgo.time)
                val oneYearAgoEntry = dao.getEntryByDate(oneYearAgoDateString)
                val hasOneYearAgoEntry = oneYearAgoEntry?.isCompleted == true

                val title = if (hasOneYearAgoEntry) {
                    "A trace of your past self..."
                } else {
                    "A quiet moment for your soul"
                }

                val message = if (hasOneYearAgoEntry) {
                    "One year ago today, you left a piece of your soul behind. Tap here to unlock this memory."
                } else {
                    "Take a gentle pause to check in with yourself and capture today's memories."
                }

                sendNotification(context, title, message)

                ReminderScheduler.scheduleNextAlarm(context)
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error in onReceive", e)
            }
        }
    }

    private fun sendNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Soul Daily Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for daily journal reflections"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
