package com.pushlock.app.ui

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pushlock.app.Config
import com.pushlock.app.container
import com.pushlock.app.ui.components.AppIconImage
import com.pushlock.app.ui.components.BtnStyle
import com.pushlock.app.ui.components.PLButton
import com.pushlock.app.ui.theme.Accent
import com.pushlock.app.ui.theme.Bg
import com.pushlock.app.ui.theme.PushLockTheme
import com.pushlock.app.ui.theme.Surface1
import com.pushlock.app.ui.theme.TextMuted
import com.pushlock.app.ui.theme.TextPrimary
import com.pushlock.app.ui.theme.appBackground
import com.pushlock.app.util.AppCatalog
import com.pushlock.app.util.goHome
import com.pushlock.app.util.launchApp

/** Full-screen lock shown on top of a guarded app. Strict: no skip, only push-ups or the daily emergency unlock. */
class LockActivity : ComponentActivity() {
    private var pkg by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        pkg = intent.getStringExtra(EXTRA_PKG)
        onBackPressedDispatcher.addCallback(this) { goHome() }

        setContent {
            PushLockTheme {
                LockScreen(
                    pkg = pkg,
                    onStart = {
                        startActivity(Intent(this, WorkoutActivity::class.java).putExtra(EXTRA_PKG, pkg))
                        finish()
                    },
                    onEmergency = {
                        container.prefs.useEmergency()
                        pkg?.let { launchApp(it) }
                        finish()
                    },
                    onHome = { goHome() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pkg = intent.getStringExtra(EXTRA_PKG)
    }

    override fun onResume() {
        super.onResume()
        // Unlocked in the meantime (e.g. emergency unlock): nothing left to guard.
        if (container.prefs.isUnlocked()) finish()
    }

    companion object {
        const val EXTRA_PKG = "pkg"
    }
}

@Composable
private fun LockScreen(pkg: String?, onStart: () -> Unit, onEmergency: () -> Unit, onHome: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = ctx.container.prefs
    val (label, icon) = remember(pkg) { AppCatalog.labelAndIcon(ctx, pkg) }
    val required = prefs.requiredReps()
    val emergencyAvailable = prefs.emergencyAvailable()
    var confirm by remember { mutableStateOf(false) }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow by pulse.animateFloat(
        initialValue = 0.85f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "glow",
    )

    Column(
        Modifier.fillMaxSize().appBackground().systemBarsPadding().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(150.dp).scale(glow).alpha(0.22f).clip(CircleShape).background(Accent),
            )
            AppIconImage(icon, 96.dp)
            Box(
                Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 2.dp)
                    .size(40.dp).clip(CircleShape).background(Accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Lock, null, tint = Bg, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("$label is locked", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Do $required push-ups to open it for ${prefs.unlockMinutes} minutes.",
            color = TextMuted,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))

        PLButton("Start push-ups", onClick = onStart, icon = Icons.Rounded.FitnessCenter)
        Spacer(Modifier.height(10.dp))
        PLButton(
            text = if (emergencyAvailable) "Emergency unlock · ${Config.EMERGENCY_UNLOCK_MINUTES} min" else "Emergency unlock used today",
            onClick = { confirm = true },
            style = BtnStyle.Secondary,
            enabled = emergencyAvailable,
        )
        Spacer(Modifier.height(4.dp))
        PLButton("Go home", onClick = onHome, style = BtnStyle.Ghost)
        Spacer(Modifier.height(12.dp))
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = Surface1,
            title = { Text("Use today's emergency unlock?") },
            text = {
                Text(
                    "You get ${Config.EMERGENCY_UNLOCK_MINUTES} minutes, and it's only available once per day.",
                    color = TextMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = { confirm = false; onEmergency() }) { Text("Unlock", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Not now", color = TextPrimary) }
            },
        )
    }
}
