package com.pushlock.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.pushlock.app.data.AppDatabase
import com.pushlock.app.data.Prefs
import com.pushlock.app.net.AuthRepository
import com.pushlock.app.net.SubscriptionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Tiny hand-rolled dependency container (no DI framework needed). */
class AppContainer(app: Application) {
    val prefs = Prefs(app)
    val db: AppDatabase = Room.databaseBuilder(app, AppDatabase::class.java, "pushlock.db").build()
    val auth = AuthRepository(prefs)
    val subs = SubscriptionRepository(prefs, auth)
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

class PushLockApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.container: AppContainer
    get() = (applicationContext as PushLockApp).container
