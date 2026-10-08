package ntmt.schedule

import android.app.Application
import android.content.ComponentCallbacks
import android.content.res.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.runBlocking
import ntmt.schedule.data.Prefs
import ntmt.schedule.notify.Notify
import ntmt.schedule.notify.ScheduleWorker
import ntmt.schedule.widget.ScheduleWidget
import java.util.concurrent.TimeUnit

class NtmtApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            Notify.createChannels(this)
            runBlocking { Prefs(this@NtmtApp).ensureUpdateDefaults() }
            ntmt.schedule.data.ScheduleCache.sweep(this)
            val req = PeriodicWorkRequestBuilder<ScheduleWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "ntmt-watch",
                ExistingPeriodicWorkPolicy.UPDATE,
                req,
            )
            registerComponentCallbacks(object : ComponentCallbacks {
                override fun onConfigurationChanged(newConfig: Configuration) {
                    ScheduleWidget.refresh(this@NtmtApp, newConfig)
                }

                override fun onLowMemory() = Unit
            })
        } catch (_: Throwable) {
            // UI must still open if notifications fail to register
        }
    }
}
