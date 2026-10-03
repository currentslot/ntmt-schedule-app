package ntmt.schedule.widget

import android.content.Intent
import android.graphics.Paint
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ntmt.schedule.R
import ntmt.schedule.data.NtmtApi
import ntmt.schedule.data.Prefs
import ntmt.schedule.data.PrefState
import ntmt.schedule.data.ScheduleCache

class ScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext, intent)

    private class Factory(
        private val context: android.content.Context,
        private val intent: Intent,
    ) : RemoteViewsFactory {
        private var rows: List<Line> = emptyList()
        private var day: WidgetPalette = WidgetPalette.of(false)
        private var night: WidgetPalette = WidgetPalette.of(true)
        private var follow = true
        private var nightNow = false
        private var widthDp: Int = 220
        private var alpha: Int = 100

        override fun onCreate() = load()
        override fun onDataSetChanged() = load()
        override fun onDestroy() {
            rows = emptyList()
        }

        override fun getCount(): Int = rows.size

        override fun getViewAt(position: Int): RemoteViews {
            val line = rows.getOrElse(position) { Line("", "Сегодня пар нет", false) }
            val views = RemoteViews(context.packageName, R.layout.widget_pair)
            val dayPill = day.pillTone(alpha)
            val nightPill = night.pillTone(alpha)
            val dayFg = chipFg(day, dayPill)
            val nightFg = chipFg(night, nightPill)
            views.applyTone(R.id.pair_title, "setTextColor", day.fg, night.fg, follow, nightNow)
            views.applyTone(R.id.pair_more, "setTextColor", day.fg, night.fg, follow, nightNow)
            views.applyTone(R.id.pair_chip, "setTextColor", dayFg, nightFg, follow, nightNow)
            views.setViewVisibility(R.id.pair_more, android.view.View.GONE)
            if (line.meta.isBlank()) {
                views.setViewVisibility(R.id.pair_chip, android.view.View.GONE)
                views.setTextViewText(R.id.pair_title, line.title)
                views.setViewVisibility(R.id.pair_title, android.view.View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.pair_chip, android.view.View.VISIBLE)
                views.setTextViewText(R.id.pair_chip, line.meta)
                views.applyChip(R.id.pair_chip, dayPill, nightPill, follow, nightNow, day.dynamic || night.dynamic)
                val (head, tail) = splitTitle(line.title, line.meta)
                if (head.isEmpty()) {
                    views.setViewVisibility(R.id.pair_title, android.view.View.GONE)
                    views.setTextViewText(R.id.pair_more, tail)
                    views.setViewVisibility(R.id.pair_more, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.pair_title, android.view.View.VISIBLE)
                    views.setTextViewText(R.id.pair_title, head)
                    if (tail.isEmpty()) {
                        views.setViewVisibility(R.id.pair_more, android.view.View.GONE)
                    } else {
                        views.setTextViewText(R.id.pair_more, tail)
                        views.setViewVisibility(R.id.pair_more, android.view.View.VISIBLE)
                    }
                }
            }
            views.setOnClickFillInIntent(R.id.pair_root, Intent())
            return views
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 1
        override fun getItemId(position: Int): Long = position.toLong()
        override fun hasStableIds(): Boolean = true

        private fun load() {
            rows = runBlocking {
                val st = Prefs(context).state.first()
                follow = st.theme != "light" && st.theme != "dark"
                nightNow = if (intent.hasExtra("nightNow")) {
                    intent.getBooleanExtra("nightNow", false)
                } else {
                    WidgetPalette.night(context, st.theme, context.resources.configuration)
                }
                day = WidgetPalette.resolve(context, false, st.materialYou)
                night = WidgetPalette.resolve(context, true, st.materialYou)
                alpha = st.widgetAlpha
                widthDp = intent.getIntExtra("widthDp", 220).coerceAtLeast(120)
                val group = st.group
                val weeks = group?.let { ScheduleCache.load(context)?.second?.get(it) }
                if (group == null || weeks == null) emptyList()
                else pairRows(group, weeks, st)
            }
        }

        private fun chipFg(palette: WidgetPalette, pill: Int): Int {
            return if (WidgetPalette.contrast(pill, palette.fg) >= 3.0) palette.fg else palette.bg
        }

        private fun pairRows(group: String, weeks: ntmt.schedule.data.GroupWeeks, st: PrefState): List<Line> {
            val slots = NtmtApi.slotsFor(weeks, NtmtApi.today())
            val ordered = NtmtApi.ordered(slots)
            if (ordered.isEmpty()) return listOf(Line("", "Сегодня пар нет", true))
            return ordered.flatMap { (key, lessons) ->
                val n = NtmtApi.PAIRS.indexOf(key).let { if (it < 0) "" else "${it + 1}" }
                val range = NtmtApi.pairRanges(group, key).firstOrNull()
                val time = when {
                    !st.widgetShowTime || range == null -> ""
                    st.widgetTime == "start" -> range.first
                    else -> "${range.first}–${range.second}"
                }
                val items = if (lessons.isEmpty()) listOf(null) else lessons
                items.map { lesson ->
                    val room = clean(lesson?.a)
                    val title = clean(lesson?.n) ?: "Занятие"
                    val parts = buildList {
                        if (st.widgetNumber && n.isNotEmpty()) add(n)
                        if (time.isNotEmpty()) add(time)
                        if (st.widgetRoom && room != null) add(room)
                    }
                    Line(parts.joinToString(" • "), title, true)
                }
            }
        }

        private fun splitTitle(title: String, meta: String): Pair<String, String> {
            val dm = context.resources.displayMetrics
            val titlePaint = Paint().apply { textSize = 13f * dm.scaledDensity }
            val chipPaint = Paint().apply { textSize = 11f * dm.scaledDensity }
            val content = (widthDp - 24).coerceAtLeast(80) * dm.density
            val chipW = chipPaint.measureText(meta) + 12f * dm.density + 6f * dm.density
            val avail = content - chipW
            if (avail <= 24f * dm.density) return "" to title
            if (titlePaint.measureText(title) <= avail) return title to ""
            val count = titlePaint.breakText(title, true, avail, null)
            if (count <= 0) return "" to title
            var cut = count
            if (cut < title.length && !title[cut - 1].isWhitespace() && title[cut].isLetterOrDigit()) {
                val space = title.lastIndexOf(' ', cut - 1)
                if (space > 0) cut = space
            }
            val head = title.substring(0, cut).trim()
            val tail = title.substring(cut).trim()
            return if (head.isEmpty()) "" to title else head to tail
        }

        private fun clean(v: String?): String? {
            val s = v?.trim().orEmpty()
            if (s.isEmpty() || s.equals("null", true) || s == "-" || s == "—") return null
            return s
        }
    }

    private data class Line(val meta: String, val title: String, val inline: Boolean)
}
