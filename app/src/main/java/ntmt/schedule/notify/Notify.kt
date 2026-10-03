package ntmt.schedule.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ntmt.schedule.MainActivity
import ntmt.schedule.R

object Notify {
    const val CH_CHANGE = "changes"
    const val CH_MORNING = "morning"
    const val CH_UPDATE = "updates"
    const val CH_FILE = "cabinet-file"
    private const val FILE_ID = 41

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CH_CHANGE, "Изменения расписания", NotificationManager.IMPORTANCE_DEFAULT),
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_MORNING, "Утреннее напоминание", NotificationManager.IMPORTANCE_DEFAULT),
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_UPDATE, "Обновления приложения", NotificationManager.IMPORTANCE_DEFAULT),
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_FILE, "Загрузка файлов", NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null)
                enableVibration(false)
                vibrationPattern = longArrayOf(0L)
            },
        )
    }

    fun show(ctx: Context, id: Int, channel: String, title: String, body: String) {
        val open = PendingIntent.getActivity(
            ctx,
            0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_ntmt)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, n)
    }

    fun showUpdate(ctx: Context, title: String, body: String) {
        val open = PendingIntent.getActivity(
            ctx,
            3,
            Intent(ctx, MainActivity::class.java)
                .putExtra(ntmt.schedule.MainActivity.EXTRA_UPDATES, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CH_UPDATE)
            .setSmallIcon(R.drawable.ic_stat_ntmt)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(3, n)
    }

    fun fileProgress(ctx: Context, name: String, fraction: Float) {
        if (!canPost(ctx)) return
        val known = fraction > 0f
        val pct = (fraction * 100f).toInt().coerceIn(0, 100)
        val n = NotificationCompat.Builder(ctx, CH_FILE)
            .setSmallIcon(R.drawable.ic_stat_ntmt)
            .setContentTitle(name)
            .setContentText(if (known) "Загрузка $pct%" else "Загрузка")
            .setProgress(100, if (known) pct else 0, !known)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVibrate(null)
            .setContentIntent(openApp(ctx, 41))
            .build()
        manager(ctx).notify(FILE_ID, n)
    }

    fun fileDone(ctx: Context, name: String) {
        if (!canPost(ctx)) return
        val n = NotificationCompat.Builder(ctx, CH_FILE)
            .setSmallIcon(R.drawable.ic_stat_ntmt)
            .setContentTitle(name)
            .setContentText("Файл скачан")
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVibrate(null)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx, 42))
            .build()
        manager(ctx).notify(FILE_ID, n)
    }

    fun fileClear(ctx: Context) {
        manager(ctx).cancel(FILE_ID)
    }

    private fun manager(ctx: Context) = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun canPost(ctx: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT < 33) return true
        return ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun openApp(ctx: Context, code: Int): PendingIntent {
        return PendingIntent.getActivity(
            ctx,
            code,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
