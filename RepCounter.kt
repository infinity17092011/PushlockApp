package com.pushlock.app.pose

import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min

data class Joint(val x: Float, val y: Float, val conf: Float)
data class PoseFrame(val width: Int, val height: Int, val joints: Map<Int, Joint>)

enum class Phase { SEARCHING, UP, DOWN }

data class RepState(
    val reps: Int = 0,
    val phase: Phase = Phase.SEARCHING,
    val hint: String = "Step into view",
    val formOk: Boolean = false,
)

/**
 * Plain-Kotlin push-up counter. ML Kit gives us body landmarks; this turns them
 * into reps with an elbow-angle state machine:
 *   arms extended (>155°)  ->  elbows bent (<100°)  ->  extended again  =  1 rep
 * It also requires a roughly horizontal body and a straight back, so waving your
 * arms or bobbing while sitting does not count.
 */
class RepCounter {
    private var reps = 0
    private var phase = Phase.SEARCHING
    private var smoothed: Float? = null
    private var lastSide = -1
    private var downAt = 0L
    private var badForm = false

    private class Side(
        val id: Int,
        val shoulder: Joint,
        val elbow: Joint,
        val wrist: Joint,
        val hip: Joint,
        val knee: Joint?,
        val ankle: Joint?,
        val score: Float,
    )

    fun update(frame: PoseFrame?, now: Long): RepState {
        if (frame == null) return lose("Step into view so your whole body is visible")
        val side = pickSide(frame) ?: return lose("Turn side-on so one arm is fully visible")
        if (side.id != lastSide) {
            smoothed = null
            lastSide = side.id
        }

        val raw = angle(side.shoulder, side.elbow, side.wrist)
        val elbow = smoothed?.let { it * 0.6f + raw * 0.4f } ?: raw
        smoothed = elbow

        val foot = side.ankle ?: side.knee ?: side.hip
        if (horizontalDeviation(side.shoulder, foot) > MAX_TILT) {
            return lose("Get into push-up position, side-on")
        }

        val hipAngle = side.ankle?.let { angle(side.shoulder, side.hip, it) }
        val formOk = hipAngle == null || hipAngle >= MIN_HIP_ANGLE

        var hint: String
        when (phase) {
            Phase.SEARCHING -> {
                hint = "Lock your arms out to begin"
                if (elbow > UP_ANGLE) {
                    phase = Phase.UP
                    hint = "Lower your chest"
                }
            }
            Phase.UP -> {
                hint = if (formOk) "Lower your chest" else "Keep your back straight"
                if (elbow < DOWN_ANGLE) {
                    phase = Phase.DOWN
                    downAt = now
                    badForm = false
                    hint = "Now push up!"
                }
            }
            Phase.DOWN -> {
                hint = "Push up!"
                if (!formOk) badForm = true
                if (elbow > UP_ANGLE) {
                    phase = Phase.UP
                    hint = when {
                        badForm -> "Rep skipped. Keep your back straight"
                        now - downAt < MIN_REP_MS -> "Too fast. Control the movement"
                        else -> {
                            reps++
                            "Nice! Rep $reps"
                        }
                    }
                    badForm = false
                }
            }
        }
        return RepState(reps, phase, hint, formOk)
    }

    private fun lose(hint: String): RepState {
        phase = Phase.SEARCHING
        smoothed = null
        badForm = false
        return RepState(reps, Phase.SEARCHING, hint, false)
    }

    private fun pickSide(f: PoseFrame): Side? {
        fun build(id: Int, s: Int, e: Int, w: Int, h: Int, k: Int, a: Int): Side? {
            val sj = f.joints[s]
            val ej = f.joints[e]
            val wj = f.joints[w]
            val hj = f.joints[h]
            if (sj == null || ej == null || wj == null || hj == null) return null
            if (minOf(sj.conf, ej.conf, wj.conf, hj.conf) < MIN_CONF) return null
            val knee = f.joints[k]?.takeIf { it.conf >= 0.3f }
            val ankle = f.joints[a]?.takeIf { it.conf >= 0.3f }
            return Side(id, sj, ej, wj, hj, knee, ankle, sj.conf + ej.conf + wj.conf + hj.conf)
        }

        val left = build(
            0, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST,
            PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE,
        )
        val right = build(
            1, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST,
            PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE,
        )
        return when {
            left != null && right != null -> if (left.score >= right.score) left else right
            else -> left ?: right
        }
    }

    /** Angle at point b (in degrees, 0..180) formed by a-b-c. */
    private fun angle(a: Joint, b: Joint, c: Joint): Float {
        val rad = atan2(c.y - b.y, c.x - b.x) - atan2(a.y - b.y, a.x - b.x)
        val deg = abs(Math.toDegrees(rad.toDouble())).toFloat()
        return if (deg > 180f) 360f - deg else deg
    }

    /** How far the line a->b is from perfectly horizontal, in degrees. */
    private fun horizontalDeviation(a: Joint, b: Joint): Float {
        val deg = abs(Math.toDegrees(atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble()))).toFloat()
        return min(deg, 180f - deg)
    }

    private companion object {
        const val UP_ANGLE = 155f
        const val DOWN_ANGLE = 100f
        const val MIN_HIP_ANGLE = 150f
        const val MAX_TILT = 40f
        const val MIN_CONF = 0.5f
        const val MIN_REP_MS = 300L
    }
}
