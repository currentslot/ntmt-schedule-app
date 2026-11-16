package ntmt.schedule.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import ntmt.schedule.data.NtmtApi
import ntmt.schedule.data.PrefState
import ntmt.schedule.data.Prefs
import ntmt.schedule.data.ScheduleCache
import java.time.LocalTime

class ScheduleWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        val st = prefs.state.first()
        return try {
            when (inputData.getString("reason")) {
                "widget" -> widgetTick(prefs, st)
                // Пуш с сервера не успел обработаться сразу: проверяем, как только будет сеть
                "push" -> if (st.notify && st.push && !pullSchedule(prefs, st, allowNotify = true)) return Result.retry()
                else -> timed(prefs, st)
            }
            ntmt.schedule.widget.ScheduleWidget.refresh(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun widgetTick(prefs: Prefs, st: PrefState) {
        val now = System.currentTimeMillis()
        if (st.liveWatch && now - st.lastLiveCheck >= ChangeWatch.GAP_MS) {
            if (pullSchedule(prefs, st, allowNotify = st.notify)) {
                prefs.setLastLiveCheck(System.currentTimeMillis())
            }
        }
    }

    private suspend fun timed(prefs: Prefs, st: PrefState) {
        val group = st.group ?: return
        val now = LocalTime.now(NtmtApi.tz)
        val today = NtmtApi.today().toString()
        // Воркер просыпается раз в 15 минут и позже в режиме Doze, поэтому окно берётся с запасом
        val needMorning = st.morning && inWindow(now, st.morningTime, CATCH_UP_MINUTES) && st.lastMorning != today
        // Автообновление расписания в выбранное время убрано в 3.0: расписание подтягивают пуш сервера,
        // «Следить по активности» и сам вход в приложение
        if (!needMorning) return
        val cached = ScheduleCache.load(applicationContext)
        val map = cached?.second
        if (map == null) return
        val weeks = map[group] ?: return
        val text = NtmtApi.daySummary(group, weeks)
        if (text != null) {
            Notify.show(
                applicationContext,
                2,
                Notify.CH_MORNING,
                "Пары · $group",
                text,
            )
        }
        prefs.setLastMorning(today)
    }

    private suspend fun pullSchedule(prefs: Prefs, st: PrefState, allowNotify: Boolean): Boolean =
        ScheduleCheck.pull(applicationContext, prefs, st, allowNotify)

    private companion object {
        const val CATCH_UP_MINUTES = 120L
    }

    private fun inWindow(now: LocalTime, raw: String, minutes: Long = 20): Boolean {
        val parts = raw.split(":")
        val hh = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: return false
        val mm = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val start = LocalTime.of(hh.coerceIn(0, 23), mm.coerceIn(0, 59))
        val end = start.plusMinutes(minutes)
        return if (!end.isBefore(start) && end != start) {
            !now.isBefore(start) && now.isBefore(end)
        } else {
            !now.isBefore(start) || now.isBefore(end)
        }
    }
}
