package ntmt.schedule.notify

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object ChangeWatch {
    @Volatile var enabled: Boolean = true
    @Volatile private var lastEnqueue = 0L
    @Volatile private var netCb: ConnectivityManager.NetworkCallback? = null

    fun enqueue(ctx: Context, force: Boolean = false) {
        if (!enabled && !force) return
        val now = System.currentTimeMillis()
        if (!force && now - lastEnqueue < 90_000L) return
        lastEnqueue = now
        val req = OneTimeWorkRequestBuilder<ScheduleWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        WorkManager.getInstance(ctx.applicationContext).enqueueUniqueWork(
            "ntmt-watch-now",
            ExistingWorkPolicy.KEEP,
            req,
        )
    }

    fun registerNetwork(ctx: Context) {
        if (netCb != null) return
        val app = ctx.applicationContext
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                enqueue(app)
            }
        }
        try {
            app.getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(cb)
            netCb = cb
        } catch (_: Exception) {
        }
    }
}
