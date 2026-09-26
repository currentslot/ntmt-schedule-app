package ntmt.schedule.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ntmt.schedule.MainActivity
import ntmt.schedule.R
import ntmt.schedule.data.NtmtApi
import ntmt.schedule.data.Prefs

class ScheduleWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SWAP) {
            runBlocking { Prefs(context).swapGroups() }
            refresh(context)
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        newOptions: Bundle,
    ) {
        render(context, manager, id)
    }

    companion object {
        const val ACTION_SWAP = "ntmt.schedule.widget.SWAP"
        const val EXTRA_TAB = "open_tab"

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ScheduleWidget::class.java))
            ids.forEach { render(context, manager, it) }
        }

        private fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val st = runBlocking { Prefs(context).state.first() }
            val palette = WidgetPalette.of(WidgetPalette.night(context, st.theme))
            val views = RemoteViews(context.packageName, R.layout.widget_schedule)
            val open = PendingIntent.getActivity(
                context,
                id,
                Intent(context, MainActivity::class.java).apply {
                    putExtra(EXTRA_TAB, st.widgetOpen)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            views.setPendingIntentTemplate(R.id.widget_list, open)
            views.setOnClickPendingIntent(R.id.widget_body, open)
            val shade = st.widgetAlpha.coerceIn(10, 100)
            views.setInt(R.id.widget_plate, "setColorFilter", palette.bg)
            views.setInt(R.id.widget_plate, "setImageAlpha", shade * 255 / 100)
            views.setTextColor(R.id.widget_title, palette.fg)
            views.setTextColor(R.id.widget_sub, palette.muted)
            views.setTextColor(R.id.widget_empty, palette.muted)
            views.setInt(R.id.widget_swap, "setColorFilter", palette.fg)
            val widthDp = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 220)
            val service = Intent(context, ScheduleWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra("night", palette.night)
                putExtra("widthDp", widthDp)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, service)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            val group = st.group
            val pack = ntmt.schedule.data.ScheduleCache.load(context)
            val weeks = group?.let { pack?.second?.get(it) }
            if (group == null || weeks == null) {
                views.setTextViewText(R.id.widget_title, "Расписание НТМТ")
                views.setTextViewText(R.id.widget_sub, if (group == null) "Выберите группу" else "Нет данных")
                views.setTextViewText(R.id.widget_empty, if (group == null) "Выберите группу" else "Нет данных")
                views.setViewVisibility(R.id.widget_sub, View.VISIBLE)
                views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            } else {
                val date = NtmtApi.today()
                views.setTextViewText(R.id.widget_title, group)
                if (st.widgetHeader) {
                    views.setViewVisibility(R.id.widget_sub, View.VISIBLE)
                    views.setTextViewText(R.id.widget_sub, subtitle(date))
                } else {
                    views.setViewVisibility(R.id.widget_sub, View.GONE)
                }
                views.setViewVisibility(R.id.widget_empty, View.GONE)
            }
            val swap = PendingIntent.getBroadcast(
                context,
                id,
                Intent(context, ScheduleWidget::class.java).apply { action = ACTION_SWAP },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            if (st.group2 != null && group != null) {
                views.setViewVisibility(R.id.widget_swap, View.VISIBLE)
                views.setOnClickPendingIntent(R.id.widget_title_btn, swap)
            } else {
                views.setViewVisibility(R.id.widget_swap, View.GONE)
                views.setOnClickPendingIntent(R.id.widget_title_btn, open)
            }
            manager.updateAppWidget(id, views)
            manager.notifyAppWidgetViewDataChanged(id, R.id.widget_list)
        }

        private fun subtitle(date: LocalDate): String {
            val day = when (date.dayOfWeek) {
                DayOfWeek.MONDAY -> "Понедельник"
                DayOfWeek.TUESDAY -> "Вторник"
                DayOfWeek.WEDNESDAY -> "Среда"
                DayOfWeek.THURSDAY -> "Четверг"
                DayOfWeek.FRIDAY -> "Пятница"
                DayOfWeek.SATURDAY -> "Суббота"
                else -> "Воскресенье"
            }
            val week = if (NtmtApi.isNumerator(date)) "Числитель" else "Знаменатель"
            val month = listOf("Января", "Февраля", "Марта", "Апреля", "Мая", "Июня", "Июля", "Августа", "Сентября", "Октября", "Ноября", "Декабря")[date.monthValue - 1]
            return "$day • ${date.dayOfMonth} $month • $week"
        }
    }
}
