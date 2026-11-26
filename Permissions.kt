package com.pushlock.app.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.pushlock.app.service.LockAccessibilityService

data class PermState(val accessibility: Boolean, val overlay: Boolean, val camera: Boolean) {
    val all get() = accessibility && overlay && camera
}

object Permissions {
    fun snapshot(ctx: Context) = PermState(
        accessibility = accessibilityEnabled(ctx),
        overlay = Settings.canDrawOverlays(ctx),
        camera = cameraGranted(ctx),
    )

    fun cameraGranted(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    fun accessibilityEnabled(ctx: Context): Boolean {
        val component = ComponentName(ctx, LockAccessibilityService::class.java)
        val full = component.flattenToString()
        val short = component.flattenToShortString()
        val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(':').any { it.equals(full, true) || it.equals(short, true) }
    }

    fun openAccessibilitySettings(ctx: Context) = start(ctx, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun openOverlaySettings(ctx: Context) = start(
        ctx, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")),
    )

    fun openAppDetails(ctx: Context) = start(
        ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")),
    )

    private fun start(ctx: Context, intent: Intent) {
        runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
