package ntmt.schedule.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.WeekFields

object NtmtApi {
    private const val BASE = "https://nti.urfu.ru"
    private val FULLTIME = listOf(1, 2, 3, 4, 6, 7, 8)
    val SEMESTERS = mapOf(
        1 to "1 семестр (1 полусеместр)",
        2 to "1 семестр (2 полусеместр)",
        3 to "2 семестр (1 полусеместр)",
        4 to "2 семестр (2 полусеместр)",
        5 to "Осенняя сессия",
        6 to "Зимняя сессия",
        7 to "Весенняя сессия",
        8 to "Летняя сессия",
    )
    val DAYS = listOf("ПОНЕДЕЛЬНИК", "ВТОРНИК", "СРЕДА", "ЧЕТВЕРГ", "ПЯТНИЦА", "СУББОТА", "ВОСКРЕСЕНЬЕ")
    val DAYS_SHORT = mapOf(
        "ПОНЕДЕЛЬНИК" to "Пн",
        "ВТОРНИК" to "Вт",
        "СРЕДА" to "Ср",
        "ЧЕТВЕРГ" to "Чт",
        "ПЯТНИЦА" to "Пт",
        "СУББОТА" to "Сб",
        "ВОСКРЕСЕНЬЕ" to "Вс",
    )
    val PAIRS = listOf("I", "II", "III", "IV", "V", "VI", "VII")
    val BELLS_ODD = mapOf(
        1 to "9:00–10:20",
        2 to "10:30–11:10 / 11:50–12:30",
        3 to "12:40–14:00",
        4 to "14:10–15:30",
        5 to "15:40–17:00",
        6 to "17:10–18:30",
    )
    val BELLS_EVEN = mapOf(
        1 to "9:00–10:20",
        2 to "10:30–11:50",
        3 to "12:40–14:00",
        4 to "14:10–15:30",
        5 to "15:40–17:00",
        6 to "17:10–18:30",
    )

    val tz: ZoneId = ZoneId.of("Asia/Yekaterinburg")

    fun today(): LocalDate = LocalDate.now(tz)

    // Понедельник недели-числителя. От него недели чередуются без сбоя на стыке лет:
    // в годах с 53 ISO-неделями (например, 2026) чётность номера недели сбивалась
    private val NUMERATOR_ANCHOR: LocalDate = LocalDate.of(2026, 1, 5)

    fun isNumerator(date: LocalDate): Boolean {
        val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
        val weeks = java.time.temporal.ChronoUnit.WEEKS.between(NUMERATOR_ANCHOR, monday)
        return Math.floorMod(weeks, 2L) == 0L
    }

    fun dayName(date: LocalDate): String = DAYS[date.dayOfWeek.value - 1]

    fun courseOdd(group: String): Boolean {
        val d = group.firstOrNull { it.isDigit() } ?: return true
        return d.digitToInt() % 2 == 1
    }

    fun pairTime(group: String, key: String): String {
        val n = PAIRS.indexOf(key) + 1
        val table = if (courseOdd(group)) BELLS_ODD else BELLS_EVEN
        return table[n].orEmpty()
    }

