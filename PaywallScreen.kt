package com.pushlock.app.ui.screens

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
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pushlock.app.ui.AppViewModel
import com.pushlock.app.ui.components.BtnStyle
import com.pushlock.app.ui.components.IconBadge
import com.pushlock.app.ui.components.LogoMark
import com.pushlock.app.ui.components.PLButton
import com.pushlock.app.ui.theme.Accent
import com.pushlock.app.ui.theme.Danger
import com.pushlock.app.ui.theme.TextMuted
import com.pushlock.app.ui.theme.appBackground

@Composable
fun PaywallScreen(vm: AppViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .appBackground()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        LogoMark(84.dp)
        Spacer(Modifier.height(24.dp))
        Text("Unlock PushLock", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Subscribe to switch on the lock and start earning your scroll.",
            color = TextMuted,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
            Feature(Icons.Rounded.Apps, "Lock any app or browser", "Instagram, TikTok, Chrome and more.")
            Feature(Icons.Rounded.Videocam, "Rep counting by camera", "Runs on your phone. No video leaves it.")
            Feature(Icons.Rounded.LocalFireDepartment, "Streaks and progress", "See your week and keep the chain going.")
        }

        Spacer(Modifier.height(36.dp))
        vm.message?.let {
            Text(
                it,
                color = if (vm.messageIsError) Danger else Accent,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            )
        }
        PLButton("Subscribe with PayPal", onClick = { vm.startCheckout() }, loading = vm.busy)
        Spacer(Modifier.height(10.dp))
        PLButton(
            "I've subscribed. Check again",
            onClick = { vm.refreshSubscription(sync = true, userInitiated = true) },
            style = BtnStyle.Secondary,
            enabled = !vm.busy,
        )
        if (vm.busy) {
            Text(
                "This can take up to a minute while the server wakes up.",
                color = TextMuted,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Billed through PayPal. Cancel anytime in Settings.",
            color = TextMuted,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = { vm.signOut() }) { Text("Sign out", color = TextMuted) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
