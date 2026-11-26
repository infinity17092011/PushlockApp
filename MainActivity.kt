package com.pushlock.app.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.pushlock.app.ui.screens.AppPickerScreen
import com.pushlock.app.ui.screens.AuthScreen
import com.pushlock.app.ui.screens.HomeScreen
import com.pushlock.app.ui.screens.PaywallScreen
import com.pushlock.app.ui.screens.SettingsScreen
import com.pushlock.app.ui.screens.SetupScreen
import com.pushlock.app.ui.theme.PushLockTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        handleLink(intent)
        setContent { PushLockTheme { AppRoot(vm) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLink(intent)
    }

    private fun handleLink(intent: Intent?) {
        if (intent?.data?.scheme == "pushlock") vm.onDeepLinkSubscribed()
    }
}

@Composable
private fun AppRoot(vm: AppViewModel) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.onResume() }

    // Gates come first: sign in -> subscribe -> first-time setup -> the app itself.
    val screen = when {
        !vm.signedIn -> Route.Auth
        !vm.subValid -> Route.Paywall
        !vm.onboarded -> Route.Setup
        else -> vm.route
    }

    BackHandler(enabled = screen == Route.Picker || screen == Route.Settings || (screen == Route.Setup && vm.onboarded)) {
        vm.route = Route.Home
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
        label = "route",
    ) { target ->
        when (target) {
            Route.Auth -> AuthScreen(vm)
            Route.Paywall -> PaywallScreen(vm)
            Route.Setup -> SetupScreen(vm)
            Route.Home -> HomeScreen(vm)
            Route.Picker -> AppPickerScreen(vm)
            Route.Settings -> SettingsScreen(vm)
        }
    }
}
