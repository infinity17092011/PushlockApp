package com.pushlock.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pushlock.app.container
import com.pushlock.app.net.SessionExpiredException
import com.pushlock.app.net.userMessage
import com.pushlock.app.util.Permissions
import com.pushlock.app.util.PermState
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

enum class Route { Auth, Paywall, Setup, Home, Picker, Settings }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container

    var signedIn by mutableStateOf(c.prefs.accessToken != null); private set
    var email by mutableStateOf(c.prefs.email); private set
    var subValid by mutableStateOf(c.prefs.isSubscriptionValid()); private set
    var onboarded by mutableStateOf(c.prefs.onboarded); private set
    var perms by mutableStateOf(Permissions.snapshot(app)); private set

    var route by mutableStateOf(Route.Home)
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    var messageIsError by mutableStateOf(true); private set

    private var awaitingCheckout = false
    private var lastRefresh = 0L

    fun refreshPermissions() {
        perms = Permissions.snapshot(getApplication())
    }

    /** Called every time the app comes to the front. */
    fun onResume() {
        refreshPermissions()
        if (!signedIn) return
        if (awaitingCheckout) {
            awaitingCheckout = false
            refreshSubscription(sync = true, userInitiated = true)
        } else if (System.currentTimeMillis() - lastRefresh > 60_000) {
            refreshSubscription()
        }
    }

    /** The pushlock://subscribed deep link opened us after PayPal checkout. */
    fun onDeepLinkSubscribed() {
        awaitingCheckout = true
    }

    fun clearMessage() {
        message = null
    }

    private fun say(text: String?, error: Boolean = true) {
        message = text
        messageIsError = error
    }

    // ---- Auth ----

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            busy = true; say(null)
            c.auth.signIn(email, password)
                .onSuccess { afterAuth() }
                .onFailure { say(it.userMessage()) }
            busy = false
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            busy = true; say(null)
            c.auth.signUp(email, password)
                .onSuccess { signedInNow ->
                    if (signedInNow) afterAuth()
                    else say("Account created. Check your inbox to confirm your email, then sign in.", error = false)
                }
                .onFailure { say(it.userMessage()) }
            busy = false
        }
    }

    private fun afterAuth() {
        signedIn = true
        email = c.prefs.email
        refreshSubscription()
    }

    fun signOut() {
        c.auth.signOut()
        signedIn = false
        subValid = false
        email = null
        route = Route.Home
        say(null)
    }

    // ---- Subscription ----

    fun refreshSubscription(sync: Boolean = false, userInitiated: Boolean = false) {
        viewModelScope.launch {
            if (userInitiated) { busy = true; say(null) }
            val result = if (sync) c.subs.sync() else c.subs.status()
            result.onFailure { e ->
                if (e is SessionExpiredException) {
                    signedIn = c.prefs.accessToken != null
                } else if (userInitiated) {
                    say(e.userMessage())
                }
            }
            subValid = c.prefs.isSubscriptionValid()
            lastRefresh = System.currentTimeMillis()
            if (userInitiated) {
                busy = false
                if (result.isSuccess && !subValid && message == null) {
                    say("No active subscription yet. If you just paid, wait a few seconds and try again.", error = false)
                }
            }
        }
    }

    fun startCheckout() {
        viewModelScope.launch {
            busy = true; say(null)
            c.subs.createCheckout()
                .onSuccess { url ->
                    if (url == null) {
                        subValid = c.prefs.isSubscriptionValid()
                    } else {
                        awaitingCheckout = true
                        getApplication<Application>().startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                }
                .onFailure { e ->
                    if (e is SessionExpiredException) signedIn = c.prefs.accessToken != null
                    say(e.userMessage())
                }
            busy = false
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch {
            busy = true; say(null)
            c.subs.cancel()
                .onSuccess { say("Cancelled. You keep access until the end of the billing period.", error = false) }
                .onFailure { say(it.userMessage()) }
            subValid = c.prefs.isSubscriptionValid()
            busy = false
        }
    }

    fun subscriptionLine(): String {
        val end = c.prefs.subPeriodEnd
        if (end <= 0L) return "Active"
        val date = Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        return "Active until $date"
    }

    // ---- Onboarding ----

    fun completeSetup() {
        c.prefs.onboarded = true
        onboarded = true
        route = Route.Home
    }
}
