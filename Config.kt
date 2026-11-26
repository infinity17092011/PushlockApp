package com.pushlock.app

/** Everything you might want to change lives here. */
object Config {
    const val SUPABASE_URL = "https://hyahiftxwwaxswwaldzd.supabase.co"
    // Publishable (anon) key: safe to ship inside the app.
    const val SUPABASE_KEY = "sb_publishable_DCiPVFlVEqPDmCnkYGqILg_fXwrsCVt"

    // Your subscription server (Render).
    const val SERVER_URL = "https://pushlock.onrender.com"

    const val EMERGENCY_UNLOCK_MINUTES = 3
    /** How long the app trusts a cached "subscribed" answer when offline. */
    const val OFFLINE_GRACE_HOURS = 72L
}
