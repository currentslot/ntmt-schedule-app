package ntmt.schedule.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object FilterCache {
    fun load(ctx: Context, mode: Int, semesterId: Int, updateDt: String): Map<String, GroupWeeks>? {
        val file = file(ctx, mode)
        if (!file.exists()) return null
        return try {
            val root = JSONObject(file.readText())
            if (root.optInt("semesterId") != semesterId || root.optString("updateDt") != updateDt) return null
            val items = root.getJSONObject("items")
            val out = linkedMapOf<String, GroupWeeks>()
            val keys = items.keys()
            while (keys.hasNext()) {
                val name = keys.next()
                out[name] = parse(items.getJSONArray(name))
            }
            out
        } catch (_: Exception) {
            null
        }
    }

    fun save(ctx: Context, mode: Int, semesterId: Int, updateDt: String, items: Map<String, GroupWeeks>) {
        val body = JSONObject()
        val packed = JSONObject()
        for ((name, weeks) in items) packed.put(name, writeWeeks(weeks))
        body.put("semesterId", semesterId)
        body.put("updateDt", updateDt)
        body.put("items", packed)
        val file = file(ctx, mode)
        val tmp = File(ctx.filesDir, "${file.name}.tmp")
        tmp.writeText(body.toString())
        if (file.exists()) file.delete()
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    private fun file(ctx: Context, mode: Int) = File(ctx.filesDir, "schedule-filter-$mode.json")

    private fun writeWeeks(weeks: GroupWeeks): JSONArray {
        val arr = JSONArray()
        weeks.forEach { week ->
            val w = JSONObject()
            week.forEach { (day, slots) ->
                val s = JSONObject()
                slots.forEach { (key, lessons) ->
                    val a = JSONArray()
                    lessons.forEach { l ->
                        a.put(
                            JSONObject()
                                .put("n", l.n ?: JSONObject.NULL)
                                .put("a", l.a ?: JSONObject.NULL)
                                .put("p", l.p ?: JSONObject.NULL)
                                .put("g", l.g ?: JSONObject.NULL),
                        )
                    }
                    s.put(key, a)
                }
                w.put(day, s)
            }
            arr.put(w)
        }
        return arr
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
                            n = text(o, "n"),
                            a = text(o, "a"),
                            p = text(o, "p"),
                            g = text(o, "g"),
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

    private fun text(o: JSONObject, key: String): String? {
        if (!o.has(key) || o.isNull(key)) return null
        val s = o.opt(key)?.toString()?.trim().orEmpty()
        if (s.isEmpty() || s.equals("null", true)) return null
        return s
    }
}
