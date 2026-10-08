package ntmt.schedule.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object ScheduleCache {
    private const val FILE = "schedule-cache.json"

    fun save(ctx: Context, catalog: Catalog, groups: Map<String, GroupWeeks>) {
        val root = JSONObject()
        root.put("savedAt", System.currentTimeMillis())
        root.put(
            "catalog",
            JSONObject()
                .put("updateDt", catalog.updateDt)
                .put("semesterId", catalog.semesterId)
                .put("semesterTitle", catalog.semesterTitle)
                .put("weekTypeNumerator", catalog.weekTypeNumerator)
                .put("todayName", catalog.todayName)
                .put("todayIso", catalog.todayIso)
                .put("isoWeek", catalog.isoWeek)
                .put("groups", JSONArray(catalog.groups)),
        )
        val g = JSONObject()
        for ((name, weeks) in groups) {
            val arr = JSONArray()
            weeks.forEach { week ->
                val w = JSONObject()
                week.forEach { (day, slots) ->
                    val s = JSONObject()
                    slots.forEach { (k, lessons) ->
                        val a = JSONArray()
                        lessons.forEach { l ->
                            a.put(JSONObject().put("n", l.n ?: JSONObject.NULL).put("a", l.a ?: JSONObject.NULL).put("p", l.p ?: JSONObject.NULL))
                        }
                        s.put(k, a)
                    }
                    w.put(day, s)
                }
                arr.put(w)
            }
            g.put(name, arr)
        }
        root.put("groups", g)
        val dir = ctx.filesDir
        val file = File(dir, FILE)
        val tmp = File(dir, "$FILE.tmp")
        writeFresh(tmp, root.toString())
        if (file.exists() && !file.delete()) file.writeBytes(ByteArray(0))
        if (!tmp.renameTo(file)) {
            writeFresh(file, tmp.readText())
            tmp.delete()
        }
        sweep(ctx)
    }

    fun load(ctx: Context): Pair<Catalog, Map<String, GroupWeeks>>? {
        sweep(ctx)
        val f = File(ctx.filesDir, FILE)
        if (!f.exists()) return null
        return try {
            val root = JSONObject(f.readText())
            val c = root.getJSONObject("catalog")
            val names = mutableListOf<String>()
            val namesArr = c.getJSONArray("groups")
            for (i in 0 until namesArr.length()) names += namesArr.getString(i)
            val catalog = Catalog(
                updateDt = c.getString("updateDt"),
                semesterId = c.getInt("semesterId"),
                semesterTitle = c.getString("semesterTitle"),
                groups = names,
                weekTypeNumerator = c.getBoolean("weekTypeNumerator"),
                todayName = c.optString("todayName"),
                todayIso = c.optString("todayIso"),
                isoWeek = c.optInt("isoWeek"),
            )
            val groups = mutableMapOf<String, GroupWeeks>()
            val g = root.getJSONObject("groups")
            val keys = g.keys()
            while (keys.hasNext()) {
                val name = keys.next()
                groups[name] = parse(g.getJSONArray(name))
            }
            catalog to groups
        } catch (_: Exception) {
            null
        }
    }

    fun sweep(ctx: Context) {
        val dir = ctx.filesDir
        val file = File(dir, FILE)
        if (file.exists()) {
            val text = try {
                file.readText()
            } catch (_: Exception) {
                ""
            }
            val whole = try {
                JSONObject(text)
                true
            } catch (_: Exception) {
                false
            }
            if (!whole) {
                val last = lastObject(text)
                if (last != null && try {
                        JSONObject(last)
                        true
                    } catch (_: Exception) {
                        false
                    }
                ) writeFresh(file, last) else file.delete()
            }
        }
        dir.listFiles()?.forEach { extra ->
            if (extra.name.startsWith("schedule-cache") && extra.name != FILE) extra.delete()
        }
    }

    private fun writeFresh(file: File, text: String) {
        FileOutputStream(file, false).use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    private fun lastObject(text: String): String? {
        var start = -1
        var depth = 0
        var inStr = false
        var esc = false
        var last: String? = null
        for (i in text.indices) {
            val c = text[i]
            if (inStr) {
                if (esc) esc = false
                else if (c == '\\') esc = true
                else if (c == '"') inStr = false
                continue
            }
            when (c) {
                '"' -> inStr = true
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                '}' -> if (depth > 0) {
                    depth--
                    if (depth == 0 && start >= 0) {
                        last = text.substring(start, i + 1)
                        start = -1
                    }
                }
            }
        }
        return last
    }

    private fun parse(arr: JSONArray): GroupWeeks {
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
                            n = o.optString("n").takeIf { it.isNotBlank() && it != "null" },
                            a = o.opt("a")?.toString()?.takeIf { it != "null" && it.isNotBlank() },
                            p = o.optString("p").takeIf { it.isNotBlank() && it != "null" },
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
}
