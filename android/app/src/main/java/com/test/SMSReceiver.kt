package com.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Telephony
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import android.util.Log
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import com.test.NotificationUtils

class SMSReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SMSReceiver"
    }

    private fun getSettings(context: Context): Pair<String, List<Contact>> {
        val prefs = context.getSharedPreferences("SMSEventSettings", Context.MODE_PRIVATE)
        val triggerWord = prefs.getString("triggerWord", "URGENT") ?: "URGENT"
        val contactsJson = prefs.getString("allowedContacts", "[]") ?: "[]"
        
        val contacts = mutableListOf<Contact>()
        try {
            val jsonArray = JSONArray(contactsJson)
            for (i in 0 until jsonArray.length()) {
                val contactObj = jsonArray.getJSONObject(i)
                contacts.add(
                    Contact(
                        name = contactObj.optString("name", ""),
                        phoneNumber = contactObj.optString("phoneNumber", "")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing contacts", e)
        }
        
        return Pair(triggerWord, contacts)
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive called with action: ${intent.action}")
        
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            try {
                // Get saved settings
                val (triggerWord, allowedContacts) = getSettings(context)
                Log.d(TAG, "Trigger word: $triggerWord")
                Log.d(TAG, "Allowed contacts: ${allowedContacts.size}")

                // Log each allowed contact's phone number
                for (contact in allowedContacts) {
                    Log.d(TAG, "Allowed contact phone number: ${contact.phoneNumber}")
                }

                // Check if this is a test message
                val testMessage = intent.getStringExtra("test_message")
                val testSender = intent.getStringExtra("test_sender")
                
                if (testMessage != null && testSender != null) {
                    // Handle test message
                    if (testMessage.contains(triggerWord, ignoreCase = true)) {
                        handleUrgentMessage(context, testSender, testMessage)
                    }
                } else {
                    // Handle real SMS
                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    Log.d(TAG, "Number of messages received: ${messages.size}")
                    
                    for (sms in messages) {
                        // Retrieve SMS details
                        val originatingAddress = sms.originatingAddress ?: "Unknown"
                        val messageBody = sms.messageBody ?: ""
                        val timestamp = sms.timestampMillis
                        val serviceCenterAddress = sms.serviceCenterAddress ?: "Unknown"
                        
                        // Log all available details
                        Log.d(TAG, "SMS Details:")
                        Log.d(TAG, "  Sender           : $originatingAddress")
                        Log.d(TAG, "  Message Content  : $messageBody")
                        Log.d(TAG, "  Timestamp        : $timestamp")
                        Log.d(TAG, "  Service Center   : $serviceCenterAddress")

                        // Check if sender is in allowed contacts with flexible matching
                        val isAllowedSender = allowedContacts.any { 
                            val normalizedAllowed = it.phoneNumber.replace(Regex("[^0-9]"), "")
                            val normalizedSender = originatingAddress.replace(Regex("[^0-9]"), "")
                            // This condition will be true if:
                            // - The sender's number ends with the allowed contact's number, or
                            // - The allowed contact's number ends with the sender's number.
                            normalizedSender.endsWith(normalizedAllowed) || normalizedAllowed.endsWith(normalizedSender)
                        }
                        
                        if (isAllowedSender) {
                            Log.d(TAG, "Sender is in allowed contacts")
                            // Check if message contains trigger word
                            if (messageBody?.contains(triggerWord, ignoreCase = true) == true) {
                                Log.d(TAG, "Message contains trigger word: $triggerWord")
                                // Get sender name from contacts list
                                val senderName = allowedContacts.find { 
                                    it.phoneNumber.replace(Regex("[^0-9]"), "") == originatingAddress 
                                }?.name ?: originatingAddress
                                handleUrgentMessage(context, senderName, messageBody)
                            }
                        } else {
                            Log.d(TAG, "Sender not in allowed contacts")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message", e)
            }
        }
    }

    private fun handleUrgentMessage(context: Context, sender: String, message: String) {
        try {
            Log.d(TAG, "Handling urgent message from $sender")
            // Get audio manager
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
            // Check for DND permission
            val notificationManager = 
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
                !notificationManager.isNotificationPolicyAccessGranted) {
                Log.d(TAG, "DND permission not granted")
                // Show notification without changing audio settings
                NotificationUtils.showNotification(context, "Permission Required", 
                    "Please grant Do Not Disturb access to enable audio alerts")
                return
            }

            // Switch to normal mode
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            
            // Set volumes to maximum
            val maxRingVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            val maxNotificationVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
            val maxAlarmVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            
            audioManager.apply {
                setStreamVolume(AudioManager.STREAM_RING, maxRingVolume, 0)
                setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxNotificationVolume, 0)
                setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVolume, 0)
            }
            
            // Show notification using the utility class
            NotificationUtils.showNotification(
                context,
                "Urgent Message",
                "Urgent message received from $sender: $message"
            )
            
            Log.d(TAG, "Urgent message handled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling urgent message", e)
        }
    }
}

data class Contact(
    val name: String,
    val phoneNumber: String
) 