package com.gezimos.katapult.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent

/** Enables the Home screen double-tap sleep gesture after the user enables this service. */
class TapToSleepAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        activeService = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (activeService === this) activeService = null
        return super.onUnbind(intent)
    }

    companion object {
        @Volatile private var activeService: TapToSleepAccessibilityService? = null

        fun lockScreen(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                (activeService?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) == true)
    }
}
