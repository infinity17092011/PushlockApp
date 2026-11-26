package com.pushlock.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pushlock.app.container
import com.pushlock.app.data.computeStats
import com.pushlock.app.ui.AppViewModel
import com.pushlock.app.ui.Route
import com.pushlock.app.ui.WorkoutActivity
import com.pushlock.app.ui.components.AppIconImage
import com.pushlock.app.ui.components.BtnStyle
import com.pushlock.app.ui.components.LogoMark
import com.pushlock.app.ui.components.PLButton
import com.pushlock.app.ui.components.PLCard
import com.pushlock.app.ui.components.Pill
import com.pushlock.app.ui.components.RoundIconButton
import com.pushlock.app.ui.components.Ring
import com.pushlock.app.ui.components.SectionLabel
import com.pushlock.app.ui.theme.Accent
import com.pushlock.app.ui.theme.Line
import com.pushlock.app.ui.theme.TextMuted
import com.pushlock.app.ui.theme.TextPrimary
import com.pushlock.app.ui.theme.Warn
import com.pushlock.app.ui.theme.appBackground
import com.pushlock.app.util.AppCatalog
import com.pushlock.app.util.formatClock
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val prefs = ctx.container.prefs
    val sessions by ctx.container.db.sessionDao().all().collectAsStateWithLifecycle(initialValue = emptyList())
    val stats = remember(sessions) { computeStats(sessions) }

    // Ticks every second so the countdown and lock status stay live.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val remaining = (prefs.unlockUntil - now).coerceAtLeast(0L)
    val unlocked = remaining > 0
    val locked = prefs.lockedApps
    val required = prefs.requiredReps()
    val icons by produceState(emptyList<ImageBitmap>(), locked) {
        value = AppCatalog.iconsFor(ctx, locked.take(6))
    }

    Column(
        Modifier
            .fillMaxSize()
            .appBackground()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            LogoMark(40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("PushLock", style = MaterialTheme.typography.titleLarge)
                Text("Earn your scroll", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            }
            RoundIconButton(Icons.Rounded.Settings, onClick = { vm.route = Route.Settings })
        }

        // Hero
        PLCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(if (unlocked) "UNLOCKED" else "LOCKED", color = if (unlocked) Accent else Warn)
                Spacer(Modifier.weight(1f))
                Text(
                    "${locked.size} ${if (locked.size == 1) "app" else "apps"} guarded",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(20.dp))
            Ring(
                progress = if (unlocked) remaining / (prefs.unlockMinutes * 60_000f) else 0f,
                modifier = Modifier.size(220.dp).align(Alignment.CenterHorizontally),
                stroke = 12.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (unlocked) {
                        Text(formatClock(remaining), style = MaterialTheme.typography.displayMedium)
                        Text("until relock", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Icon(Icons.Rounded.Lock, null, tint = Accent, modifier = Modifier.size(26.dp))
                        Text("$required", style = MaterialTheme.typography.displayLarge)
                        Text("push-ups to unlock", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            PLButton(
                text = if (unlocked) "Top up with push-ups" else "Start push-ups",
                icon = Icons.Rounded.FitnessCenter,
                onClick = { ctx.startActivity(Intent(ctx, WorkoutActivity::class.java)) },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Unlocks for ${prefs.unlockMinutes} minutes",
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        // Setup / empty warnings
        if (!vm.perms.all) {
            WarningCard(
                "Setup incomplete",
                "Blocking is paused until PushLock has all three permissions.",
                "Finish setup",
            ) { vm.route = Route.Setup }
        } else if (locked.isEmpty()) {
            WarningCard(
                "No apps locked yet",
                "Choose the social apps and browsers you want guarded.",
                "Choose apps",
            ) { vm.route = Route.Picker }
        }

        // Stats
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Modifier.weight(1f), "${stats.todayReps}", "Today", Icons.Rounded.FitnessCenter)
            StatTile(Modifier.weight(1f), "${stats.streak}", "Day streak", Icons.Rounded.LocalFireDepartment)
            StatTile(Modifier.weight(1f), "${stats.weekTotal}", "This week", Icons.Rounded.Apps)
        }

        // Week chart
        PLCard(Modifier.fillMaxWidth()) {
            Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            val max = stats.week.maxOf { it.reps }.coerceAtLeast(1)
            Row(
                Modifier.fillMaxWidth().height(110.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                stats.week.forEach { d ->
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp + 64.dp * (d.reps / max.toFloat()))
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        d.isToday -> Accent
                                        d.reps > 0 -> Accent.copy(alpha = 0.45f)
                                        else -> Line
                                    },
                                ),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            d.label,
                            color = if (d.isToday) TextPrimary else TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }

        // Guarded apps
        SectionLabel("GUARDED APPS")
        PLCard(Modifier.fillMaxWidth(), onClick = { vm.route = Route.Picker }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icons.isEmpty()) {
                    Text("None yet. Tap to choose.", color = TextMuted, modifier = Modifier.weight(1f))
                } else {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        icons.forEach { AppIconImage(it, 40.dp) }
                    }
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = TextMuted)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StatTile(modifier: Modifier, value: String, label: String, icon: ImageVector) {
    PLCard(modifier) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun WarningCard(title: String, body: String, action: String, onClick: () -> Unit) {
    PLCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, null, tint = Warn, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(6.dp))
        Text(body, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(14.dp))
        PLButton(action, onClick = onClick, style = BtnStyle.Secondary)
    }
}
