package com.emisure.app

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView

/**
 * Persistent Lock Screen Activity - Production Ready
 * 
 * Beautiful, professional lock screen displayed when payment is overdue.
 * Cannot be dismissed by the user and persists across reboots.
 * Only emergency calls are allowed.
 */
class LockScreenActivity : Activity() {

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_SELLER_NAME = "seller_name"
        const val EXTRA_SELLER_PHONE = "seller_phone"
        const val EXTRA_AMOUNT_DUE = "amount_due"
        const val EXTRA_DUE_DATE = "due_date"
        const val EXTRA_MESSAGE = "message"
        
        fun createIntent(
            context: Context,
            title: String = "Payment Required",
            sellerName: String = "",
            sellerPhone: String = "",
            amountDue: String = "",
            dueDate: String = "",
            message: String = "Please pay your outstanding balance to unlock this device."
        ): Intent {
            return Intent(context, LockScreenActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_SELLER_NAME, sellerName)
                putExtra(EXTRA_SELLER_PHONE, sellerPhone)
                putExtra(EXTRA_AMOUNT_DUE, amountDue)
                putExtra(EXTRA_DUE_DATE, dueDate)
                putExtra(EXTRA_MESSAGE, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
        }
    }

    private var title: String = "Payment Required"
    private var sellerName: String = ""
    private var sellerPhone: String = ""
    private var amountDue: String = ""
    private var dueDate: String = ""
    private var message: String = "Please pay your outstanding balance to unlock this device."
    
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val checkLockStateRunnable = object : Runnable {
        override fun run() {
            if (!shouldStayLocked()) {
                closeAndUnlock()
            } else {
                handler.postDelayed(this, 2000)
            }
        }
    }
    
