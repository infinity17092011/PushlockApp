package com.pushlock.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val reps: Int,
    val completed: Boolean,
    val durationSec: Int,
)

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: Session)

    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    fun all(): Flow<List<Session>>
}

@Database(entities = [Session::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}

// ---- Stats derived from the history ----

data class DayTotal(val label: String, val reps: Int, val isToday: Boolean)

data class Stats(val todayReps: Int, val streak: Int, val weekTotal: Int, val week: List<DayTotal>)

fun computeStats(sessions: List<Session>): Stats {
    val zone = ZoneId.systemDefault()
    fun Session.day(): LocalDate = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

    val today = LocalDate.now()
    val repsByDay = sessions.groupBy { it.day() }.mapValues { (_, list) -> list.sumOf { it.reps } }
    val completedDays = sessions.filter { it.completed }.map { it.day() }.toSet()

    var cursor = if (today in completedDays) today else today.minusDays(1)
    var streak = 0
    while (cursor in completedDays) {
        streak++
        cursor = cursor.minusDays(1)
    }

    val week = (6 downTo 0).map { i ->
        val d = today.minusDays(i.toLong())
        DayTotal(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), repsByDay[d] ?: 0, i == 0)
    }
    return Stats(repsByDay[today] ?: 0, streak, week.sumOf { it.reps }, week)
}
