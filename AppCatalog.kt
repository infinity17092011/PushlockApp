package com.pushlock.app.util

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(val label: String, val pkg: String, val icon: ImageBitmap, val suggested: Boolean)

object AppCatalog {
    /** Popular social apps and browsers we pre-suggest. */
    val suggestedPackages = setOf(
        // Social
        "com.instagram.android", "com.instagram.barcelona", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
        "com.facebook.katana", "com.facebook.lite", "com.twitter.android", "com.snapchat.android",
        "com.google.android.youtube", "com.reddit.frontpage", "com.pinterest", "com.linkedin.android",
        // Browsers
        "com.android.chrome", "org.mozilla.firefox", "com.brave.browser", "com.microsoft.emmx",
        "com.opera.browser", "com.sec.android.app.sbrowser", "com.duckduckgo.mobile.android", "com.vivaldi.browser",
    )

    @Suppress("DEPRECATION")
    suspend fun load(ctx: Context): List<AppInfo> = withContext(Dispatchers.Default) {
        val pm = ctx.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launcher, 0)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map {
                val pkg = it.activityInfo.packageName
                AppInfo(
                    label = it.loadLabel(pm).toString(),
                    pkg = pkg,
                    icon = it.loadIcon(pm).toBitmap(96, 96).asImageBitmap(),
                    suggested = pkg in suggestedPackages,
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    @Suppress("DEPRECATION")
    suspend fun iconsFor(ctx: Context, pkgs: Collection<String>): List<ImageBitmap> = withContext(Dispatchers.Default) {
        val pm = ctx.packageManager
        pkgs.mapNotNull { runCatching { pm.getApplicationIcon(it).toBitmap(96, 96).asImageBitmap() }.getOrNull() }
    }

    @Suppress("DEPRECATION")
    fun labelAndIcon(ctx: Context, pkg: String?): Pair<String, ImageBitmap?> {
        if (pkg == null) return "This app" to null
        val pm = ctx.packageManager
        return runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() to
                pm.getApplicationIcon(pkg).toBitmap(160, 160).asImageBitmap()
        }.getOrDefault("This app" to null)
    }
}

fun Context.goHome() {
    startActivity(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

fun Context.launchApp(pkg: String): Boolean {
    val intent = packageManager.getLaunchIntentForPackage(pkg) ?: return false
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    return true
}

fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
