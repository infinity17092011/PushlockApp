package com.pushlock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pushlock.app.Config
import com.pushlock.app.container
import com.pushlock.app.ui.AppViewModel
import com.pushlock.app.ui.Route
import com.pushlock.app.ui.components.BtnStyle
import com.pushlock.app.ui.components.PLButton
import com.pushlock.app.ui.components.PLCard
import com.pushlock.app.ui.components.Pill
import com.pushlock.app.ui.components.ScreenHeader
import com.pushlock.app.ui.components.SectionLabel
import com.pushlock.app.ui.components.switchColors
import com.pushlock.app.ui.theme.Accent
import com.pushlock.app.ui.theme.Bg
import com.pushlock.app.ui.theme.Danger
import com.pushlock.app.ui.theme.Line
import com.pushlock.app.ui.theme.Surface1
import com.pushlock.app.ui.theme.Surface2
import com.pushlock.app.ui.theme.TextMuted
import com.pushlock.app.ui.theme.TextPrimary
import com.pushlock.app.ui.theme.appBackground

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val prefs = LocalContext.current.container.prefs
    var reps by remember { mutableIntStateOf(prefs.baseReps) }
    var minutes by remember { mutableIntStateOf(prefs.unlockMinutes) }
    var scale by remember { mutableStateOf(prefs.scaleReps) }
    var confirmCancel by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .appBackground()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("Settings", onBack = { vm.route = Route.Home })

        SectionLabel("PUSH-UPS")
        PLCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Reps to unlock", style = MaterialTheme.typography.titleMedium)
                    Text("Per unlock", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
                Stepper(reps, onMinus = { if (reps > 5) { reps--; prefs.baseReps = reps } },
                    onPlus = { if (reps < 50) { reps++; prefs.baseReps = reps } })
            }
        }
        PLCard(Modifier.fillMaxWidth()) {
            Text("Unlock duration", style = MaterialTheme.typography.titleMedium)
            Text("How long apps stay open after you earn it", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60, 120).forEach { m ->
                    DurationChip(
                        label = if (m < 60) "${m}m" else "${m / 60}h",
                        selected = minutes == m,
                        modifier = Modifier.weight(1f),
                    ) { minutes = m; prefs.unlockMinutes = m }
                }
            }
        }
        PLCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Scale difficulty", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "+2 reps for each unlock you've already earned today (up to 3x).",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = scale, onCheckedChange = { scale = it; prefs.scaleReps = it }, colors = switchColors())
            }
        }

        SectionLabel("STRICT MODE")
        PLCard(Modifier.fillMaxWidth()) {
            Text("No skip button", style = MaterialTheme.typography.titleMedium)
            Text(
                "The only way past the lock is push-ups, or one ${Config.EMERGENCY_UNLOCK_MINUTES}-minute emergency unlock per day.",
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        SectionLabel("PERMISSIONS")
        PLCard(Modifier.fillMaxWidth(), onClick = { vm.route = Route.Setup }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Accessibility, camera, overlay", style = MaterialTheme.typography.titleMedium)
                    Text("Tap to review", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
                if (vm.perms.all) Pill("ALL ON") else Pill("ACTION NEEDED", color = Danger)
            }
        }

        SectionLabel("SUBSCRIPTION")
        PLCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("PushLock", style = MaterialTheme.typography.titleMedium)
                    Text(vm.subscriptionLine(), color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
                Pill("ACTIVE")
            }
            vm.message?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = if (vm.messageIsError) Danger else Accent, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))
            PLButton("Cancel subscription", onClick = { confirmCancel = true }, style = BtnStyle.Danger, loading = vm.busy)
        }

        SectionLabel("ACCOUNT")
        PLCard(Modifier.fillMaxWidth()) {
            Text(vm.email ?: "Signed in", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            PLButton("Sign out", onClick = { vm.signOut() }, style = BtnStyle.Secondary)
        }

        Text(
            "PushLock 1.0.0",
            color = TextMuted,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            containerColor = Surface1,
            title = { Text("Cancel subscription?") },
            text = { Text("You keep access until the end of the period you've paid for.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = { confirmCancel = false; vm.cancelSubscription() }) {
                    Text("Cancel it", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = false }) { Text("Keep it", color = TextPrimary) }
            },
        )
    }
}

@Composable
private fun Stepper(value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton(Icons.Rounded.Remove, onMinus)
        Text(
            "$value",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp),
        )
        StepButton(Icons.Rounded.Add, onPlus)
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(Surface2)
            .border(1.dp, Line, CircleShape)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Icon(icon, null, tint = TextPrimary)
    }
}

@Composable
private fun DurationChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) Accent else Surface2)
            .border(1.dp, if (selected) Accent else Line, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) Bg else TextPrimary, style = MaterialTheme.typography.labelLarge)
    }
}
