package ntmt.schedule.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

object ChangeWatch {
    const val GAP_MS = 30L * 60L * 1000L

    fun onWidgetUpdated(ctx: Context) {
        val app = ctx.applicationContext
        val req = OneTimeWorkRequestBuilder<ScheduleWorker>()
            .setInputData(workDataOf("reason" to "widget"))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        WorkManager.getInstance(app).enqueueUniqueWork(
            "ntmt-widget-tick",
            ExistingWorkPolicy.KEEP,
            req,
        )
    }
}