    private val unlockReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.emisure.UNLOCK_DEVICE") {
                closeAndUnlock()
            }
        }
    }
    
    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    if (shouldStayLocked()) {
                        try {
                            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                            activityManager.moveTaskToFront(taskId, 0)
                        } catch (e: Exception) { }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        title = intent.getStringExtra(EXTRA_TITLE) ?: "Payment Required"
        sellerName = intent.getStringExtra(EXTRA_SELLER_NAME) ?: ""
        sellerPhone = intent.getStringExtra(EXTRA_SELLER_PHONE) ?: ""
        amountDue = intent.getStringExtra(EXTRA_AMOUNT_DUE) ?: ""
        dueDate = intent.getStringExtra(EXTRA_DUE_DATE) ?: ""
        message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Please pay your outstanding balance to unlock this device."
        
        // Register receivers
        val unlockFilter = android.content.IntentFilter("com.emisure.UNLOCK_DEVICE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockReceiver, unlockFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(unlockReceiver, unlockFilter)
        }
        
        val screenFilter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, screenFilter)
        
        handler.postDelayed(checkLockStateRunnable, 2000)
        
        setupWindowFlags()
        setContentView(createLockScreenUI())
        startLockTaskModeIfPossible()
    }
    
    private fun closeAndUnlock() {
        handler.removeCallbacks(checkLockStateRunnable)
        try { stopLockTask() } catch (e: Exception) { }
        finishAndRemoveTask()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkLockStateRunnable)
        try { unregisterReceiver(unlockReceiver) } catch (e: Exception) { }
        try { unregisterReceiver(screenReceiver) } catch (e: Exception) { }
    }

    private fun setupWindowFlags() {
        window.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
            
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            }
            
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    private fun startLockTaskModeIfPossible() {
        val devicePolicyService = DevicePolicyService(this)
        if (devicePolicyService.isDeviceOwner()) {
            try {
                val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                val componentName = EmisureDeviceAdminReceiver.getComponentName(this)
                dpm.setLockTaskPackages(componentName, arrayOf(packageName))
                startLockTask()
            } catch (e: Exception) { }
        }
    }

    private fun createLockScreenUI(): View {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.parseColor("#0F172A"),
                    Color.parseColor("#1E293B")
                )
            )
            background = gradient
        }

        // Top spacer
        mainLayout.addView(Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        })

        // Lock Icon Circle
        val iconContainer = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            val iconBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#EF4444"))
            }
            background = iconBg
            val size = dpToPx(100)
            layoutParams = LinearLayout.LayoutParams(size, size)
        }
        
        val lockIcon = TextView(this).apply {
            text = "🔒"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 48f)
            gravity = Gravity.CENTER
        }
        iconContainer.addView(lockIcon)
        mainLayout.addView(iconContainer)
        
        // Spacing
        mainLayout.addView(createSpacer(32))

        // Title
        val titleView = TextView(this).apply {
            text = this@LockScreenActivity.title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
        }
        mainLayout.addView(titleView)
        
        // Spacing
        mainLayout.addView(createSpacer(12))

        // Subtitle Message
        val subtitle = TextView(this).apply {
            text = message
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(dpToPx(40), 0, dpToPx(40), 0)
        }
        mainLayout.addView(subtitle)
        
        // Spacing
        mainLayout.addView(createSpacer(32))

        // Payment Info Card
        if (amountDue.isNotEmpty() || sellerName.isNotEmpty()) {
            val infoCard = createPaymentCard()
            mainLayout.addView(infoCard)
            mainLayout.addView(createSpacer(24))
        }

        // Note: Buttons removed as they don't work in Lock Task Mode
        // Emergency calls can be made via hardware power button

        // Bottom spacer
        mainLayout.addView(Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.5f
            )
        })

        // Footer
        val footer = TextView(this).apply {
            text = "Emisure Protection"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#475569"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dpToPx(24))
        }
        mainLayout.addView(footer)

        return mainLayout
    }

    private fun createPaymentCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(20))
            
            val cardBg = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = dpToPx(16).toFloat()
                setStroke(1, Color.parseColor("#334155"))
            }
            background = cardBg
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(dpToPx(24), 0, dpToPx(24), 0)
            layoutParams = params
        }

        // Amount Due (Large, prominent)
        if (amountDue.isNotEmpty()) {
            val amountLabel = TextView(this).apply {
                text = "Amount Due"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(Color.parseColor("#64748B"))
                gravity = Gravity.CENTER
            }
            card.addView(amountLabel)
            
            val amountValue = TextView(this).apply {
                text = amountDue
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
                setTextColor(Color.parseColor("#EF4444"))
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(4), 0, dpToPx(16))
            }
            card.addView(amountValue)
        }

        // Divider
        if (amountDue.isNotEmpty() && (sellerName.isNotEmpty() || dueDate.isNotEmpty())) {
            val divider = View(this).apply {
                setBackgroundColor(Color.parseColor("#334155"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).apply { setMargins(0, 0, 0, dpToPx(16)) }
            }
            card.addView(divider)
        }

        // Seller & Due Date Info
        if (sellerName.isNotEmpty()) {
            card.addView(createInfoRow("Seller", sellerName))
        }
        if (dueDate.isNotEmpty()) {
            card.addView(createInfoRow("Due Date", dueDate))
        }
        if (sellerPhone.isNotEmpty()) {
            card.addView(createInfoRow("Contact", sellerPhone))
        }

        return card
    }

    private fun createInfoRow(label: String, value: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(6), 0, dpToPx(6))

            val labelView = TextView(this@LockScreenActivity).apply {
                text = label
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(Color.parseColor("#64748B"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(labelView)

            val valueView = TextView(this@LockScreenActivity).apply {
                text = value
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
            }
            addView(valueView)
        }
    }

    private fun createSpacer(heightDp: Int): Space {
        return Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(heightDp)
            )
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    // Block back button
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Blocked
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME, 
            KeyEvent.KEYCODE_APP_SWITCH, KeyEvent.KEYCODE_MENU -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onPause() {
        super.onPause()
        if (shouldStayLocked()) {
            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.moveTaskToFront(taskId, 0)
            } catch (e: SecurityException) { }
        }
    }

    override fun onStop() {
        super.onStop()
        if (shouldStayLocked()) {
            val intent = LockScreenActivity.createIntent(
                this, title, sellerName, sellerPhone, amountDue, dueDate, message
            )
            startActivity(intent)
        }
    }

    private fun shouldStayLocked(): Boolean {
        val deviceProtectedContext = createDeviceProtectedStorageContext()
        val prefs = deviceProtectedContext.getSharedPreferences("emisure_prefs", MODE_PRIVATE)
        return prefs.getBoolean("is_locked", false)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && shouldStayLocked()) {
            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.moveTaskToFront(taskId, 0)
            } catch (e: Exception) { }
        }
    }
}
