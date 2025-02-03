package com.test

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import android.provider.Telephony
import com.test.SMSReceiver

class SMSBackgroundService : Service() {
    private lateinit var smsReceiver: SMSReceiver

    override fun onCreate() {
        Log.e(TAG, "SMSBackgroundService onCreate() called")
        // Initialize and register receiver
        smsReceiver = SMSReceiver()
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        registerReceiver(smsReceiver, filter)
        super.onCreate()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "SMSBackgroundService onStartCommand() called")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, 
                "SMS Background Service", 
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Listener Active")
            .setContentText("Listening for urgent messages")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.e(TAG, "SMSBackgroundService onTaskRemoved() called")
        super.onTaskRemoved(rootIntent)
        val restartServiceIntent = Intent(applicationContext, this::class.java)
        startService(restartServiceIntent)
    }
     override fun onDestroy() {
        Log.e(TAG, "SMSBackgroundService onDestroy() called")
        super.onDestroy()
        // Unregister receiver to prevent memory leaks
        unregisterReceiver(smsReceiver)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "SMS_SERVICE_CHANNEL"
        private const val TAG = "SMSBackgroundService"
    }
}