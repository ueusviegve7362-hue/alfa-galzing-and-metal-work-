package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

/**
 * Local alert service for admin punch-in alerts and employee shift reminders.
 * Operates purely using Android system NotificationManager without external Firebase FCM dependencies.
 */
object AlfaNotificationService {

    private const val CHANNEL_ID = "alfa_glazing_notifications"
    private const val CHANNEL_NAME = "Alfa Glazing Attendance & Shift Alerts"

    fun sendLocalNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Admin punch-in alerts and Employee shift reminders"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun sendPunchInAdminNotification(context: Context, employeeName: String, timeStr: String, siteName: String) {
        sendLocalNotification(
            context = context,
            title = "📌 Employee Punch In Alert",
            message = "$employeeName has successfully punched in at $timeStr at $siteName."
        )
    }

    fun sendShiftReminderNotification(context: Context, employeeName: String) {
        sendLocalNotification(
            context = context,
            title = "⏰ Shift Reminder",
            message = "Hello $employeeName, your shift is starting soon! Don't forget to punch in at the job site."
        )
    }
}
