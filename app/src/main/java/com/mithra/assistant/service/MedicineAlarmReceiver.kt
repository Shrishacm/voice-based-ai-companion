package com.mithra.assistant.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mithra.assistant.MainActivity
import com.mithra.assistant.R

private const val TAG = "VOICE_ALARM"
private const val CHANNEL_ID = "mithra_reminder_channel"
private const val NOTIFICATION_ID_BASE = 100

class MedicineAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1)
        val title = intent.getStringExtra("title") ?: "Medicine Reminder"
        val type = intent.getStringExtra("type") ?: "medicine"

        Log.d(TAG, "Reminder triggered: $title (type=$type, id=$reminderId)")

        createNotificationChannel(context)
        showNotification(context, reminderId, title, type)

        // Re-schedule recurring medicine reminders from the DB (no fragile title parsing)
        if (type == "medicine") {
            (context.applicationContext as? com.mithra.assistant.MithraApplication)
                ?.scheduleRemindersFromDatabase()
        }
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Medicine Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for medicine and appointment reminders"
            enableVibration(true)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun showNotification(context: Context, id: Long, title: String, type: String) {
        val notificationId = (NOTIFICATION_ID_BASE + id).toInt()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = when (type) {
            "medicine"    -> "Time to take: $title"
            "appointment" -> "Upcoming: $title"
            else          -> title
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Mithra Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }
}
