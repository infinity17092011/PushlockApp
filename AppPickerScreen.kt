package com.pushlock.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pushlock.app.container
import com.pushlock.app.ui.AppViewModel
import com.pushlock.app.ui.Route
import com.pushlock.app.ui.components.AppIconImage
import com.pushlock.app.ui.components.BtnStyle
import com.pushlock.app.ui.components.PLButton
import com.pushlock.app.ui.components.ScreenHeader
import com.pushlock.app.ui.components.SectionLabel
import com.pushlock.app.ui.components.fieldColors
import com.pushlock.app.ui.components.switchColors
import com.pushlock.app.ui.theme.Accent
import com.pushlock.app.ui.theme.TextMuted
import com.pushlock.app.ui.theme.appBackground
import com.pushlock.app.util.AppCatalog
import com.pushlock.app.util.AppInfo

@Composable
fun AppPickerScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val prefs = ctx.container.prefs
    val apps by produceState<List<AppInfo>?>(null) { value = AppCatalog.load(ctx) }
    var query by remember { mutableStateOf("") }
    var locked by remember { mutableStateOf(prefs.lockedApps) }

    fun setLocked(next: Set<String>) {
        locked = next
        prefs.lockedApps = next
    }

    Column(
        Modifier
            .fillMaxSize()
            .appBackground()
            .systemBarsPadding(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            ScreenHeader("Choose apps", onBack = { vm.route = Route.Home })
            Text(
                "${locked.size} selected. These ask for push-ups before they open.",
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search apps") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val list = apps
        if (list == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
        } else {
            val filtered = list.filter { it.label.contains(query.trim(), ignoreCase = true) }
            val suggested = filtered.filter { it.suggested }
            val others = filtered.filter { !it.suggested }

            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (suggested.isNotEmpty()) {
                    item {
                        SectionLabel("SOCIAL APPS AND BROWSERS")
                        Spacer(Modifier.height(8.dp))
                        val allOn = suggested.all { it.pkg in locked }
                        PLButton(
                            text = if (allOn) "Unlock all suggested" else "Lock all suggested (${suggested.size})",
                            style = if (allOn) BtnStyle.Secondary else BtnStyle.Primary,
                            onClick = {
                                val pkgs = suggested.map { it.pkg }
                                setLocked(if (allOn) locked - pkgs.toSet() else locked + pkgs)
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    items(suggested, key = { it.pkg }) { app ->
                        AppRow(app, app.pkg in locked) { on ->
                            setLocked(if (on) locked + app.pkg else locked - app.pkg)
                        }
                    }
                }
                if (others.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        SectionLabel("ALL APPS")
                        Spacer(Modifier.height(8.dp))
                    }
                    items(others, key = { it.pkg }) { app ->
                        AppRow(app, app.pkg in locked) { on ->
                            setLocked(if (on) locked + app.pkg else locked - app.pkg)
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            "No apps match \"$query\".",
                            color = TextMuted,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppInfo, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconImage(app.icon, 44.dp)
        Spacer(Modifier.width(14.dp))
        Text(app.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1)
        Switch(checked = checked, onCheckedChange = onChange, colors = switchColors())
    }
}
