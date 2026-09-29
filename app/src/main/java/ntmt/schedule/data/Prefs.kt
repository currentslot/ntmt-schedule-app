package ntmt.schedule.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ds by preferencesDataStore("ntmt")

data class PrefState(
    val group: String? = null,
    val group2: String? = null,
    val notify: Boolean = true,
    val morning: Boolean = false,
    val morningTime: String = "07:30",
    val autoRefresh: Boolean = true,
    val autoRefreshTime: String = "06:00",
    val theme: String = "auto",
    val materialYou: Boolean = true,
    val tab: String = "Today",
    val launcher: String = "calendar",
    val snapshot: String? = null,
    val lastMorning: String? = null,
    val lastAuto: String? = null,
    val highlightCourse: Boolean = true,
    val highlightWeekToday: Boolean = true,
    val liveWatch: Boolean = true,
    val widgetNumber: Boolean = true,
    val widgetShowTime: Boolean = true,
    val widgetRoom: Boolean = true,
    val widgetTime: String = "full",
    val widgetPlace: String = "above",
    val widgetHeader: Boolean = true,
    val widgetFullDate: Boolean = false,
    val widgetOpen: String = "Today",
    val widgetAlpha: Int = 100,
    val updateChannel: String = "stable",
    val updateSource: String = "github",
    val updateNotify: Boolean = true,
    val updateAuto: Boolean = true,
    val updateCheckTime: String = "09:00",
    val lastUpdateCheck: String? = null,
    val lastUpdateCheckAt: Long = 0L,
    val lastLiveCheck: Long = 0L,
    val availableUpdate: Int = 0,
    val lastNotifiedUpdate: Int = 0,
)

class Prefs(private val context: Context) {
    private val G = stringPreferencesKey("group")
    private val G2 = stringPreferencesKey("group2")
    private val N = booleanPreferencesKey("notify")
    private val M = booleanPreferencesKey("morning")
    private val MT = stringPreferencesKey("morningTime")
    private val AR = booleanPreferencesKey("autoRefresh")
    private val ART = stringPreferencesKey("autoRefreshTime")
    private val TH = stringPreferencesKey("theme")
    private val MY = booleanPreferencesKey("materialYou")
    private val T = stringPreferencesKey("tab")
    private val L = stringPreferencesKey("launcher")
    private val S = stringPreferencesKey("snapshot")
    private val LM = stringPreferencesKey("lastMorning")
    private val LA = stringPreferencesKey("lastAuto")
    private val HC = booleanPreferencesKey("highlightCourse")
    private val HW = booleanPreferencesKey("highlightWeekToday")
    private val LW = booleanPreferencesKey("liveWatch")
    private val WN = booleanPreferencesKey("widgetNumber")
    private val WST = booleanPreferencesKey("widgetShowTime")
    private val WR = booleanPreferencesKey("widgetRoom")
    private val WT = stringPreferencesKey("widgetTime")
    private val WP = stringPreferencesKey("widgetPlace")
    private val WH = booleanPreferencesKey("widgetHeader")
    private val WF = booleanPreferencesKey("widgetFullDate")
    private val WO = stringPreferencesKey("widgetOpen")
    private val WA = intPreferencesKey("widgetAlpha")
    private val UC = stringPreferencesKey("updateChannel")
    private val US = stringPreferencesKey("updateSource")
    private val UN = booleanPreferencesKey("updateNotify")
    private val UA = booleanPreferencesKey("updateAuto")
    private val UCT = stringPreferencesKey("updateCheckTime")
    private val LUC = stringPreferencesKey("lastUpdateCheck")
    private val LUCA = longPreferencesKey("lastUpdateCheckAt")
    private val LLC = longPreferencesKey("lastLiveCheck")
    private val AU = intPreferencesKey("availableUpdate")
    private val LNU = intPreferencesKey("lastNotifiedUpdate")

    val state: Flow<PrefState> = context.ds.data.map { saved ->
        PrefState(
            group = saved[G],
            group2 = saved[G2],
            notify = saved[N] ?: true,
            morning = saved[M] ?: false,
            morningTime = normTime(saved[MT], "07:30"),
            autoRefresh = saved[AR] ?: true,
            autoRefreshTime = normTime(saved[ART], "06:00"),
            theme = saved[TH] ?: "auto",
            materialYou = saved[MY] ?: true,
            tab = saved[T] ?: "Today",
            launcher = normLauncher(saved[L]),
            snapshot = saved[S],
            lastMorning = saved[LM],
            lastAuto = saved[LA],
            highlightCourse = saved[HC] ?: true,
            highlightWeekToday = saved[HW] ?: true,
            liveWatch = saved[LW] ?: true,
            widgetNumber = saved[WN] ?: true,
            widgetShowTime = saved[WST] ?: true,
            widgetRoom = saved[WR] ?: true,
            widgetTime = if (saved[WT] == "start") "start" else "full",
            widgetPlace = if (saved[WP] == "inline") "inline" else "above",
            widgetHeader = saved[WH] ?: true,
            widgetFullDate = saved[WF] ?: false,
            widgetOpen = normTab(saved[WO]),
            widgetAlpha = (saved[WA] ?: 100).coerceIn(10, 100),
            updateChannel = when (saved[UC]) {
                "beta" -> "beta"
                else -> "stable"
            },
            updateSource = when (saved[US]) {
                "server" -> "server"
                else -> "github"
            },
            updateNotify = saved[UN] ?: true,
            updateAuto = saved[UA] ?: true,
            updateCheckTime = normTime(saved[UCT], "09:00"),
            lastUpdateCheck = saved[LUC],
            lastUpdateCheckAt = saved[LUCA] ?: 0L,
            lastLiveCheck = saved[LLC] ?: 0L,
            availableUpdate = saved[AU] ?: 0,
            lastNotifiedUpdate = saved[LNU] ?: 0,
        )
    }

