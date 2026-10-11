package com.daytodo.app

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.daytodo.app.data.AppDatabase
import com.daytodo.app.data.Repository
import com.daytodo.app.prefs.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class App : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repo: Repository by lazy { Repository(database) }
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppCompatDelegate.setDefaultNightMode(Prefs(this).nightMode)
    }

    companion object {
        lateinit var instance: App
            private set
    }
}

fun Context.app(): App = applicationContext as App