    fun pairRanges(group: String, key: String): List<Pair<String, String>> {
        val raw = pairTime(group, key)
        if (raw.isBlank()) return emptyList()
        return raw.split("/").mapNotNull { chunk ->
            val parts = chunk.trim().split(Regex("[–—-]")).map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size >= 2) parts[0] to parts[1] else null
        }
    }

    fun pairFromTo(group: String, key: String): String =
        pairRanges(group, key).joinToString(" · ") { (from, to) -> "с $from до $to" }

    fun formatUpdate(raw: String, todayIso: String = ""): String {
        val src = raw.trim().trim('"').replace("Обновлено:", "", ignoreCase = true).trim()
        val m = Regex("""^(\d{1,2})\.(\d{1,2})\.(\d{4})(?:[ T]+(\d{1,2}):(\d{2}))?""").find(src) ?: return src.ifBlank { "—" }
        val dd = m.groupValues[1].padStart(2, '0')
        val mo = m.groupValues[2].padStart(2, '0')
        val y = m.groupValues[3]
        val hhRaw = m.groupValues[4]
        val mm = m.groupValues[5]
        return if (hhRaw.isNotEmpty() && mm.isNotEmpty()) "$dd.$mo.$y ${hhRaw.padStart(2, '0')}:$mm" else "$dd.$mo.$y"
    }

    fun pairMarks(group: String, keys: List<String>, now: LocalTime = LocalTime.now(tz)): Map<String, String> {
        fun mins(s: String): Int {
            val p = s.split(":")
            val h = p.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: return -1
            val m = p.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            return h * 60 + m
        }
        data class Span(val k: String, val ranges: List<Pair<Int, Int>>)
        val nowM = now.hour * 60 + now.minute
        val spans = keys.mapNotNull { k ->
            val ranges = pairRanges(group, k).mapNotNull { (a, b) ->
                val f = mins(a)
                val t = mins(b)
                if (f >= 0 && t >= 0) f to t else null
            }
            if (ranges.isEmpty()) null else Span(k, ranges)
        }
        val cur = spans.find { s -> s.ranges.any { nowM >= it.first && nowM < it.second } }
        if (cur != null) return mapOf(cur.k to "now")
        val next = spans.find { s -> s.ranges.minOf { it.first } > nowM }
        return if (next != null) mapOf(next.k to "next") else emptyMap()
    }

    fun online(ctx: Context): Boolean {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun load(): Pair<Catalog, Map<String, GroupWeeks>> {
        val dt = fetchUpdateDt()
        val semRaw = JSONArray(get("$BASE/api/schedule/active_semester/ntmt?_=${System.currentTimeMillis()}"))
        val active = buildSet {
            for (i in 0 until semRaw.length()) {
                val o = semRaw.getJSONObject(i)
                if (o.optBoolean("active")) add(o.getInt("semester_id"))
            }
        }
        // Во время сессии очного семестра может не быть среди активных
        val sem = FULLTIME.firstOrNull { it in active }
            ?: active.minOrNull()
            ?: error("На сайте нет активного семестра")
        val raw = JSONObject(get("$BASE/api/schedule/ntmt/1/1/$sem?_=${System.currentTimeMillis()}"))
        val groups = mutableMapOf<String, GroupWeeks>()
        val names = raw.keys().asSequence().toList().sorted()
        for (name in names) groups[name] = parseWeeks(raw.getJSONArray(name))
        val today = today()
        val catalog = Catalog(
            updateDt = dt,
            semesterId = sem,
            semesterTitle = SEMESTERS[sem] ?: "семестр $sem",
            groups = names,
            weekTypeNumerator = isNumerator(today),
            todayName = dayName(today),
            todayIso = today.toString(),
            isoWeek = today.get(WeekFields.ISO.weekOfWeekBasedYear()),
            hash = contentHash(groups),
        )
        return catalog to groups
    }

    /** Сайт отдаёт среди аудиторий заглушки вроде «/», «-» или None: таких аудиторий нет */
    fun realRoom(name: String): Boolean {
        val s = name.trim()
        if (s.isEmpty() || s.equals("None", true) || s.equals("null", true)) return false
        return s.any { it.isLetterOrDigit() }
    }

    fun loadNamed(mode: Int, semesterId: Int): Map<String, GroupWeeks> {
        val raw = JSONObject(get("$BASE/api/schedule/ntmt/$mode/1/$semesterId?_=${System.currentTimeMillis()}"))
        val parsed = linkedMapOf<String, GroupWeeks>()
        for (name in raw.keys()) parsed[name] = parseWeeks(raw.getJSONArray(name))
        return mergeNamed(mode, parsed)
    }

    /**
     * Совместные занятия сайт отдаёт отдельной записью: «Дейнес ОН\ Нистулина АВ», «211\107», «201/211».
     * Такие занятия раскладываются по каждому преподавателю или аудитории, а в самом занятии
     * остаётся полная запись, чтобы было видно второго преподавателя или вторую аудиторию.
     * Записи без единого занятия в список не попадают.
     */
    fun mergeNamed(mode: Int, items: Map<String, GroupWeeks>): Map<String, GroupWeeks> {
        val split = if (mode == 3) Regex("""[\\/]""") else Regex("""\\""")
        val acc = linkedMapOf<String, MutableList<MutableMap<String, MutableMap<String, MutableList<Lesson>>>>>()
        for ((full, weeks) in items) {
            val parts = full.split(split).map { it.trim() }.filter { it.isNotEmpty() }
            val joint = parts.size > 1
            for (part in parts) {
                if (mode == 3 && !realRoom(part)) continue
                val target = acc.getOrPut(part) { mutableListOf() }
                weeks.forEachIndexed { wi, week ->
                    while (target.size <= wi) target += mutableMapOf()
                    week.forEach { (day, slots) ->
                        val dayAcc = target[wi].getOrPut(day) { mutableMapOf() }
                        slots.forEach { (pair, lessons) ->
                            val list = dayAcc.getOrPut(pair) { mutableListOf() }
                            for (l in lessons) {
                                val shown = when {
                                    !joint -> l
                                    mode == 3 -> l.copy(a = l.a ?: full.trim())
                                    else -> l.copy(p = l.p ?: full.trim())
                                }
                                if (shown !in list) list += shown
                            }
                        }
                    }
                }
            }
        }
        val kept = acc.filterValues { weeks -> weeks.any { w -> w.values.any { d -> d.values.any { it.isNotEmpty() } } } }
        val collator = java.text.Collator.getInstance(java.util.Locale("ru"))
        val order = if (mode == 3) {
            compareBy<String>({ it.toIntOrNull() == null }, { it.toIntOrNull() ?: 0 }).thenComparator { a, b -> collator.compare(a, b) }
        } else {
            Comparator<String> { a, b -> collator.compare(a, b) }
        }
        val out = linkedMapOf<String, GroupWeeks>()
        for (name in kept.keys.sortedWith(order)) out[name] = kept.getValue(name)
        return out
    }

    private fun parseWeeks(arr: JSONArray): GroupWeeks {
        val out = mutableListOf<Map<String, Map<String, List<Lesson>>>>()
        for (w in 0 until arr.length()) {
            val weekObj = arr.optJSONObject(w) ?: JSONObject()
            val week = mutableMapOf<String, Map<String, List<Lesson>>>()
            val days = weekObj.keys()
            while (days.hasNext()) {
                val day = days.next()
                val slotsObj = weekObj.optJSONObject(day) ?: continue
                val slots = mutableMapOf<String, List<Lesson>>()
                val keys = slotsObj.keys()
                while (keys.hasNext()) {
                    val pair = keys.next()
                    val les = slotsObj.optJSONArray(pair) ?: continue
                    val list = mutableListOf<Lesson>()
                    for (i in 0 until les.length()) {
                        val o = les.optJSONObject(i) ?: continue
                        list += Lesson(
                            n = jsonStr(o, "n"),
                            a = jsonStr(o, "a"),
                            p = jsonStr(o, "p"),
                            g = jsonStr(o, "g"),
                        )
                    }
                    slots[pair] = list
                }
                week[day] = slots
            }
            out += week
        }
        return out
    }

    private fun jsonStr(o: JSONObject, key: String): String? {
        if (!o.has(key) || o.isNull(key)) return null
        val s = o.opt(key)?.toString()?.trim().orEmpty()
        if (s.isEmpty() || s.equals("null", true) || s.equals("undefined", true) || s == "-" || s == "—") return null
        return s
    }

    /** Единая дата обновления сайта. Пары могут поменяться и без неё, поэтому есть ещё хэш содержимого */
    private fun fetchUpdateDt(): String {
        val body = get("$BASE/api/schedule/update_dt/nti?_=${System.currentTimeMillis()}", 8000, 8000)
        return parseUpdateDt(body)
    }

    private fun parseUpdateDt(body: String): String {
        val t = body.trim()
        return try {
            org.json.JSONTokener(t).nextValue()?.toString()?.trim()?.trim('"') ?: t.trim('"')
        } catch (_: Exception) {
            t.trim('"')
        }
    }

    /** Версия в User-Agent берётся из сборки, чтобы не устаревала */
    val USER_AGENT = "NTMT-Android/${ntmt.schedule.BuildConfig.VERSION_NAME.replace(' ', '-')} (${ntmt.schedule.BuildConfig.VERSION_CODE})"

    private fun get(url: String, connectMs: Int = 15000, readMs: Int = 20000): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = connectMs
            readTimeout = readMs
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Pragma", "no-cache")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        conn.inputStream.bufferedReader().use { return it.readText() }
    }

    /** Снимок расписания группы для уведомлений. Префикс версии: снимки старого формата не сравниваются */
    fun snapshot(weeks: GroupWeeks): String = SNAPSHOT_PREFIX + sha256(weeksText(weeks))

    fun snapshotCurrent(snap: String?): Boolean = snap != null && snap.startsWith(SNAPSHOT_PREFIX) && snap.length == SNAPSHOT_PREFIX.length + 64

    /** Хэш всего расписания: группы по алфавиту, дни и пары в одном порядке, чтобы порядок ключей JSON не влиял */
    fun contentHash(groups: Map<String, GroupWeeks>): String {
        val sb = StringBuilder()
        for (name in groups.keys.sorted()) sb.append('#').append(name).append('\n').append(weeksText(groups.getValue(name)))
        return sha256(sb.toString())
    }

    private const val SNAPSHOT_PREFIX = "2:"

    private fun weeksText(weeks: GroupWeeks): String {
        val sb = StringBuilder()
        weeks.forEachIndexed { wi, week ->
            for (day in week.keys.sorted()) {
                val slots = week.getValue(day)
                for (pair in slots.keys.sorted()) {
                    sb.append(wi).append('|').append(day).append('|').append(pair).append('|')
                    slots.getValue(pair).forEach { sb.append(it.n).append('/').append(it.a).append('/').append(it.p).append(';') }
                    sb.append('\n')
                }
            }
        }
        return sb.toString()
    }

    private fun sha256(text: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return md.joinToString("") { "%02x".format(it) }
    }

    fun dayEndMinute(group: String, weeks: GroupWeeks, date: LocalDate = today()): Int? {
        val ends = ordered(slotsFor(weeks, date)).mapNotNull { (key, lessons) ->
            if (lessons.none { filled(it) }) return@mapNotNull null
            pairRanges(group, key).mapNotNull { (_, end) -> minutes(end) }.maxOrNull()
        }
        return ends.maxOrNull()
    }

    fun dayOver(group: String, weeks: GroupWeeks, date: LocalDate = today(), now: LocalTime = LocalTime.now(tz)): Boolean {
        val last = dayEndMinute(group, weeks, date) ?: return false
        return now.hour * 60 + now.minute >= last
    }

    /**
     * Какой день показывать с включённым «расписанием на завтра»: после последней пары следующий день,
     * а пустое воскресенье пропускается до понедельника (в том числе, если сегодня воскресенье без пар)
     */
    fun shownDate(group: String, weeks: GroupWeeks, showTomorrow: Boolean, now: LocalTime = LocalTime.now(tz)): LocalDate {
        val day = today()
        if (!showTomorrow) return day
        var shown = if (dayOver(group, weeks, day, now) || emptySunday(weeks, day)) day.plusDays(1) else day
        if (shown != day && emptySunday(weeks, shown)) shown = shown.plusDays(1)
        return shown
    }

    fun shownOffset(group: String, weeks: GroupWeeks, showTomorrow: Boolean): Int =
        java.time.temporal.ChronoUnit.DAYS.between(today(), shownDate(group, weeks, showTomorrow)).toInt()

    /** Показанный день уже на следующей неделе (суббота вечером или воскресенье → понедельник) */
    fun nextWeek(date: LocalDate): Boolean {
        val monday = { d: LocalDate -> d.with(java.time.DayOfWeek.MONDAY) }
        return monday(date).isAfter(monday(today()))
    }

    private fun emptySunday(weeks: GroupWeeks, date: LocalDate): Boolean =
        date.dayOfWeek == java.time.DayOfWeek.SUNDAY && ordered(slotsFor(weeks, date)).none { (_, lessons) -> lessons.any { filled(it) } }

    fun daySummary(group: String, weeks: GroupWeeks, date: LocalDate = today()): String? {
        val pairs = ordered(slotsFor(weeks, date)).filter { (_, lessons) -> lessons.any { filled(it) } }
        if (pairs.isEmpty()) return null
        val from = pairRanges(group, pairs.first().first).firstOrNull()?.first
        val to = pairRanges(group, pairs.last().first).lastOrNull()?.second
        val count = pairCount(pairs.size)
        return if (from != null && to != null) "Сегодня $count, с $from до $to" else "Сегодня $count"
    }

    private fun filled(lesson: Lesson): Boolean {
        return !lesson.n.isNullOrBlank() || !lesson.a.isNullOrBlank() || !lesson.p.isNullOrBlank()
    }

    private fun minutes(raw: String): Int? {
        val p = raw.split(":")
        val h = p.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: return null
        val m = p.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    private fun pairCount(n: Int): String {
        val word = when {
            n % 100 in 11..14 -> "пар"
            n % 10 == 1 -> "пара"
            n % 10 in 2..4 -> "пары"
            else -> "пар"
        }
        return "$n $word"
    }

    fun slotsFor(weeks: GroupWeeks, date: LocalDate): Map<String, List<Lesson>> {
        val idx = if (isNumerator(date)) 0 else 1
        val week = weeks.getOrNull(idx) ?: return emptyMap()
        return week[dayName(date)] ?: emptyMap()
    }

    fun ordered(slots: Map<String, List<Lesson>>): List<Pair<String, List<Lesson>>> {
        val extra = slots.keys.filter { it !in PAIRS && it != "null" }
        return (PAIRS + extra).mapNotNull { k -> slots[k]?.takeIf { it.isNotEmpty() }?.let { k to it } }
    }
}

fun DayOfWeek.ru(): String = when (this) {
    DayOfWeek.MONDAY -> "Понедельник"
    DayOfWeek.TUESDAY -> "Вторник"
    DayOfWeek.WEDNESDAY -> "Среда"
    DayOfWeek.THURSDAY -> "Четверг"
    DayOfWeek.FRIDAY -> "Пятница"
    DayOfWeek.SATURDAY -> "Суббота"
    DayOfWeek.SUNDAY -> "Воскресенье"
}
