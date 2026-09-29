package ntmt.schedule.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import ntmt.schedule.data.GroupWeeks
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
        val needMorning = st.morning && inWindow(now, st.morningTime) && st.lastMorning != today
        val needAuto = st.autoRefresh && inWindow(now, st.autoRefreshTime, 20) && st.lastAuto != today
        if (!needMorning && !needAuto) return
        if (needAuto && pullSchedule(prefs, st, allowNotify = false)) prefs.setLastAuto(today)
        if (!needMorning) return
        val cached = ScheduleCache.load(applicationContext)
        val map = cached?.second
        if (map == null) return
        if (hasPairs(group, map)) {
            Notify.show(
                applicationContext,
                2,
                Notify.CH_MORNING,
                "Пары · $group",
                "Сегодня в расписании есть пары",
            )
        }
        prefs.setLastMorning(today)
    }

    private suspend fun pullSchedule(prefs: Prefs, st: PrefState, allowNotify: Boolean): Boolean {
        val group = st.group ?: return true
        return try {
            val cached = ScheduleCache.load(applicationContext)
            val siteDt = NtmtApi.peekUpdateDt()
            if (cached != null && cached.first.updateDt == siteDt) return true
            val (catalog, map) = NtmtApi.load()
            ScheduleCache.save(applicationContext, catalog, map)
            ntmt.schedule.widget.ScheduleWidget.refresh(applicationContext)
            val weeks = map[group] ?: return true
            val snap = NtmtApi.snapshot(weeks)
            if (allowNotify && st.notify && st.snapshot != null && st.snapshot != snap) {
                Notify.show(
                    applicationContext,
                    1,
                    Notify.CH_CHANGE,
                    "Расписание $group",
                    "Обновлено на сайте ${catalog.updateDt}",
                )
            }
            prefs.setSnapshot(snap)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun hasPairs(group: String, map: Map<String, GroupWeeks>): Boolean {
        val weeks = map[group] ?: return false
        val slots = NtmtApi.slotsFor(weeks, NtmtApi.today())
        return NtmtApi.ordered(slots).any { (_, lessons) ->
            lessons.any { lesson ->
                !lesson.n.isNullOrBlank() || !lesson.a.isNullOrBlank() || !lesson.p.isNullOrBlank()
            }
        }
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
