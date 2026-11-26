package com.pushlock.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pushlock.app.ui.AppViewModel
import com.pushlock.app.ui.Route
import com.pushlock.app.ui.components.BtnStyle
import com.pushlock.app.ui.components.IconBadge
import com.pushlock.app.ui.components.PLButton
import com.pushlock.app.ui.components.PLCard
import com.pushlock.app.ui.components.Pill
import com.pushlock.app.ui.components.ScreenHeader
import com.pushlock.app.ui.theme.Accent
import com.pushlock.app.ui.theme.TextMuted
import com.pushlock.app.ui.theme.appBackground
import com.pushlock.app.util.Permissions

@Composable
fun SetupScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val p = vm.perms
    var cameraAsked by remember { mutableStateOf(false) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.refreshPermissions()
    }
    val back: (() -> Unit)? = if (vm.onboarded) ({ vm.route = Route.Home }) else null

    Column(
        Modifier
            .fillMaxSize()
            .appBackground()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader("Set up", onBack = back)
        Text(
            "Three quick permissions and PushLock is ready. You can come back to this any time.",
            color = TextMuted,
            style = MaterialTheme.typography.bodyLarge,
        )

        PermissionCard(
            icon = Icons.Rounded.Accessibility,
            title = "Accessibility",
            body = "Lets PushLock notice which app is in front, so it can ask for push-ups before a locked app opens. It never reads your screen, messages or passwords.",
            hint = "If the switch is greyed out: Settings > Apps > PushLock > ⋮ menu > Allow restricted settings, then try again.",
            granted = p.accessibility,
            actionLabel = "Open settings",
            onAction = { Permissions.openAccessibilitySettings(ctx) },
        )
        PermissionCard(
            icon = Icons.Rounded.Videocam,
            title = "Camera",
            body = "Used to count your push-ups. Pose detection runs on your phone and no video is stored or sent anywhere.",
            hint = null,
            granted = p.camera,
            actionLabel = "Allow camera",
            onAction = {
                if (cameraAsked) Permissions.openAppDetails(ctx)
                else {
                    cameraAsked = true
                    cameraLauncher.launch(Manifest.permission.CAMERA)
                }
            },
        )
        PermissionCard(
            icon = Icons.Rounded.Layers,
            title = "Display over other apps",
            body = "Lets the lock screen appear on top of a locked app the moment you open it.",
            hint = null,
            granted = p.overlay,
            actionLabel = "Open settings",
            onAction = { Permissions.openOverlaySettings(ctx) },
        )

        Spacer(Modifier.height(4.dp))
        PLButton("Continue", onClick = { vm.completeSetup() }, enabled = p.all)
        if (!p.all) {
            PLButton("Skip for now", onClick = { vm.completeSetup() }, style = BtnStyle.Ghost)
            Text(
                "Blocking stays paused until all three are on.",
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    body: String,
    hint: String?,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    PLCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(if (granted) Icons.Rounded.Check else icon)
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (granted) Pill("ON", color = Accent)
        }
        Spacer(Modifier.height(12.dp))
        Text(body, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        if (hint != null && !granted) {
            Spacer(Modifier.height(8.dp))
            Text(hint, color = TextMuted.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
        }
        if (!granted) {
            Spacer(Modifier.height(16.dp))
            PLButton(actionLabel, onClick = onAction, style = BtnStyle.Secondary)
        }
    }
}
