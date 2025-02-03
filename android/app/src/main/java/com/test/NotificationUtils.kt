package com.test

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import android.util.Log

object NotificationUtils {
    private const val TAG = "NotificationUtils"
    
    fun showNotification(context: Context, title: String, message: String) {
        try {
            Log.d(TAG, "Showing notification - Title: $title, Message: $message")
            
            val notificationManager = 
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "urgent_sms",
                    "Urgent SMS",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for Urgent SMS notifications"
                }
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created")
            }

            val builder = NotificationCompat.Builder(context, "urgent_sms")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            notificationManager.notify(1, builder.build())
            Log.d(TAG, "Notification sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }
} 