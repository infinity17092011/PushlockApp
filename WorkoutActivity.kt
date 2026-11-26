package com.pushlock.app.ui

import android.Manifest
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pushlock.app.pose.Joint
import com.pushlock.app.pose.PoseAnalyzer
import com.pushlock.app.pose.PoseFrame
import com.pushlock.app.ui.components.BtnStyle
import com.pushlock.app.ui.components.PLButton
import com.pushlock.app.ui.components.Pill
import com.pushlock.app.ui.components.Ring
import com.pushlock.app.ui.components.RoundIconButton
import com.pushlock.app.ui.theme.Accent
import com.pushlock.app.ui.theme.PushLockTheme
import com.pushlock.app.util.AppCatalog
import com.pushlock.app.util.Permissions
import com.pushlock.app.util.goHome
import com.pushlock.app.util.launchApp
import java.util.concurrent.Executors
import kotlin.math.max

class WorkoutActivity : ComponentActivity() {
    private val vm: WorkoutViewModel by viewModels()
    private var targetPkg: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        targetPkg = intent.getStringExtra(LockActivity.EXTRA_PKG)
        val targetLabel = targetPkg?.let { AppCatalog.labelAndIcon(this, it).first }

        setContent {
            PushLockTheme {
                WorkoutScreen(
                    vm = vm,
                    targetLabel = targetLabel,
                    onClose = ::close,
                    onDone = ::done,
                )
            }
        }
    }

    /** Leaving early: if we came from a locked app, send the user home rather than back into it. */
    private fun close() {
        vm.leave()
        if (targetPkg != null && !vm.finished) goHome()
        finish()
    }

    private fun done() {
        targetPkg?.let { launchApp(it) }
        finish()
    }

    override fun onDestroy() {
        if (isFinishing) vm.leave()
        super.onDestroy()
    }
}

@Composable
private fun WorkoutScreen(vm: WorkoutViewModel, targetLabel: String?, onClose: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    var cameraGranted by remember { mutableStateOf(Permissions.cameraGranted(ctx)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { cameraGranted = it }
    val analyzer = remember { PoseAnalyzer { vm.onFrame(it) } }
    val haptic = LocalHapticFeedback.current
    val state = vm.state

    DisposableEffect(Unit) { onDispose { analyzer.close() } }
    LaunchedEffect(Unit) { if (!cameraGranted) launcher.launch(Manifest.permission.CAMERA) }
    LaunchedEffect(state.reps) { if (state.reps > 0) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    BackHandler { onClose() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraGranted) {
            CameraPreview(vm.lens, analyzer, Modifier.fillMaxSize())
            SkeletonOverlay({ vm.frame }, vm.lens == CameraSelector.LENS_FACING_FRONT, Modifier.fillMaxSize())
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Rounded.Videocam, null, tint = Accent, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Camera needed", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "PushLock counts reps with your camera. Nothing is recorded or uploaded.",
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(24.dp))
                PLButton("Allow camera", onClick = { launcher.launch(Manifest.permission.CAMERA) })
            }
        }

        // Scrims so overlays stay readable on any background
        Box(
            Modifier.fillMaxWidth().height(220.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))),
        )
        Box(
            Modifier.fillMaxWidth().height(200.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))),
        )

        // Top bar
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(Icons.Rounded.Close, onClick = onClose)
            Spacer(Modifier.weight(1f))
            Pill("SIDE VIEW")
            Spacer(Modifier.weight(1f))
            RoundIconButton(Icons.Rounded.FlipCameraAndroid, onClick = { vm.flipCamera() })
        }

        // Rep counter
        Ring(
            progress = state.reps / vm.target.toFloat(),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 84.dp).size(150.dp),
            stroke = 10.dp,
            track = Color.White.copy(alpha = 0.18f),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${state.reps}", style = MaterialTheme.typography.displayLarge, color = Color.White)
                Text("of ${vm.target}", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Coaching hint
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val shape = RoundedCornerShape(50)
            Box(
                Modifier
                    .clip(shape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, if (state.formOk) Accent else Color.White.copy(alpha = 0.25f), shape)
                    .padding(horizontal = 22.dp, vertical = 14.dp),
            ) {
                Text(state.hint, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Prop your phone on the floor, side-on, 1 to 2 m away, whole body in frame.",
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Success
        AnimatedVisibility(vm.finished, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.88f)), contentAlignment = Alignment.Center) {
                Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Ring(progress = 1f, modifier = Modifier.size(140.dp), stroke = 10.dp) {
                        Icon(Icons.Rounded.Check, null, tint = Accent, modifier = Modifier.size(64.dp))
                    }
                    Spacer(Modifier.height(28.dp))
                    Text("Unlocked", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${vm.target} push-ups done. ${vm.unlockMinutes} minutes earned.",
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(32.dp))
                    PLButton(if (targetLabel != null) "Open $targetLabel" else "Done", onClick = onDone)
                    if (targetLabel != null) {
                        Spacer(Modifier.height(10.dp))
                        PLButton("Stay in PushLock", onClick = { onClose() }, style = BtnStyle.Ghost)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(lens: Int, analyzer: ImageAnalysis.Analyzer, modifier: Modifier) {
    val context: Context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    DisposableEffect(lens, analyzer) {
        val executor = Executors.newSingleThreadExecutor()
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = androidx.camera.core.Preview.Builder().build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executor, analyzer) }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.Builder().requireLensFacing(lens).build(),
                    preview, analysis,
                )
            } catch (e: Exception) {
                // Device may lack the requested lens; fall back to whatever back camera exists.
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { future.get().unbindAll() }
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private val bones = listOf(
    11 to 13, 13 to 15, 12 to 14, 14 to 16, // arms
    11 to 12, 23 to 24, 11 to 23, 12 to 24, // torso
    23 to 25, 25 to 27, 24 to 26, 26 to 28, // legs
)

/** Draws the detected skeleton on top of the camera preview (frame is read at draw time to avoid recomposition). */
@Composable
private fun SkeletonOverlay(frame: () -> PoseFrame?, mirrored: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val f = frame() ?: return@Canvas
        val scale = max(size.width / f.width, size.height / f.height)
        val dx = (size.width - f.width * scale) / 2f
        val dy = (size.height - f.height * scale) / 2f
        fun map(j: Joint): Offset {
            val x = j.x * scale + dx
            return Offset(if (mirrored) size.width - x else x, j.y * scale + dy)
        }

        for ((a, b) in bones) {
            val ja = f.joints[a] ?: continue
            val jb = f.joints[b] ?: continue
            if (ja.conf < 0.5f || jb.conf < 0.5f) continue
            drawLine(Accent.copy(alpha = 0.25f), map(ja), map(jb), strokeWidth = 14.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Accent, map(ja), map(jb), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        }
        f.joints.values.filter { it.conf >= 0.5f }.forEach {
            drawCircle(Color.White, radius = 5.dp.toPx(), center = map(it))
        }
    }
}
