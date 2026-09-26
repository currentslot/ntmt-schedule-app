package ntmt.schedule

import android.app.Application
import android.content.ComponentCallbacks
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ntmt.schedule.data.Prefs
import ntmt.schedule.notify.ChangeWatch
import ntmt.schedule.notify.Notify
import ntmt.schedule.notify.ScheduleWorker
import ntmt.schedule.notify.WatchReceiver
import ntmt.schedule.widget.ScheduleWidget
import java.util.concurrent.TimeUnit

class NtmtApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        try {
            Notify.createChannels(this)
            scope.launch {
                Prefs(this@NtmtApp).state.collect { ChangeWatch.enabled = it.liveWatch }
            }
            val req = PeriodicWorkRequestBuilder<ScheduleWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "ntmt-watch",
                ExistingPeriodicWorkPolicy.UPDATE,
                req,
            )
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(WatchReceiver(), filter, RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(WatchReceiver(), filter)
            }
            ChangeWatch.registerNetwork(this)
            registerComponentCallbacks(object : ComponentCallbacks {
                override fun onConfigurationChanged(newConfig: Configuration) {
                    ScheduleWidget.refresh(this@NtmtApp)
                }

                override fun onLowMemory() = Unit
            })
        } catch (_: Throwable) {
            // UI must still open if notifications fail to register
        }
    }
}
