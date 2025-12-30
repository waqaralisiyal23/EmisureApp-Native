package com.emisure.app

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging Service
 * 
 * Handles FCM messages at the native Android level for reliable
 * background message processing.
 */
class EmisureFCMService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "EmisureFCM"
        private const val PREFS_NAME = "emisure_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"

        /**
         * Get the current FCM token from SharedPreferences
         */
        fun getStoredToken(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_FCM_TOKEN, null)
        }

        /**
         * Fetch and store a fresh FCM token
         */
        fun refreshToken(context: Context, onComplete: (String?) -> Unit) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    saveToken(context, token)
                    Log.i(TAG, "=====================================")
                    Log.i(TAG, "🔑 FCM TOKEN REFRESHED")
                    Log.i(TAG, token)
                    Log.i(TAG, "=====================================")
                    onComplete(token)
                } else {
                    Log.e(TAG, "Failed to get FCM token: ${task.exception?.message}")
                    onComplete(null)
                }
            }
        }

        private fun saveToken(context: Context, token: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
        }
    }

    /**
     * Called when a new FCM token is generated.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "=====================================")
        Log.d(TAG, "🔄 NEW FCM TOKEN")
        Log.d(TAG, token)
        Log.d(TAG, "=====================================")
        
        // Save token to SharedPreferences
        saveToken(applicationContext, token)
        
        // TODO: Send token to your server if needed
    }

    /**
     * Called when a message is received.
     * This is called for both foreground and background messages.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d(TAG, "=====================================")
        Log.d(TAG, "📩 FCM MESSAGE RECEIVED (Native)")
        Log.d(TAG, "From: ${message.from}")
        Log.d(TAG, "Data: ${message.data}")
        Log.d(TAG, "Notification: ${message.notification?.title} - ${message.notification?.body}")
        Log.d(TAG, "=====================================")
        
        // Handle data payload
        if (message.data.isNotEmpty()) {
            handleDataMessage(message.data)
        }
    }

    /**
     * Handle the data message and perform actions.
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val action = data["action"] ?: return
        
        Log.d(TAG, "=====================================")
        Log.d(TAG, "⚡ HANDLING ACTION: $action")
        Log.d(TAG, "=====================================")
        
        val devicePolicyService = DevicePolicyService(applicationContext)
        
        // Ensure notification permission is locked whenever we receive a message
        if (devicePolicyService.isDeviceOwner()) {
            devicePolicyService.lockNotificationPermission()
        }
        
        when (action) {
            "lock" -> {
                // Lock the device with payment screen
                val title = data["title"] ?: "Payment Required"
                val sellerName = data["sellerName"] ?: ""
                val sellerPhone = data["sellerPhone"] ?: ""
                val amountDue = data["amountDue"] ?: ""
                val dueDate = data["dueDate"] ?: ""
                val message = data["message"] ?: ""
                
                devicePolicyService.showLockScreen(
                    title,
                    sellerName,
                    sellerPhone,
                    amountDue,
                    dueDate,
                    message
                )
                
                Log.d(TAG, "✅ Device locked via FCM (Native)")
            }
            
            "unlock" -> {
                // Unlock the device
                devicePolicyService.hideLockScreen()
                
                // Send broadcast to close lock screen activity
                val unlockBroadcast = Intent("com.emisure.UNLOCK_DEVICE")
                unlockBroadcast.setPackage(packageName)
                sendBroadcast(unlockBroadcast)
                
                // Re-enable calls
                if (devicePolicyService.isDeviceOwner()) {
                    devicePolicyService.enableOutgoingCalls()
                }
                
                Log.d(TAG, "✅ Device unlocked via FCM (Native)")
            }
            
            "disable_factory_reset" -> {
                val success = devicePolicyService.disableFactoryReset()
                Log.d(TAG, if (success) "✅ Factory reset disabled" else "❌ Failed to disable factory reset")
            }
            
            "enable_factory_reset" -> {
                val success = devicePolicyService.enableFactoryReset()
                Log.d(TAG, if (success) "✅ Factory reset enabled" else "❌ Failed to enable factory reset")
            }
            
            "status" -> {
                val status = devicePolicyService.getDeviceStatus()
                Log.d(TAG, "📊 Device Status: $status")
            }
            
            "disable_debugging" -> {
                val success = devicePolicyService.disableUSBDebugging()
                Log.d(TAG, if (success) "✅ USB debugging disabled" else "❌ Failed to disable USB debugging")
            }
            
            "enable_debugging" -> {
                val success = devicePolicyService.enableUSBDebugging()
                Log.d(TAG, if (success) "✅ USB debugging enabled" else "❌ Failed to enable USB debugging")
            }
            
            "enforce_notifications" -> {
                val success = devicePolicyService.enforceNotificationsEnabled()
                Log.d(TAG, if (success) "✅ Notifications enforced" else "❌ Failed to enforce notifications")
            }
            
            "release_device" -> {
                // IMPORTANT: This action fully releases the device when customer pays all installments
                // After this, the app can be uninstalled and all restrictions are removed
                Log.d(TAG, "🎉 Received RELEASE_DEVICE command - Customer paid all installments!")
                val success = devicePolicyService.releaseDevice()
                if (success) {
                    Log.d(TAG, "✅ Device fully released to customer!")
                    Log.d(TAG, "   - All restrictions removed")
                    Log.d(TAG, "   - Device Owner status cleared")
                    Log.d(TAG, "   - App can now be uninstalled")
                } else {
                    Log.e(TAG, "❌ Failed to release device")
                }
            }
            
            else -> {
                Log.d(TAG, "Unknown action: $action")
            }
        }
    }
}
