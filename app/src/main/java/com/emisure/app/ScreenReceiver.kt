package com.emisure.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Screen State Receiver for Emisure
 * 
 * Listens for screen on/off events to re-show the lock screen when the screen turns on.
 */
class ScreenReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "EmisureScreen"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                Log.i(TAG, "📱 Screen turned ON")
                showLockScreenIfNeeded(context)
            }
            Intent.ACTION_USER_PRESENT -> {
                Log.i(TAG, "👤 User unlocked device")
                showLockScreenIfNeeded(context)
            }
        }
    }
    
    private fun showLockScreenIfNeeded(context: Context) {
        val devicePolicyService = DevicePolicyService(context)
        
        if (devicePolicyService.isDeviceLocked()) {
            Log.i(TAG, "🔒 Device is locked, showing lock screen...")
            
            val lockInfo = devicePolicyService.getLockInfo()
            val lockIntent = LockScreenActivity.createIntent(
                context,
                lockInfo["title"] as? String ?: "Payment Required",
                lockInfo["sellerName"] as? String ?: "",
                lockInfo["sellerPhone"] as? String ?: "",
                lockInfo["amountDue"] as? String ?: "",
                lockInfo["dueDate"] as? String ?: "",
                lockInfo["message"] as? String ?: "Please pay your outstanding balance to unlock this device."
            )
            
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(lockIntent)
            
            Log.i(TAG, "✅ Lock screen shown on screen wake")
        }
    }
}
