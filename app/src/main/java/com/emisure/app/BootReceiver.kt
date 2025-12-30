package com.emisure.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Boot Receiver for Emisure
 * 
 * This receiver is triggered when the device boots up.
 * It re-applies device restrictions and lock screen to ensure they persist across reboots.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "EmisureBoot"
        
        // List of all boot-related actions we listen for
        private val BOOT_ACTIONS = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_USER_UNLOCKED
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "=====================================")
        Log.i(TAG, "📱 BootReceiver triggered!")
        Log.i(TAG, "Action: ${intent.action}")
        Log.i(TAG, "=====================================")
        
        if (intent.action in BOOT_ACTIONS) {
            Log.i(TAG, "Recognized boot action, re-applying restrictions...")
            applyRestrictionsAndShowLock(context)
        }
    }
    
    private fun applyRestrictionsAndShowLock(context: Context) {
        val devicePolicyService = DevicePolicyService(context)
        
        Log.i(TAG, "Device Owner: ${devicePolicyService.isDeviceOwner()}")
        Log.i(TAG, "Device Admin: ${devicePolicyService.isDeviceAdmin()}")
        Log.i(TAG, "Device Locked: ${devicePolicyService.isDeviceLocked()}")
        
        // Re-apply restrictions if we are device owner
        if (devicePolicyService.isDeviceOwner()) {
            // Re-disable factory reset
            if (devicePolicyService.isFactoryResetDisabled()) {
                devicePolicyService.disableFactoryReset()
                Log.i(TAG, "✅ Factory reset restriction re-applied")
            }
            
            // Re-disable safe mode boot
            devicePolicyService.disableSafeModeBoot()
            Log.i(TAG, "✅ Safe mode boot restriction re-applied")
            
            // NOTE: USB debugging restriction is commented out for development
            // Uncomment for production to prevent ADB access
            // devicePolicyService.disableUSBDebugging()
            // Log.i(TAG, "✅ USB debugging restriction re-applied")
            
            // Lock notification permission and enforce it
            devicePolicyService.lockNotificationPermission()
            devicePolicyService.enforceNotificationsEnabled()
            Log.i(TAG, "✅ Notification permission locked and enforced")
        }
        
        // Re-show lock screen if device was locked
        if (devicePolicyService.isDeviceLocked()) {
            Log.i(TAG, "🔒 Device is locked, showing lock screen...")
            
            val lockInfo = devicePolicyService.getLockInfo()
            Log.i(TAG, "Lock info: $lockInfo")
            
            val lockIntent = LockScreenActivity.createIntent(
                context,
                lockInfo["title"] as? String ?: "Payment Required",
                lockInfo["sellerName"] as? String ?: "",
                lockInfo["sellerPhone"] as? String ?: "",
                lockInfo["amountDue"] as? String ?: "",
                lockInfo["dueDate"] as? String ?: "",
                lockInfo["message"] as? String ?: "Please pay your outstanding balance to unlock this device."
            )
            
            // Add flags to ensure it starts properly from boot
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
            }
            
            context.startActivity(lockIntent)
            
            // Re-disable outgoing calls
            if (devicePolicyService.isDeviceOwner()) {
                devicePolicyService.disableOutgoingCalls()
            }
            
            Log.i(TAG, "✅ Lock screen shown on boot")
        } else {
            Log.i(TAG, "Device is not locked, skipping lock screen")
        }
    }
}
