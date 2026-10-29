package ntmt.schedule.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    val showTomorrow: Boolean = true,
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
    val updateNotify: Boolean = true,
    val updateAuto: Boolean = true,
    val updateCheckTime: String = "09:00",
    val lastUpdateCheck: String? = null,
    val lastUpdateCheckAt: Long = 0L,
    val lastLiveCheck: Long = 0L,
    val availableUpdate: Int = 0,
    val lastNotifiedUpdate: Int = 0,
    val updateExpandedOnce: Boolean = false,
    val backup: BackupState = BackupState(),
)

/** Папка кабинета, выбранная для резервной копии. [parent] нужен, чтобы проверить, что папка ещё есть на сервере */
data class CabinetFolder(val url: String, val parent: String, val path: String) {
    fun encode() = listOf(url, parent, path).joinToString("\t")

    companion object {
        fun decode(raw: String): CabinetFolder? {
            val p = raw.split('\t')
            if (p.size < 3 || p[0].isBlank()) return null
            return CabinetFolder(p[0], p[1], p[2])
        }
    }
}

/** Настройки и итог резервного копирования учебных файлов */
data class BackupState(
    val on: Boolean = false,
    val days: Int = 7,
    val dest: String? = null,
    val dirs: Set<String> = emptySet(),
    val saved: Boolean = true,
    val cabinet: Boolean = false,
    val cabinetFolders: List<CabinetFolder> = emptyList(),
    val lastAt: Long = 0L,
    val last: String? = null,
    val cabinetAt: Long = 0L,
    val cabinetLast: String? = null,
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
    private val ST = booleanPreferencesKey("showTomorrow")
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
    private val UN = booleanPreferencesKey("updateNotify")
    private val UA = booleanPreferencesKey("updateAuto")
    private val UCT = stringPreferencesKey("updateCheckTime")
    private val LUC = stringPreferencesKey("lastUpdateCheck")
    private val LUCA = longPreferencesKey("lastUpdateCheckAt")
    private val LLC = longPreferencesKey("lastLiveCheck")
    private val AU = intPreferencesKey("availableUpdate")
    private val LNU = intPreferencesKey("lastNotifiedUpdate")
    private val UEO = booleanPreferencesKey("updateExpandedOnce")
    private val BON = booleanPreferencesKey("backupOn")
    private val BDAYS = intPreferencesKey("backupDays")
    private val BDEST = stringPreferencesKey("backupDest")
    private val BDIRS = stringSetPreferencesKey("backupDirs")
    private val BSAVED = booleanPreferencesKey("backupSaved")
    private val BCAB = booleanPreferencesKey("backupCabinet")
    private val BCFOLD = stringSetPreferencesKey("backupCabinetFolders")
    private val BLAT = longPreferencesKey("backupLastAt")
    private val BLAST = stringPreferencesKey("backupLast")
    private val BPRINT = stringPreferencesKey("backupPrint")
    private val BCAT = longPreferencesKey("backupCabinetAt")
    private val BCLAST = stringPreferencesKey("backupCabinetLast")
    private val BCPRINT = stringPreferencesKey("backupCabinetPrint")

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
            showTomorrow = saved[ST] ?: true,
            liveWatch = saved[LW] ?: true,
            widgetNumber = saved[WN] ?: true,
            widgetShowTime = saved[WST] ?: true,
            widgetRoom = saved[WR] ?: true,
            widgetTime = if (saved[WT] == "start") "start" else "full",
            widgetPlace = if (saved[WP] == "inline") "inline" else "above",
            widgetHeader = saved[WH] ?: true,
            widgetFullDate = saved[WF] ?: false,
            widgetOpen = saved[WO]?.let(::normTab)
                ?: if (saved[G] != null || saved[T] != null || saved[S] != null) "Today" else "Cabinet",
            widgetAlpha = (saved[WA] ?: 100).coerceIn(10, 100),
            updateChannel = when (saved[UC]) {
                "beta" -> "beta"
                else -> "stable"
            },
            updateNotify = saved[UN] ?: true,
            updateAuto = saved[UA] ?: true,
            updateCheckTime = normTime(saved[UCT], "09:00"),
            lastUpdateCheck = saved[LUC],
            lastUpdateCheckAt = saved[LUCA] ?: 0L,
            lastLiveCheck = saved[LLC] ?: 0L,
            availableUpdate = saved[AU] ?: 0,
            lastNotifiedUpdate = saved[LNU] ?: 0,
            updateExpandedOnce = saved[UEO] ?: false,
            backup = BackupState(
                on = saved[BON] ?: false,
                days = normDays(saved[BDAYS]),
                dest = saved[BDEST],
                dirs = saved[BDIRS].orEmpty(),
                saved = saved[BSAVED] ?: true,
                cabinet = saved[BCAB] ?: false,
                cabinetFolders = saved[BCFOLD].orEmpty().mapNotNull(CabinetFolder::decode).sortedBy { it.path.lowercase() },
                lastAt = saved[BLAT] ?: 0L,
                last = saved[BLAST],
                cabinetAt = saved[BCAT] ?: 0L,
                cabinetLast = saved[BCLAST],
            ),
        )
    }

    suspend fun ensureUpdateDefaults() = context.ds.edit { saved ->
        // Источник «Сервер разработчика» убран в 2.1.1, остаётся только GitHub
        val oldSource = stringPreferencesKey("updateSource")
        val hadSource = saved.contains(oldSource)
        if (hadSource) saved.remove(oldSource)
        if (saved[UC] != null || hadSource) return@edit
        val legacy = saved.asMap().isNotEmpty()
        if (legacy) {
            saved[UC] = "beta"
        } else {
            saved[UC] = "stable"
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
    suspend fun setShowTomorrow(v: Boolean) = context.ds.edit { it[ST] = v }
    suspend fun setLiveWatch(v: Boolean) = context.ds.edit { it[LW] = v }
    suspend fun setWidgetNumber(v: Boolean) = context.ds.edit { it[WN] = v }
    suspend fun setWidgetShowTime(v: Boolean) = context.ds.edit { it[WST] = v }
    suspend fun setWidgetRoom(v: Boolean) = context.ds.edit { it[WR] = v }
    suspend fun setWidgetTime(v: String) = context.ds.edit { it[WT] = if (v == "start") "start" else "full" }
    suspend fun setWidgetPlace(v: String) = context.ds.edit { it[WP] = if (v == "inline") "inline" else "above" }
    suspend fun setWidgetHeader(v: Boolean) = context.ds.edit { it[WH] = v }
    suspend fun setWidgetFullDate(v: Boolean) = context.ds.edit { it[WF] = v }
    suspend fun ensureWidgetOpen() {
        context.ds.edit { saved ->
            if (saved[WO] != null) return@edit
            val existing = saved[G] != null || saved[T] != null || saved[S] != null
            saved[WO] = if (existing) "Today" else "Cabinet"
        }
    }

    suspend fun setWidgetOpen(v: String) = context.ds.edit { it[WO] = normTab(v) }
    suspend fun setWidgetAlpha(v: Int) = context.ds.edit { it[WA] = v.coerceIn(10, 100) }
    suspend fun setUpdateChannel(v: String) = context.ds.edit { it[UC] = if (v == "stable") "stable" else "beta" }
    suspend fun setUpdateNotify(v: Boolean) = context.ds.edit { it[UN] = v }
    suspend fun setUpdateAuto(v: Boolean) = context.ds.edit { it[UA] = v }
    suspend fun setUpdateCheckTime(v: String) = context.ds.edit { it[UCT] = normTime(v, "09:00") }
    suspend fun setLastUpdateCheck(v: String) = context.ds.edit { it[LUC] = v }
    suspend fun setLastUpdateCheckAt(v: Long) = context.ds.edit { it[LUCA] = v }
    suspend fun setLastLiveCheck(v: Long) = context.ds.edit { it[LLC] = v }
    suspend fun setAvailableUpdate(v: Int) = context.ds.edit { it[AU] = v }
    suspend fun setLastNotifiedUpdate(v: Int) = context.ds.edit { it[LNU] = v }
    suspend fun setUpdateExpandedOnce() = context.ds.edit { it[UEO] = true }

    suspend fun setBackupOn(v: Boolean) = context.ds.edit { it[BON] = v }
    suspend fun setBackupDays(v: Int) = context.ds.edit { it[BDAYS] = normDays(v) }
    suspend fun setBackupDest(v: String?) = context.ds.edit {
        if (v == null) it.remove(BDEST) else it[BDEST] = v
        it.remove(BPRINT)
        it.remove(BCPRINT)
    }
    suspend fun addBackupDir(v: String) = context.ds.edit { it[BDIRS] = it[BDIRS].orEmpty() + v; it.remove(BPRINT) }
    suspend fun removeBackupDir(v: String) = context.ds.edit { it[BDIRS] = it[BDIRS].orEmpty() - v; it.remove(BPRINT) }
    suspend fun setBackupSaved(v: Boolean) = context.ds.edit { it[BSAVED] = v; it.remove(BPRINT) }
    suspend fun setBackupCabinet(v: Boolean) = context.ds.edit { it[BCAB] = v }
    suspend fun setCabinetFolders(v: List<CabinetFolder>) = context.ds.edit {
        it[BCFOLD] = v.map(CabinetFolder::encode).toSet()
        it.remove(BCPRINT)
    }

    /** Убирает из выбора папки, которых больше нет на сервере */
    suspend fun dropCabinetFolders(gone: Collection<String>) = context.ds.edit { saved ->
        val left = saved[BCFOLD].orEmpty().filter { raw -> CabinetFolder.decode(raw)?.url !in gone }.toSet()
        saved[BCFOLD] = left
    }

    /** Отпечаток содержимого прошлого архива: если ничего не изменилось, новый не создаётся */
    suspend fun backupPrint(cabinet: Boolean): String? = context.ds.data.first()[if (cabinet) BCPRINT else BPRINT]

    /** После удаления всех архивов: итоги и отпечатки сброшены, следующий запуск создаст архив заново */
    suspend fun clearBackupResults() = context.ds.edit {
        listOf(BLAT, BCAT).forEach { key -> it.remove(key) }
        listOf(BLAST, BPRINT, BCLAST, BCPRINT).forEach { key -> it.remove(key) }
    }

    suspend fun setBackupResult(cabinet: Boolean, at: Long, summary: String, print: String?) = context.ds.edit {
        if (cabinet) {
            it[BCAT] = at
            it[BCLAST] = summary
            if (print != null) it[BCPRINT] = print
        } else {
            it[BLAT] = at
            it[BLAST] = summary
            if (print != null) it[BPRINT] = print
        }
    }

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

private fun normDays(raw: Int?): Int = when (raw) {
    1, 30 -> raw
    else -> 7
}

private fun normTab(raw: String?): String = when (raw) {
    "Week", "Bells", "Cabinet", "More" -> raw
    else -> "Today"
}

private fun normTime(raw: String?, fallback: String): String {
    val src = raw ?: fallback
    val p = src.trim().split(":")
    val h = p.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: fallback.substringBefore(":").toIntOrNull() ?: 7
    val m = p.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
}
