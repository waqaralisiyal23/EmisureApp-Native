package com.emisure.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import android.util.Log

/**
 * Service class that handles all device policy operations.
 * This class provides methods to lock device, disable factory reset, and check admin status.
 */
class DevicePolicyService(private val context: Context) {

    companion object {
        private const val TAG = "DevicePolicyService"
    }

    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val componentName: ComponentName =
        EmisureDeviceAdminReceiver.getComponentName(context)

    /**
     * Checks if this app is currently a device admin.
     */
    fun isDeviceAdmin(): Boolean {
        return devicePolicyManager.isAdminActive(componentName)
    }

    /**
     * Checks if this app is the device owner.
     * Device Owner has more privileges than regular Device Admin.
     */
    fun isDeviceOwner(): Boolean {
        return devicePolicyManager.isDeviceOwnerApp(context.packageName)
    }

    /**
     * Gets device identifiers (Android ID and IMEI).
     * IMEI requires Device Owner on Android 10+.
     */
    fun getDeviceIdentifiers(): Map<String, String> {
        val androidId = try {
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
        
        var imei1 = "Not available"
        var imei2 = "Not available"
        
        if (isDeviceOwner()) {
            try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    imei1 = telephonyManager.getImei(0) ?: "Not available"
                    imei2 = telephonyManager.getImei(1) ?: "Not available"
                } else {
                    @Suppress("DEPRECATION")
                    imei1 = telephonyManager.deviceId ?: "Not available"
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Permission denied for IMEI: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error getting IMEI: ${e.message}")
            }
        }
        
        return mapOf(
            "androidId" to androidId,
            "imei1" to imei1,
            "imei2" to imei2,
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL
        )
    }


