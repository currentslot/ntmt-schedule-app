package ntmt.schedule.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ntmt.schedule.data.NtmtApi
import ntmt.schedule.data.Prefs
import ntmt.schedule.data.ScheduleCache
import java.time.ZonedDateTime

class WidgetFlip : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        ScheduleWidget.refresh(app)
        arm(app)
    }

    companion object {
        private const val ACTION = "ntmt.schedule.widget.FLIP"

        fun arm(context: Context) {
            val app = context.applicationContext
            val at = runCatching { runBlocking { next(app) } }.getOrNull() ?: return
            val am = app.getSystemService(AlarmManager::class.java) ?: return
            val pi = PendingIntent.getBroadcast(
                app,
                41,
                Intent(app, WidgetFlip::class.java).setAction(ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val trigger = at.coerceAtLeast(System.currentTimeMillis() + 5_000L)
            try {
                if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
                }
            } catch (_: Exception) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        }

        private suspend fun next(context: Context): Long {
            val zone = NtmtApi.tz
            val now = ZonedDateTime.now(zone)
            val marks = mutableListOf(now.toLocalDate().plusDays(1).atStartOfDay(zone).plusSeconds(20))
            val st = Prefs(context).state.first()
            val group = st.group
            val weeks = group?.let { ScheduleCache.load(context)?.second?.get(it) }
            if (st.showTomorrow && group != null && weeks != null) {
                val end = NtmtApi.dayEndMinute(group, weeks)
                if (end != null) {
                    val at = now.toLocalDate().atStartOfDay(zone).plusMinutes(end.toLong()).plusSeconds(20)
                    if (at.isAfter(now)) marks += at
                }
            }
            return marks.minOf { it.toInstant().toEpochMilli() }
        }
    }
}
