package com.pushlock.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.pushlock.app.Config
import java.time.LocalDate

/**
 * All small pieces of state: login session, settings, locked apps, unlock timer
 * and the cached subscription answer. The accessibility service reads this too,
 * so it must stay quick and synchronous.
 */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("pushlock", Context.MODE_PRIVATE)

    // ---- Account session ----
    var accessToken: String?
        get() = sp.getString("access_token", null)
        set(v) = sp.edit { putString("access_token", v) }
    var refreshToken: String?
        get() = sp.getString("refresh_token", null)
        set(v) = sp.edit { putString("refresh_token", v) }
    var email: String?
        get() = sp.getString("email", null)
        set(v) = sp.edit { putString("email", v) }
    var tokenExpiresAt: Long
        get() = sp.getLong("token_exp", 0L)
        set(v) = sp.edit { putLong("token_exp", v) }

    // ---- Settings ----
    var baseReps: Int
        get() = sp.getInt("base_reps", 10)
        set(v) = sp.edit { putInt("base_reps", v) }
    var unlockMinutes: Int
        get() = sp.getInt("unlock_min", 30)
        set(v) = sp.edit { putInt("unlock_min", v) }
    var scaleReps: Boolean
        get() = sp.getBoolean("scale_reps", false)
        set(v) = sp.edit { putBoolean("scale_reps", v) }
    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit { putBoolean("onboarded", v) }
    var lockedApps: Set<String>
        get() = sp.getStringSet("locked_apps", null) ?: emptySet()
        set(v) = sp.edit { putStringSet("locked_apps", HashSet(v)) }

    // ---- Unlock state ----
    var unlockUntil: Long
        get() = sp.getLong("unlock_until", 0L)
        set(v) = sp.edit { putLong("unlock_until", v) }
    private var emergencyDay: Long
        get() = sp.getLong("emergency_day", -1L)
        set(v) = sp.edit { putLong("emergency_day", v) }
    private var unlocksDay: Long
        get() = sp.getLong("unlocks_day", -1L)
        set(v) = sp.edit { putLong("unlocks_day", v) }
    private var unlocksCount: Int
        get() = sp.getInt("unlocks_count", 0)
        set(v) = sp.edit { putInt("unlocks_count", v) }

    // ---- Cached subscription answer ----
    var subActive: Boolean
        get() = sp.getBoolean("sub_active", false)
        set(v) = sp.edit { putBoolean("sub_active", v) }
    var subVerifiedAt: Long
        get() = sp.getLong("sub_verified_at", 0L)
        set(v) = sp.edit { putLong("sub_verified_at", v) }
    var subPeriodEnd: Long
        get() = sp.getLong("sub_period_end", 0L)
        set(v) = sp.edit { putLong("sub_period_end", v) }

    // ---- Logic ----
    private fun today() = LocalDate.now().toEpochDay()

    fun isUnlocked(now: Long = System.currentTimeMillis()) = now < unlockUntil

    fun unlocksToday(): Int = if (unlocksDay == today()) unlocksCount else 0

    /** Reps needed for the next unlock (optionally +2 for every unlock already earned today). */
    fun requiredReps(): Int {
        val base = baseReps
        if (!scaleReps) return base
        return minOf(base + unlocksToday() * 2, base * 3)
    }

    fun grantUnlock() {
        val n = unlocksToday() + 1
        sp.edit {
            putLong("unlocks_day", today())
            putInt("unlocks_count", n)
        }
        unlockUntil = System.currentTimeMillis() + unlockMinutes * 60_000L
    }

    fun emergencyAvailable(): Boolean = emergencyDay != today()

    fun useEmergency() {
        emergencyDay = today()
        val until = System.currentTimeMillis() + Config.EMERGENCY_UNLOCK_MINUTES * 60_000L
        unlockUntil = maxOf(unlockUntil, until)
    }

    /** Trust the cached "subscribed" answer for a few days so bad signal never locks you out. */
    fun isSubscriptionValid(): Boolean {
        if (!subActive) return false
        return System.currentTimeMillis() - subVerifiedAt < Config.OFFLINE_GRACE_HOURS * 3_600_000L
    }

    fun shouldBlock(pkg: String): Boolean =
        isSubscriptionValid() && !isUnlocked() && pkg in lockedApps

    fun clearAccount() {
        sp.edit {
            remove("access_token"); remove("refresh_token"); remove("email"); remove("token_exp")
            putBoolean("sub_active", false)
            putLong("sub_verified_at", 0L)
            putLong("sub_period_end", 0L)
        }
    }
}
