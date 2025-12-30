package com.emisure.app

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

/**
 * Device Admin Receiver for Emisure
 * 
 * This receiver handles device admin events and is the core component
 * that allows the app to manage device policies like locking and preventing factory reset.
 */
class EmisureDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "EmisureAdmin"

        /**
         * Returns the ComponentName for this receiver.
         * Used to identify this admin component to the system.
         */
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context.applicationContext, EmisureDeviceAdminReceiver::class.java)
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Device Admin Enabled")
        Toast.makeText(context, "Emisure Admin Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i(TAG, "Device Admin Disabled")
        Toast.makeText(context, "Emisure Admin Disabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.w(TAG, "Device Admin Disable Requested")
        return "Warning: Disabling will remove device management. Contact your seller."
    }

//    override fun onPasswordChanged(context: Context, intent: Intent) {
//        super.onPasswordChanged(context, intent)
//        Log.i(TAG, "Password Changed")
//    }
//
//    override fun onPasswordFailed(context: Context, intent: Intent) {
//        super.onPasswordFailed(context, intent)
//        Log.w(TAG, "Password Failed Attempt")
//    }
//
//    override fun onPasswordSucceeded(context: Context, intent: Intent) {
//        super.onPasswordSucceeded(context, intent)
//        Log.i(TAG, "Password Succeeded")
//    }
}
