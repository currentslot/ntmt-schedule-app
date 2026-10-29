package ntmt.schedule.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import ntmt.schedule.data.NtmtApi
import ntmt.schedule.data.PrefState
import ntmt.schedule.data.Prefs
import ntmt.schedule.data.ScheduleCache
import ntmt.schedule.update.UpdateAuto
import java.time.LocalTime

class ScheduleWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        val st = prefs.state.first()
        return try {
            if (inputData.getString("reason") == "widget") widgetTick(prefs, st) else timed(prefs, st)
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
        UpdateAuto.run(applicationContext, force = false)
    }

    private suspend fun timed(prefs: Prefs, st: PrefState) {
        val group = st.group ?: return
        val now = LocalTime.now(NtmtApi.tz)
        val today = NtmtApi.today().toString()
        // Воркер просыпается раз в 15 минут и позже в режиме Doze, поэтому окно берётся с запасом
        val needMorning = st.morning && inWindow(now, st.morningTime, CATCH_UP_MINUTES) && st.lastMorning != today
        val needAuto = st.autoRefresh && inWindow(now, st.autoRefreshTime, CATCH_UP_MINUTES) && st.lastAuto != today
        if (!needMorning && !needAuto) return
        if (needAuto && pullSchedule(prefs, st, allowNotify = false)) prefs.setLastAuto(today)
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

    private suspend fun pullSchedule(prefs: Prefs, st: PrefState, allowNotify: Boolean): Boolean {
        val group = st.group ?: return true
        return try {
            // Дате на сайте верить нельзя: пары меняют и без новой даты, поэтому расписание
            // скачивается целиком (в gzip это несколько КБ) и сравнивается по содержимому
            val cached = ScheduleCache.load(applicationContext)
            val (catalog, map) = NtmtApi.load()
            if (cached == null || cached.first.stamp != catalog.stamp || cached.first.semesterId != catalog.semesterId) {
                ScheduleCache.save(applicationContext, catalog, map)
                ntmt.schedule.widget.ScheduleWidget.refresh(applicationContext)
            }
            val weeks = map[group] ?: return true
            val snap = NtmtApi.snapshot(weeks)
            val prev = st.snapshot
            if (allowNotify && st.notify && NtmtApi.snapshotCurrent(prev) && prev != snap) {
                val sameDate = cached != null && cached.first.updateDt == catalog.updateDt
                Notify.show(
                    applicationContext,
                    1,
                    Notify.CH_CHANGE,
                    "Расписание $group",
                    if (sameDate) "Изменилось на сайте" else "Обновлено на сайте ${NtmtApi.formatUpdate(catalog.updateDt)}",
                )
            }
            prefs.setSnapshot(snap)
            true
        } catch (_: Exception) {
            false
        }
    }

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