    suspend fun ensureUpdateDefaults() = context.ds.edit { saved ->
        if (saved[UC] != null || saved[US] != null) return@edit
        val legacy = saved.asMap().isNotEmpty()
        if (legacy) {
            saved[UC] = "beta"
            saved[US] = "server"
        } else {
            saved[UC] = "stable"
            saved[US] = "github"
            saved[UN] = true
            saved[UA] = true
            saved[MY] = true
        }
    }

    suspend fun setGroup(v: String) = context.ds.edit { it[G] = v; it.remove(S) }
    suspend fun setGroup2(v: String?) = context.ds.edit {
        if (v.isNullOrBlank()) it.remove(G2) else it[G2] = v
    }
    suspend fun swapGroups() = context.ds.edit {
        val a = it[G] ?: return@edit
        val b = it[G2] ?: return@edit
        it[G] = b
        it[G2] = a
        it.remove(S)
    }
    suspend fun setNotify(v: Boolean) = context.ds.edit { it[N] = v }
    suspend fun setMorning(v: Boolean) = context.ds.edit { it[M] = v }
    suspend fun setMorningTime(v: String) = context.ds.edit { it[MT] = normTime(v, "07:30") }
    suspend fun setAutoRefresh(v: Boolean) = context.ds.edit { it[AR] = v }
    suspend fun setAutoRefreshTime(v: String) = context.ds.edit { it[ART] = normTime(v, "06:00") }
    suspend fun setTheme(v: String) = context.ds.edit { it[TH] = v }
    suspend fun setMaterialYou(v: Boolean) = context.ds.edit { it[MY] = v }
    suspend fun setTab(v: String) = context.ds.edit { it[T] = v }
    suspend fun setSnapshot(v: String) = context.ds.edit { it[S] = v }
    suspend fun setLastMorning(v: String) = context.ds.edit { it[LM] = v }
    suspend fun setLastAuto(v: String) = context.ds.edit { it[LA] = v }
    suspend fun setHighlightCourse(v: Boolean) = context.ds.edit { it[HC] = v }
    suspend fun setHighlightWeekToday(v: Boolean) = context.ds.edit { it[HW] = v }
    suspend fun setLiveWatch(v: Boolean) = context.ds.edit { it[LW] = v }
    suspend fun setWidgetNumber(v: Boolean) = context.ds.edit { it[WN] = v }
    suspend fun setWidgetShowTime(v: Boolean) = context.ds.edit { it[WST] = v }
    suspend fun setWidgetRoom(v: Boolean) = context.ds.edit { it[WR] = v }
    suspend fun setWidgetTime(v: String) = context.ds.edit { it[WT] = if (v == "start") "start" else "full" }
    suspend fun setWidgetPlace(v: String) = context.ds.edit { it[WP] = if (v == "inline") "inline" else "above" }
    suspend fun setWidgetHeader(v: Boolean) = context.ds.edit { it[WH] = v }
    suspend fun setWidgetFullDate(v: Boolean) = context.ds.edit { it[WF] = v }
    suspend fun setWidgetOpen(v: String) = context.ds.edit { it[WO] = normTab(v) }
    suspend fun setWidgetAlpha(v: Int) = context.ds.edit { it[WA] = v.coerceIn(10, 100) }
    suspend fun setUpdateChannel(v: String) = context.ds.edit { it[UC] = if (v == "stable") "stable" else "beta" }
    suspend fun setUpdateSource(v: String) = context.ds.edit {
        it[US] = if (v == "github") "github" else "server"
    }
    suspend fun setUpdateNotify(v: Boolean) = context.ds.edit { it[UN] = v }
    suspend fun setUpdateAuto(v: Boolean) = context.ds.edit { it[UA] = v }
    suspend fun setUpdateCheckTime(v: String) = context.ds.edit { it[UCT] = normTime(v, "09:00") }
    suspend fun setLastUpdateCheck(v: String) = context.ds.edit { it[LUC] = v }
    suspend fun setLastUpdateCheckAt(v: Long) = context.ds.edit { it[LUCA] = v }
    suspend fun setLastLiveCheck(v: Long) = context.ds.edit { it[LLC] = v }
    suspend fun setAvailableUpdate(v: Int) = context.ds.edit { it[AU] = v }
    suspend fun setLastNotifiedUpdate(v: Int) = context.ds.edit { it[LNU] = v }

    suspend fun setLauncher(kind: String) {
        val resolved = "calendar"
        context.ds.edit { it[L] = resolved }
        val pm = context.packageManager
        try {
            pm.setComponentEnabledSetting(
                ComponentName(context, "ntmt.schedule.LauncherMaterial"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        } catch (_: Exception) {
        }
    }
}

private fun normLauncher(raw: String?): String = "calendar"

private fun normTab(raw: String?): String = when (raw) {
    "Week", "Bells", "More" -> raw
    else -> "Today"
}

private fun normTime(raw: String?, fallback: String): String {
    val src = raw ?: fallback
    val p = src.trim().split(":")
    val h = p.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: fallback.substringBefore(":").toIntOrNull() ?: 7
    val m = p.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
}