    /**
     * Locks the device immediately.
     * Requires Device Admin permission.
     */
    fun lockDevice(): Boolean {
        return try {
            if (isDeviceAdmin()) {
                devicePolicyManager.lockNow()
                Log.i(TAG, "Device locked successfully")
                true
            } else {
                Log.e(TAG, "Cannot lock device: Not a device admin")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error locking device: ${e.message}")
            false
        }
    }

    /**
     * Disables factory reset by adding user restrictions.
     * Requires Device Owner permission.
     */
    fun disableFactoryReset(): Boolean {
        return try {
            if (isDeviceOwner()) {
                // Disable factory reset via settings
                devicePolicyManager.addUserRestriction(
                    componentName,
                    UserManager.DISALLOW_FACTORY_RESET
                )
                Log.i(TAG, "Factory reset disabled successfully")
                true
            } else {
                Log.e(TAG, "Cannot disable factory reset: Not device owner")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling factory reset: ${e.message}")
            false
        }
    }

    /**
     * Enables factory reset by removing user restrictions.
     * Requires Device Owner permission.
     */
    fun enableFactoryReset(): Boolean {
        return try {
            if (isDeviceOwner()) {
                devicePolicyManager.clearUserRestriction(
                    componentName,
                    UserManager.DISALLOW_FACTORY_RESET
                )
                Log.i(TAG, "Factory reset enabled successfully")
                true
            } else {
                Log.e(TAG, "Cannot enable factory reset: Not device owner")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling factory reset: ${e.message}")
            false
        }
    }

    /**
     * Checks if factory reset is currently disabled.
     */
    fun isFactoryResetDisabled(): Boolean {
        return try {
            if (isDeviceOwner()) {
                val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
                userManager.hasUserRestriction(UserManager.DISALLOW_FACTORY_RESET)
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking factory reset status: ${e.message}")
            false
        }
    }

    /**
     * Disables safe mode boot.
     * Only available on Android P (API 28) and above.
     * Requires Device Owner permission.
     */
    fun disableSafeModeBoot(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isDeviceOwner()) {
                devicePolicyManager.addUserRestriction(
                    componentName,
                    UserManager.DISALLOW_SAFE_BOOT
                )
                Log.i(TAG, "Safe mode boot disabled successfully")
                true
            } else {
                Log.e(TAG, "Cannot disable safe mode: Not device owner or API < 28")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling safe mode boot: ${e.message}")
            false
        }
    }

    /**
     * Disables USB debugging to prevent ADB access.
     * Requires Device Owner permission.
     */
    fun disableUSBDebugging(): Boolean {
        return try {
            if (isDeviceOwner()) {
                devicePolicyManager.addUserRestriction(
                    componentName,
                    UserManager.DISALLOW_DEBUGGING_FEATURES
                )
                Log.i(TAG, "USB debugging disabled successfully")
                true
            } else {
                Log.e(TAG, "Cannot disable USB debugging: Not device owner")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling USB debugging: ${e.message}")
            false
        }
    }

    /**
     * Enables USB debugging by removing the restriction.
     * Requires Device Owner permission.
     */
    fun enableUSBDebugging(): Boolean {
        return try {
            if (isDeviceOwner()) {
                devicePolicyManager.clearUserRestriction(
                    componentName,
                    UserManager.DISALLOW_DEBUGGING_FEATURES
                )
                Log.i(TAG, "USB debugging enabled successfully")
                true
            } else {
                Log.e(TAG, "Cannot enable USB debugging: Not device owner")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling USB debugging: ${e.message}")
            false
        }
    }

    /**
     * Locks the POST_NOTIFICATIONS permission so users cannot revoke it.
     * This ensures FCM messages are always received.
     * Requires Device Owner permission and Android 13+.
     */
    fun lockNotificationPermission(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isDeviceOwner()) {
                val success = devicePolicyManager.setPermissionGrantState(
                    componentName,
                    context.packageName,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                )
                if (success) {
                    Log.i(TAG, "Notification permission locked successfully")
                } else {
                    Log.e(TAG, "Failed to lock notification permission")
                }
                success
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                // Before Android 13, notifications don't need runtime permission
                Log.i(TAG, "Notification permission not needed for this Android version")
                true
            } else {
                Log.e(TAG, "Cannot lock notification permission: Not device owner")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error locking notification permission: ${e.message}")
            false
        }
    }

    /**
     * Checks if notifications are enabled and forces them to be enabled if not.
     * Uses Device Owner privileges to override user settings via AppOps.
     */
    fun enforceNotificationsEnabled(): Boolean {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val areEnabled = notificationManager.areNotificationsEnabled()
            
            if (!areEnabled && isDeviceOwner()) {
                Log.w(TAG, "Notifications are disabled, attempting to force enable...")
                
                // Use App Ops to force enable notifications
                try {
                    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
                    val uid = context.applicationInfo.uid
                    
                    // OP_POST_NOTIFICATION = 11, MODE_ALLOWED = 0
                    val method = appOps.javaClass.getMethod(
                        "setMode",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        String::class.java,
                        Int::class.javaPrimitiveType
                    )
                    method.invoke(appOps, 11, uid, context.packageName, 0)
                    
                    Log.i(TAG, "Notifications force-enabled via AppOps")
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Could not force-enable via AppOps: ${e.message}")
                    false
                }
            } else {
                if (areEnabled) {
                    Log.i(TAG, "Notifications are already enabled")
                }
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enforcing notifications: ${e.message}")
            false
        }
    }

    /**
     * Shows the persistent lock screen.
     */
    fun showLockScreen(
        title: String = "Payment Required",
        sellerName: String = "",
        sellerPhone: String = "",
        amountDue: String = "",
        dueDate: String = "",
        message: String = "Please pay your outstanding balance to unlock this device."
    ): Boolean {
        return try {
            val intent = LockScreenActivity.createIntent(
                context,
                title,
                sellerName,
                sellerPhone,
                amountDue,
                dueDate,
                message
            )
            context.startActivity(intent)
            
            // Save lock state to preferences
            saveLockState(true, title, sellerName, sellerPhone, amountDue, dueDate, message)
            
            Log.i(TAG, "Lock screen shown successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error showing lock screen: ${e.message}")
            false
        }
    }

    /**
     * Hides the lock screen and stops lock task mode.
     */
    fun hideLockScreen(): Boolean {
        return try {
            // Clear lock state from preferences
            saveLockState(false)
            
            Log.i(TAG, "Lock screen hidden successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding lock screen: ${e.message}")
            false
        }
    }

    /**
     * Checks if the device is currently locked.
     * Uses device protected storage for Direct Boot support.
     */
    fun isDeviceLocked(): Boolean {
        val deviceProtectedContext = context.createDeviceProtectedStorageContext()
        val prefs = deviceProtectedContext.getSharedPreferences("emisure_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_locked", false)
    }

    /**
     * Saves lock state to shared preferences.
     * Uses device protected storage for Direct Boot support.
     * Uses commit() for synchronous persistence to survive reboots.
     */
    private fun saveLockState(
        isLocked: Boolean,
        title: String = "Payment Required",
        sellerName: String = "",
        sellerPhone: String = "",
        amountDue: String = "",
        dueDate: String = "",
        message: String = ""
    ) {
        val deviceProtectedContext = context.createDeviceProtectedStorageContext()
        val prefs = deviceProtectedContext.getSharedPreferences("emisure_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_locked", isLocked)
            putString("lock_title", title)
            putString("seller_name", sellerName)
            putString("seller_phone", sellerPhone)
            putString("amount_due", amountDue)
            putString("due_date", dueDate)
            putString("lock_message", message)
            commit()  // Use commit() for synchronous persistence
        }
        Log.i(TAG, "Lock state saved to device protected storage: isLocked=$isLocked")
    }

    /**
     * Gets the saved lock info from preferences.
     * Uses device protected storage for Direct Boot support.
     */
    fun getLockInfo(): Map<String, Any> {
        val deviceProtectedContext = context.createDeviceProtectedStorageContext()
        val prefs = deviceProtectedContext.getSharedPreferences("emisure_prefs", Context.MODE_PRIVATE)
        return mapOf(
            "isLocked" to prefs.getBoolean("is_locked", false),
            "title" to (prefs.getString("lock_title", "Payment Required") ?: "Payment Required"),
            "sellerName" to (prefs.getString("seller_name", "") ?: ""),
            "sellerPhone" to (prefs.getString("seller_phone", "") ?: ""),
            "amountDue" to (prefs.getString("amount_due", "") ?: ""),
            "dueDate" to (prefs.getString("due_date", "") ?: ""),
            "message" to (prefs.getString("lock_message", "") ?: "")
        )
    }

    /**
     * Disables outgoing calls (except emergency).
     * Requires Device Owner permission.
     */
    fun disableOutgoingCalls(): Boolean {
        return try {
            if (isDeviceOwner()) {
                devicePolicyManager.addUserRestriction(
                    componentName,
                    UserManager.DISALLOW_OUTGOING_CALLS
                )
                Log.i(TAG, "Outgoing calls disabled successfully")
                true
            } else {
                Log.e(TAG, "Cannot disable outgoing calls: Not device owner")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling outgoing calls: ${e.message}")
            false
        }
    }

    /**
     * Enables outgoing calls.
     * Requires Device Owner permission.
     */
    fun enableOutgoingCalls(): Boolean {
        return try {
            if (isDeviceOwner()) {
                devicePolicyManager.clearUserRestriction(
                    componentName,
                    UserManager.DISALLOW_OUTGOING_CALLS
                )
                Log.i(TAG, "Outgoing calls enabled successfully")
                true
            } else {
                Log.e(TAG, "Cannot enable outgoing calls: Not device owner")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling outgoing calls: ${e.message}")
            false
        }
    }

    /**
     * Checks if outgoing calls are disabled.
     */
    fun isOutgoingCallsDisabled(): Boolean {
        return try {
            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            userManager.hasUserRestriction(UserManager.DISALLOW_OUTGOING_CALLS)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking outgoing calls status: ${e.message}")
            false
        }
    }

    /**
     * Gets the device status info as a map.
     */
    fun getDeviceStatus(): Map<String, Any> {
        return mapOf(
            "isDeviceAdmin" to isDeviceAdmin(),
            "isDeviceOwner" to isDeviceOwner(),
            "isFactoryResetDisabled" to isFactoryResetDisabled(),
            "isDeviceLocked" to isDeviceLocked(),
            "isOutgoingCallsDisabled" to isOutgoingCallsDisabled(),
            "androidVersion" to Build.VERSION.SDK_INT,
            "deviceModel" to Build.MODEL,
            "manufacturer" to Build.MANUFACTURER
        )
    }

    /**
     * Fully releases the device when customer pays all installments.
     * This removes all restrictions and Device Owner status.
     * After calling this, the app can be uninstalled by the user.
     */
    fun releaseDevice(): Boolean {
        Log.i(TAG, "🚀 Starting full device release process...")
        
        return try {
            if (!isDeviceOwner()) {
                Log.e(TAG, "Cannot release device: Not device owner")
                return false
            }
            
            // Step 1: Clear lock state
            Log.i(TAG, "Step 1: Clearing lock state...")
            saveLockState(false)
            
            // Step 2: Remove all user restrictions
            Log.i(TAG, "Step 2: Removing user restrictions...")
            try {
                devicePolicyManager.clearUserRestriction(componentName, UserManager.DISALLOW_FACTORY_RESET)
                Log.i(TAG, "  ✓ Factory reset enabled")
            } catch (e: Exception) {
                Log.w(TAG, "  Could not clear factory reset restriction: ${e.message}")
            }
            
            try {
                devicePolicyManager.clearUserRestriction(componentName, UserManager.DISALLOW_SAFE_BOOT)
                Log.i(TAG, "  ✓ Safe mode enabled")
            } catch (e: Exception) {
                Log.w(TAG, "  Could not clear safe boot restriction: ${e.message}")
            }
            
            try {
                devicePolicyManager.clearUserRestriction(componentName, UserManager.DISALLOW_OUTGOING_CALLS)
                Log.i(TAG, "  ✓ Outgoing calls enabled")
            } catch (e: Exception) {
                Log.w(TAG, "  Could not clear calls restriction: ${e.message}")
            }
            
            try {
                devicePolicyManager.clearUserRestriction(componentName, UserManager.DISALLOW_DEBUGGING_FEATURES)
                Log.i(TAG, "  ✓ USB debugging enabled")
            } catch (e: Exception) {
                Log.w(TAG, "  Could not clear debugging restriction: ${e.message}")
            }
            
            // Step 3: Reset notification permission to default
            Log.i(TAG, "Step 3: Resetting notification permission...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    devicePolicyManager.setPermissionGrantState(
                        componentName,
                        context.packageName,
                        android.Manifest.permission.POST_NOTIFICATIONS,
                        DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT
                    )
                    Log.i(TAG, "  ✓ Notification permission reset to default")
                } catch (e: Exception) {
                    Log.w(TAG, "  Could not reset notification permission: ${e.message}")
                }
            }
            
            // Step 4: Re-enable status bar
            Log.i(TAG, "Step 4: Re-enabling status bar...")
            try {
                devicePolicyManager.setStatusBarDisabled(componentName, false)
                Log.i(TAG, "  ✓ Status bar enabled")
            } catch (e: Exception) {
                Log.w(TAG, "  Could not enable status bar: ${e.message}")
            }
            
            // Step 5: Clear lock task packages
            Log.i(TAG, "Step 5: Clearing lock task packages...")
            try {
                devicePolicyManager.setLockTaskPackages(componentName, arrayOf())
                Log.i(TAG, "  ✓ Lock task packages cleared")
            } catch (e: Exception) {
                Log.w(TAG, "  Could not clear lock task packages: ${e.message}")
            }
            
            // Step 6: REMOVE DEVICE OWNER STATUS (final step, cannot be undone!)
            Log.i(TAG, "Step 6: Removing Device Owner status...")
            try {
                devicePolicyManager.clearDeviceOwnerApp(context.packageName)
                Log.i(TAG, "✅ Device Owner status removed successfully!")
                Log.i(TAG, "🎉 DEVICE FULLY RELEASED - Customer now owns the device!")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to remove Device Owner status: ${e.message}")
                return false
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during device release: ${e.message}")
            false
        }
    }
}
