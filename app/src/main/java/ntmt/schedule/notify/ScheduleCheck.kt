package ntmt.schedule.notify

import android.content.Context
import ntmt.schedule.data.NtmtApi
import ntmt.schedule.data.PrefState
import ntmt.schedule.data.Prefs
import ntmt.schedule.data.ScheduleCache

/**
 * Одна проверка расписания: скачать, обновить кэш и виджет, сравнить снимок своей группы.
 * Общая для фоновой проверки телефона и для пуша с сервера, поэтому одно и то же изменение
 * не приходит уведомлением дважды: после первой проверки снимок уже совпадает.
 */
object ScheduleCheck {
    suspend fun pull(ctx: Context, prefs: Prefs, st: PrefState, allowNotify: Boolean): Boolean {
        val group = st.group ?: return true
        return try {
            // Дате на сайте верить нельзя: пары меняют и без новой даты, поэтому расписание
            // скачивается целиком (в gzip это несколько КБ) и сравнивается по содержимому
            val cached = ScheduleCache.load(ctx)
            val (catalog, map) = NtmtApi.load()
            if (cached == null || cached.first.stamp != catalog.stamp || cached.first.semesterId != catalog.semesterId) {
                ScheduleCache.save(ctx, catalog, map)
                ntmt.schedule.widget.ScheduleWidget.refresh(ctx)
            }
            val weeks = map[group] ?: return true
            val snap = NtmtApi.snapshot(weeks)
            val prev = st.snapshot
            val groupChanged = NtmtApi.snapshotCurrent(prev) && prev != snap
            val dateChanged = cached != null && cached.first.updateDt != catalog.updateDt
            if (allowNotify && st.notify) {
                if (groupChanged) {
                    Notify.show(
                        ctx,
                        1,
                        Notify.CH_CHANGE,
                        "Расписание $group",
                        if (cached != null && !dateChanged) "Изменилось на сайте" else "Обновлено на сайте ${NtmtApi.formatUpdate(catalog.updateDt)}",
                    )
                } else if (dateChanged) {
                    // Дата на сайте новая, а пары своей группы те же: об этом тоже сообщаем.
                    // Дважды не придёт: новая дата уже сохранена в кэше
                    Notify.show(
                        ctx,
                        1,
                        Notify.CH_CHANGE,
                        "Расписание обновлено на сайте",
                        "${NtmtApi.formatUpdate(catalog.updateDt)}. У группы $group изменений нет",
                    )
                }
            }
            prefs.setSnapshot(snap)
            true
        } catch (_: Exception) {
            false
        }
    }
}
