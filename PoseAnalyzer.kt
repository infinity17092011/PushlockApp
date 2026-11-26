package com.pushlock.app.pose

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

/** Feeds camera frames to ML Kit's on-device pose detector. No network, no video leaves the phone. */
class PoseAnalyzer(private val onFrame: (PoseFrame?) -> Unit) : ImageAnalysis.Analyzer {

    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build(),
    )

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val media = imageProxy.image
        if (media == null) {
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val width = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
        val height = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height

        try {
            detector.process(InputImage.fromMediaImage(media, rotation))
                .addOnSuccessListener { pose -> onFrame(pose.toFrame(width, height)) }
                .addOnFailureListener { onFrame(null) }
                .addOnCompleteListener { imageProxy.close() }
        } catch (e: Exception) {
            imageProxy.close()
        }
    }

    fun close() = runCatching { detector.close() }

    private fun Pose.toFrame(w: Int, h: Int): PoseFrame? {
        val all = allPoseLandmarks
        if (all.isEmpty()) return null
        return PoseFrame(
            w, h,
            all.associate { it.landmarkType to Joint(it.position.x, it.position.y, it.inFrameLikelihood) },
        )
    }
}
