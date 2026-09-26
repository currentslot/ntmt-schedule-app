package ntmt.schedule.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import ntmt.schedule.R
import ntmt.schedule.notify.Notify
import java.io.File

object UpdateFetch {
    data class State(
        val versionCode: Int = 0,
        val progress: Int = -1,
        val done: Boolean = false,
        val error: String? = null,
    )

    val state = MutableStateFlow(State())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun running(): Boolean = job?.isActive == true

    internal fun begin(service: Service, release: AppRelease) {
        job?.cancel()
        AppUpdate.abortDownload()
        job = scope.launch {
            state.value = State(release.versionCode, 0, false, null)
            try {
                val dest = File(service.cacheDir, "updates/ntmt-update.apk")
                AppUpdate.download(release, dest) { progress ->
                    state.value = State(release.versionCode, progress.coerceIn(0, 99), false, null)
                    notify(service, release.versionName, progress.coerceIn(0, 99))
                }
                state.value = State(release.versionCode, 100, true, null)
                Notify.showUpdate(service, "Пакет загружен", "${release.versionName} можно установить")
            } catch (e: CancellationException) {
                state.value = State()
            } catch (e: Exception) {
                val message = e.message ?: "Не удалось скачать"
                val cancelled = message.contains("отмен", true)
                state.value = if (cancelled) State() else State(release.versionCode, -1, false, message)
                if (!cancelled) Notify.showUpdate(service, "Не удалось скачать", message)
            } finally {
                service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
                service.stopSelf()
            }
        }
    }

    fun cancel() {
        AppUpdate.abortDownload()
        job?.cancel()
        state.value = State()
    }

    private fun notify(ctx: Context, name: String, progress: Int) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.notify(UpdateDownloadService.NOTIF, UpdateDownloadService.note(ctx, name, progress))
    }
}

class UpdateDownloadService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel(this)
        if (intent?.action == ACTION_CANCEL) {
            UpdateFetch.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val url = intent?.getStringExtra(EXTRA_URL)
        val code = intent?.getIntExtra(EXTRA_CODE, 0) ?: 0
        if (url.isNullOrBlank() || code <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }
        val release = AppRelease(
            versionCode = code,
            versionName = intent.getStringExtra(EXTRA_NAME).orEmpty().ifBlank { code.toString() },
            channel = intent.getStringExtra(EXTRA_CHANNEL).orEmpty(),
            notes = "",
            size = intent.getLongExtra(EXTRA_SIZE, 0L),
            sha256 = "",
            url = url,
        )
        val note = note(this, release.versionName, 0)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF, note)
        }
        UpdateFetch.begin(this, release)
        return START_NOT_STICKY
    }

    companion object {
        const val ACTION_CANCEL = "ntmt.schedule.update.CANCEL"
        const val EXTRA_URL = "url"
        const val EXTRA_NAME = "name"
        const val EXTRA_CODE = "code"
        const val EXTRA_SIZE = "size"
        const val EXTRA_CHANNEL = "channel"
        const val NOTIF = 48
        private const val CHANNEL = "update-download"

        fun start(ctx: Context, release: AppRelease) {
            val intent = Intent(ctx, UpdateDownloadService::class.java)
                .putExtra(EXTRA_URL, release.url)
                .putExtra(EXTRA_NAME, release.versionName)
                .putExtra(EXTRA_CODE, release.versionCode)
                .putExtra(EXTRA_SIZE, release.size)
                .putExtra(EXTRA_CHANNEL, release.channel)
            ContextCompat.startForegroundService(ctx, intent)
        }

        fun cancel(ctx: Context) {
            ctx.startService(Intent(ctx, UpdateDownloadService::class.java).setAction(ACTION_CANCEL))
        }

        fun note(ctx: Context, name: String, progress: Int): Notification {
            ensureChannel(ctx)
            val cancel = PendingIntent.getService(
                ctx,
                8,
                Intent(ctx, UpdateDownloadService::class.java).setAction(ACTION_CANCEL),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            return NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_ntmt)
                .setContentTitle("Загрузка $name")
                .setContentText("$progress%")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, progress.coerceIn(0, 100), false)
                .addAction(0, "Отмена", cancel)
                .build()
        }

        private fun ensureChannel(ctx: Context) {
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Загрузка обновления", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }
}
