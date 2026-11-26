package com.pushlock.app.ui

import android.app.Application
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.pushlock.app.container
import com.pushlock.app.data.Session
import com.pushlock.app.pose.PoseFrame
import com.pushlock.app.pose.RepCounter
import com.pushlock.app.pose.RepState
import kotlinx.coroutines.launch

class WorkoutViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container
    private val counter = RepCounter()
    private val startedAt = System.currentTimeMillis()
    private var saved = false

    val target: Int = c.prefs.requiredReps()
    val unlockMinutes: Int = c.prefs.unlockMinutes

    var state by mutableStateOf(RepState()); private set
    var frame by mutableStateOf<PoseFrame?>(null); private set
    var finished by mutableStateOf(false); private set
    var lens by mutableIntStateOf(CameraSelector.LENS_FACING_FRONT); private set

    fun onFrame(f: PoseFrame?) {
        if (finished) return
        frame = f
        state = counter.update(f, SystemClock.elapsedRealtime())
        if (state.reps >= target) complete()
    }

    fun flipCamera() {
        lens = if (lens == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK
        else CameraSelector.LENS_FACING_FRONT
    }

    private fun complete() {
        if (finished) return
        finished = true
        c.prefs.grantUnlock()
        save(completed = true)
    }

    /** Called when the user leaves early: keep whatever they managed. */
    fun leave() {
        if (!finished && state.reps > 0) save(completed = false)
    }

    private fun save(completed: Boolean) {
        if (saved) return
        saved = true
        val session = Session(
            timestamp = System.currentTimeMillis(),
            reps = state.reps,
            completed = completed,
            durationSec = ((System.currentTimeMillis() - startedAt) / 1000).toInt(),
        )
        c.appScope.launch { c.db.sessionDao().insert(session) }
    }
}
