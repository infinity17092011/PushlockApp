package com.pushlock.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.pushlock.app.container
import com.pushlock.app.ui.LockActivity

/**
 * Watches which app comes to the front. If it is one of the locked apps and the
 * user has not earned an unlock, the lock screen is shown on top of it.
 *
 * It only looks at the package name of the window. It cannot read screen content.
 */
class LockAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastPkg: String? = null
    private var lastLaunchAt = 0L
    private var ignored: Set<String> = emptySet()

    /** Re-checks every couple of seconds so an unlock that runs out re-locks the app you are still in. */
    private val ticker = object : Runnable {
        override fun run() {
            lastPkg?.let { evaluate(it) }
            handler.postDelayed(this, 2000)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        ignored = buildIgnoredPackages()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in ignored) return // keyboards and system UI do not count as "the app"
        lastPkg = pkg
        evaluate(pkg)
    }

    private fun evaluate(pkg: String) {
        if (pkg == packageName) return
        if (!container.prefs.shouldBlock(pkg)) return
        val now = System.currentTimeMillis()
        if (now - lastLaunchAt < 1500) return
        lastLaunchAt = now

        startActivity(
            Intent(this, LockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(LockActivity.EXTRA_PKG, pkg),
        )
    }

    private fun buildIgnoredPackages(): Set<String> {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val keyboards = imm.enabledInputMethodList.map { it.packageName }
        return (keyboards + "com.android.systemui").toSet()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }
}
