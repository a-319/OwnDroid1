package com.bintianqi.owndroid

import android.app.Application
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MyViewModel(application: Application): AndroidViewModel(application) {
    val theme = MutableStateFlow(ThemeSettings())
    private val app = application
    private val sp = SharedPrefs(application)
    private var monitorJob: Job? = null

    init {
        theme.value = ThemeSettings(sp.materialYou, sp.darkTheme, sp.blackTheme)
        viewModelScope.launch {
            theme.collect {
                sp.materialYou = it.materialYou
                sp.darkTheme = it.darkTheme
                sp.blackTheme = it.blackTheme
            }
        }
    }

    // פונקציית הניטור החדשה
    fun startScreenMonitoring() {
        // מפעילים רק אם מצב Dhizuku כבוי (עבור Device Owner רגיל)
        if (!sp.dhizuku) {
            monitorJob?.cancel()
            monitorJob = viewModelScope.launch(Dispatchers.Default) {
                val usageStatsManager = app.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                while (isActive) {
                    val time = System.currentTimeMillis()
                    val events = usageStatsManager.queryEvents(time - 1000, time)
                    val event = UsageEvents.Event()
                    
                    while (events.hasNextEvent()) {
                        events.getNextEvent(event)
                        if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                            // בדיקה אם ה-Activity שנפתח נמצא ברשימת החסומים
                            if (sp.blockedActivities.contains(event.className)) {
                                launchLockScreen()
                            }
                        }
                    }
                    delay(500) // בדיקה כל חצי שנייה לחיסכון בסוללה
                }
            }
        }
    }

    private fun launchLockScreen() {
        if (!sp.lockPasswordHash.isNullOrEmpty()) {
            val intent = Intent(app, ManageSpaceActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            app.startActivity(intent)
        }
    }
}

data class ThemeSettings(
    val materialYou: Boolean = false,
    val darkTheme: Int = -1,
    val blackTheme: Boolean = false
)
