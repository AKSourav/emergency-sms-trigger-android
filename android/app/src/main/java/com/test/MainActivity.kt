package com.test

import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint
import com.facebook.react.defaults.DefaultReactActivityDelegate
import android.os.Bundle
import android.content.Intent
import android.util.Log

class MainActivity : ReactActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun getMainComponentName(): String = "test"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start background service
        Log.d(TAG, "Starting SMSBackgroundService")
        val serviceIntent = Intent(this, SMSBackgroundService::class.java)
        startForegroundService(serviceIntent)
    }

    override fun createReactActivityDelegate(): ReactActivityDelegate {
        return DefaultReactActivityDelegate(
            this,
            mainComponentName,
            DefaultNewArchitectureEntryPoint.fabricEnabled
        )
    }

    // Remove manual receiver registration to rely on manifest-declared receiver
}