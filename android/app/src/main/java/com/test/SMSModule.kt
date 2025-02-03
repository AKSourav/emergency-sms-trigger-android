package com.test

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.content.Context
import android.media.AudioManager
import android.app.NotificationManager
import android.util.Log
import android.provider.Settings
import com.facebook.react.bridge.WritableNativeMap
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import com.facebook.react.bridge.*
import com.test.NotificationUtils

class SMSModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    
    private val prefs: SharedPreferences by lazy {
        reactContext.getSharedPreferences("SMSEventSettings", Context.MODE_PRIVATE)
    }
    
    override fun getName() = "SMSModule"

    @ReactMethod
    fun requestPermissions(promise: Promise) {
        val activity = currentActivity
        if (activity == null) {
            promise.reject("ACTIVITY_NULL", "Activity is null")
            return
        }

        try {
            // Permissions we can request at runtime (SMS and volume control).
            val permissions = arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS
            )

            // Check if any permission is missing.
            val needsPermission = permissions.any {
                ContextCompat.checkSelfPermission(reactContext, it) != PackageManager.PERMISSION_GRANTED
            }

            // Check if Notification Policy (DND) access has been granted.
            val notificationManager = reactContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val hasDNDAccess = notificationManager.isNotificationPolicyAccessGranted

            // Prepare the result that tells us permission status.
            val result = WritableNativeMap().apply {
                putBoolean("smsPermissionsGranted", !needsPermission)
                putBoolean("dndAccessGranted", hasDNDAccess)
            }

            // Request runtime permissions if needed.
            if (needsPermission) {
                ActivityCompat.requestPermissions(activity, permissions, PERMISSION_CODE)
            }

            // If DND access is not granted, open the settings screen so the user can enable it.
            if (!hasDNDAccess) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                reactContext.startActivity(intent)
            }

            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("PERMISSION_ERROR", e.message)
        }
    }

    @ReactMethod
    fun testUrgentSMS() {
        Log.d("SMSModule", "Testing urgent SMS functionality")
        try {
            val notificationManager = reactContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                throw Exception("DND access not granted")
            }

            val audioManager = reactContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            
            val maxRingVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            val maxNotificationVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
            val maxAlarmVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            
            audioManager.apply {
                setStreamVolume(AudioManager.STREAM_RING, maxRingVolume, AudioManager.FLAG_SHOW_UI)
                setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxNotificationVolume, AudioManager.FLAG_SHOW_UI)
                setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVolume, AudioManager.FLAG_SHOW_UI)
            }
            
            // Use the utility class for showing notifications
            NotificationUtils.showNotification(
                reactContext,
                "Test Urgent Message",
                "This is a test URGENT message from 123456789"
            )
            
            Log.d("SMSModule", "Test completed successfully")
        } catch (e: Exception) {
            Log.e("SMSModule", "Error during test", e)
        }
    }

    @ReactMethod
    fun checkNotificationPolicyAccess(): Boolean {
        val notificationManager = reactContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    @ReactMethod
    fun updateSettings(triggerWord: String, contacts: ReadableArray, promise: Promise) {
        try {
            val contactsJson = JSONArray()
            for (i in 0 until contacts.size()) {
                val contact = contacts.getMap(i)
                contact?.let { // Null safety check
                    val contactJson = JSONObject().apply {
                        put("name", it.getString("name") ?: "")
                        put("phoneNumber", it.getString("phoneNumber") ?: "")
                    }
                    contactsJson.put(contactJson)
                }
            }

            prefs.edit().apply {
                putString("triggerWord", triggerWord)
                putString("allowedContacts", contactsJson.toString())
                apply()
            }
            
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("UPDATE_SETTINGS_ERROR", e.message)
        }
    }

    @ReactMethod
    fun getSettings(promise: Promise) {
        try {
            val triggerWord = prefs.getString("triggerWord", "URGENT") ?: "URGENT"
            val contactsJson = prefs.getString("allowedContacts", "[]") ?: "[]"
            
            val result = WritableNativeMap().apply {
                putString("triggerWord", triggerWord)
                putArray("allowedContacts", convertJsonToArray(contactsJson))
            }
            
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("GET_SETTINGS_ERROR", e.message)
        }
    }

    private fun convertJsonToArray(jsonStr: String): WritableArray {
        val array = WritableNativeArray()
        try {
            val jsonArray = JSONArray(jsonStr)
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val contact = WritableNativeMap().apply {
                    putString("name", jsonObject.optString("name", ""))
                    putString("phoneNumber", jsonObject.optString("phoneNumber", ""))
                }
                array.pushMap(contact)
            }
        } catch (e: Exception) {
            Log.e("SMSModule", "Error converting JSON to array", e)
        }
        return array
    }

    fun isAllowedContact(phoneNumber: String): Boolean {
        val contactsJson = prefs.getString("allowedContacts", "[]") ?: return false
        try {
            val jsonArray = JSONArray(contactsJson)
            for (i in 0 until jsonArray.length()) {
                val contact = jsonArray.optJSONObject(i) ?: continue
                if (contact.optString("phoneNumber", "") == phoneNumber) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e("SMSModule", "Error checking allowed contact", e)
        }
        return false
    }

    fun getTriggerWord(): String {
        return prefs.getString("triggerWord", "URGENT") ?: "URGENT"
    }

    @ReactMethod
    fun requestDNDAccess(promise: Promise) {
        val notificationManager = reactContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            // Open the settings for the user to grant DND access
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            reactContext.startActivity(intent)
            promise.resolve("DND settings opened")
        } else {
            promise.resolve("DND access already granted")
        }
    }

    companion object {
        private const val PERMISSION_CODE = 123
    }
} 