package ntmt.schedule.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import ntmt.schedule.data.NtmtApi
import ntmt.schedule.data.Prefs
import ntmt.schedule.data.ScheduleCache
import ntmt.schedule.update.AppUpdate
import java.time.LocalTime

class ScheduleWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        val st = prefs.state.first()
        checkAppUpdate(prefs, st)
        val group = st.group ?: return Result.success()
        return try {
            val now = LocalTime.now(NtmtApi.tz)
            val today = NtmtApi.today().toString()
            val needMorning = st.morning && inWindow(now, st.morningTime) && st.lastMorning != today
            val needAuto = st.autoRefresh && inWindow(now, st.autoRefreshTime, 20) && st.lastAuto != today

            val cached = ScheduleCache.load(applicationContext)
            val siteDt = try {
                NtmtApi.peekUpdateDt()
            } catch (_: Exception) {
                cached?.first?.updateDt
            }
            val dtUnchanged = cached != null && siteDt != null && cached.first.updateDt == siteDt
            if (dtUnchanged && !needAuto) {
                if (needMorning) {
                    Notify.show(
                        applicationContext,
                        2,
                        Notify.CH_MORNING,
                        "Доброе утро · $group",
                        "Расписание на сегодня уже в приложении",
                    )
                    prefs.setLastMorning(today)
                }
                return Result.success()
            }

            val (catalog, map) = NtmtApi.load()
            ScheduleCache.save(applicationContext, catalog, map)
            ntmt.schedule.widget.ScheduleWidget.refresh(applicationContext)
            val weeks = map[group] ?: return Result.success()
            val snap = NtmtApi.snapshot(weeks)
            if (st.notify && st.snapshot != null && st.snapshot != snap) {
                Notify.show(
                    applicationContext,
                    1,
                    Notify.CH_CHANGE,
                    "Расписание $group",
                    "Обновлено на сайте ${catalog.updateDt}",
                )
            }
            prefs.setSnapshot(snap)
            if (needMorning) {
                Notify.show(
                    applicationContext,
                    2,
                    Notify.CH_MORNING,
                    "Доброе утро · $group",
                    "Расписание на сегодня уже в приложении",
                )
                prefs.setLastMorning(today)
            }
            if (needAuto) prefs.setLastAuto(today)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
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

    private suspend fun checkAppUpdate(prefs: Prefs, st: ntmt.schedule.data.PrefState) {
        if (!st.updateAuto) return
        val now = LocalTime.now(NtmtApi.tz)
        val today = NtmtApi.today().toString()
        if (!inWindow(now, st.updateCheckTime) || st.lastUpdateCheck == today) return
        try {
            val code = AppUpdate.installedCode(applicationContext)
            val found = AppUpdate.check(st.updateSource, st.updateChannel, code)
            val release = found.release
            if (st.updateNotify && found.newer && release != null && st.lastNotifiedUpdate != release.versionCode) {
                Notify.showUpdate(
                    applicationContext,
                    "Доступно обновление",
                    "${release.versionName}. Откройте, чтобы скачать.",
                )
                prefs.setLastNotifiedUpdate(release.versionCode)
            }
        } catch (_: Exception) {
        }
        prefs.setLastUpdateCheck(today)
    }
}
