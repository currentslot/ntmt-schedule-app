package ntmt.schedule.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.first
import ntmt.schedule.data.BackupState
import ntmt.schedule.data.CabinetApi
import ntmt.schedule.data.Prefs
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Резервные копии учебных файлов в zip.
 *
 * Обычный архив (NTMT-backup-…) — скачанные из кабинета файлы по папкам сервера,
 * собирается по расписанию WorkManager. Архив кабинета (NTMT-cabinet-…) — выбранные папки
 * кабинета на сайте; для него нужна сессия кабинета, поэтому он собирается после входа,
 * когда подошёл срок, или по кнопке. Если с прошлого раза ничего не поменялось, новый архив не создаётся,
 * а после удачного нового архива предыдущие того же вида удаляются.
 */
object Backup {
    private const val PERIODIC = "ntmt-backup"
    const val NOW = "ntmt-backup-now"
    const val KEY_MANUAL = "manual"
    const val KEY_CABINET = "cabinet"

    private const val MAX_FILES = 5000
    private const val MAX_CABINET_FILE = 200L * 1024 * 1024
    private const val MAX_CABINET_TOTAL = 2L * 1024 * 1024 * 1024
    private const val PREFIX_LOCAL = "NTMT-backup-"
    private const val PREFIX_CABINET = "NTMT-cabinet-"

    fun schedule(ctx: Context, st: BackupState) {
        val wm = WorkManager.getInstance(ctx)
        if (!st.on || st.dest == null) {
            wm.cancelUniqueWork(PERIODIC)
            return
        }
        // Папкам кабинета нужна сеть, иначе фоновая копия ждёт подключения
        val online = st.cabinet && st.cabinetFolders.isNotEmpty()
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .setRequiredNetworkType(if (online) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED)
            .build()
        val req = PeriodicWorkRequestBuilder<BackupWorker>(st.days.toLong(), TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()
        wm.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    /** Разовый запуск: кнопка «Сделать сейчас» или вход в кабинет, когда подошёл срок */
    fun runNow(ctx: Context, cabinet: Boolean) {
        val req = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInputData(workDataOf(KEY_MANUAL to true, KEY_CABINET to cabinet))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(if (cabinet) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(NOW, ExistingWorkPolicy.KEEP, req)
    }

    class Outcome(
        val summary: String,
        val print: String?,
        val failed: Boolean,
        val gone: List<String> = emptyList(),
        val cancelled: Boolean = false,
    )

    private class Item(val path: String, val size: Long, val stamp: Long, val open: () -> InputStream?)

    fun destination(ctx: Context, st: BackupState): DocumentFile? {
        val raw = st.dest ?: return null
        val dir = runCatching { DocumentFile.fromTreeUri(ctx, Uri.parse(raw)) }.getOrNull() ?: return null
        return dir.takeIf { it.exists() && it.isDirectory && it.canWrite() }
    }

    fun folderName(ctx: Context, raw: String): String =
        runCatching { DocumentFile.fromTreeUri(ctx, Uri.parse(raw))?.name }.getOrNull()
            ?: Uri.parse(raw).lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')?.ifBlank { null }
            ?: "Папка"

    /**
     * Архив скачанных из кабинета файлов. Каждый файл кладётся в ту папку, из которой
     * он был скачан на сервере ([ShareOrigin]); полученные по QR/NFC — в «Без папки»
     */
    fun runLocal(ctx: Context, st: BackupState, previous: String?, force: Boolean, stopped: () -> Boolean = { false }): Outcome {
        val dest = destination(ctx, st) ?: return Outcome("Нет доступа к папке для архивов. Выберите её заново", null, true)
        if (!st.saved) return Outcome("Скачанные файлы не сохраняются", null, false)
        val items = mutableListOf<Item>()
        val origin = ShareOrigin.all(ctx)
        val taken = mutableMapOf<String, MutableSet<String>>()
        ntmt.schedule.data.Downloads.list(ctx).sortedBy { it.name }.forEach { file ->
            if (items.size >= MAX_FILES) return@forEach
            val where = origin[file.name]?.map(::clean)?.filter { it.isNotBlank() }
            val dir = if (where.isNullOrEmpty()) "Скачанные/Без папки" else "Скачанные/Мои файлы/" + where.joinToString("/")
            val name = unique(taken.getOrPut(dir.lowercase()) { mutableSetOf() }, clean(file.name))
            items += Item("$dir/$name", file.size, file.modified) { ntmt.schedule.data.Downloads.input(ctx, file) }
        }
        if (items.isEmpty()) return Outcome("Нет скачанных файлов", null, false)
        val print = fingerprint(items.map { "${it.path}|${it.size}|${it.stamp}" })
        if (!force && print == previous) return Outcome("${stamp(System.currentTimeMillis())} · без изменений", print, false)
        return write(ctx, dest, PREFIX_LOCAL, withDirs(items, emptyList()), print, "", stopped)
    }

    /** Удаляет все архивы NTMT из папки для архивов. Возвращает число удалённых */
    fun clearAll(ctx: Context, st: BackupState): Int {
        val dest = destination(ctx, st) ?: return 0
        var n = 0
        dest.listFiles()
            .filter { it.isFile && it.name.orEmpty().endsWith(".zip") && (it.name.orEmpty().startsWith(PREFIX_LOCAL) || it.name.orEmpty().startsWith(PREFIX_CABINET)) }
            .forEach { if (runCatching { it.delete() }.getOrDefault(false)) n++ }
        return n
    }

    /** Отдельные записи для папок, чтобы архив повторял дерево сервера, включая пустые папки */
    private fun withDirs(items: List<Item>, folders: List<String>): List<Item> {
        val dirs = sortedSetOf<String>()
        fun addParents(path: String, self: Boolean) {
            val parts = path.split('/')
            val upto = if (self) parts.size else parts.size - 1
            for (i in 1..upto) dirs += parts.take(i).joinToString("/") + "/"
        }
        items.forEach { addParents(it.path, self = false) }
        folders.forEach { addParents(it, self = true) }
        return dirs.map { Item(it, 0L, 0L) { null } } + items
    }

    /** Архив выбранных папок кабинета. Нужна действующая сессия; исчезнувшие с сервера папки возвращаются в [Outcome.gone] */
    fun runCabinet(ctx: Context, st: BackupState, previous: String?, force: Boolean, stopped: () -> Boolean = { false }): Outcome {
        val dest = destination(ctx, st) ?: return Outcome("Нет доступа к папке для архивов. Выберите её заново", null, true)
        if (st.cabinetFolders.isEmpty()) return Outcome("Не выбраны папки кабинета", null, false)
        val login = BackupVault.read(ctx)
            ?: return Outcome("Откройте «Выбрать папки кабинета» и подтвердите вход, чтобы копия работала в фоне", null, true)
        // Своя сессия: экран «Кабинет» её не видит и по-прежнему требует отпечаток
        return CabinetApi.isolated {
            try {
                CabinetApi.login(login.first, login.second)
            } catch (e: IllegalStateException) {
                return@isolated Outcome("Вход для копии не удался: ${e.message}. Подтвердите выбор папок ещё раз", null, true)
            }
            try {
                cabinetBody(ctx, st, dest, previous, force, stopped)
            } finally {
                runCatching { CabinetApi.logout() }
            }
        }
    }

    private fun cabinetBody(
        ctx: Context,
        st: BackupState,
        dest: DocumentFile,
        previous: String?,
        force: Boolean,
        stopped: () -> Boolean,
    ): Outcome {
        val listing = mutableListOf<Pair<String, ntmt.schedule.data.ShareEntry>>()
        val folders = mutableListOf<String>()
        val gone = mutableListOf<ntmt.schedule.data.CabinetFolder>()
        val seen = mutableSetOf<String>()
        val tops = mutableSetOf<String>()
        try {
            for (folder in st.cabinetFolders) {
                if (stopped()) return Outcome("${stamp(System.currentTimeMillis())} · отменено", null, false, gone.map { it.url }, cancelled = true)
                if (!exists(folder)) {
                    gone += folder
                    continue
                }
                val top = folder.path.split('/').filter { it.isNotBlank() }.joinToString("/") { clean(it) }.ifBlank { "Папка" }
                crawl(folder.url, "Мои файлы/${unique(tops, top)}", 0, seen, listing, folders, stopped)
            }
        } catch (e: IllegalStateException) {
            return Outcome(e.message ?: "Кабинет не ответил", null, true, gone.map { it.url })
        }
        val note = if (gone.isEmpty()) "" else " · убраны отсутствующие на сервере: ${gone.joinToString { it.path }}"
        val goneUrls = gone.map { it.url }
        if (listing.isEmpty()) return Outcome("${stamp(System.currentTimeMillis())} · в выбранных папках нет файлов$note", null, false, goneUrls)
        val print = fingerprint(listing.map { (path, e) -> "$path|${e.url}|${e.detail}" } + folders)
        if (!force && print == previous) return Outcome("${stamp(System.currentTimeMillis())} · без изменений$note", print, false, goneUrls)
        val temp = File(ctx.cacheDir, "backup-cabinet").apply { deleteRecursively(); mkdirs() }
        var skipped = 0
        var total = 0L
        val files = listing.map { (path, entry) ->
            Item(path, -1L, 0L) {
                temp.listFiles()?.forEach { it.delete() }
                val hit = if (total > MAX_CABINET_TOTAL) null else runCatching { CabinetApi.open(ctx, entry.url, entry.name, into = temp) }.getOrNull()
                val file = (hit as? ntmt.schedule.data.ShareHit.Saved)?.file
                if (file == null || file.length() > MAX_CABINET_FILE) {
                    skipped++
                    file?.delete()
                    null
                } else {
                    total += file.length()
                    file.inputStream()
                }
            }
        }
        return try {
            val res = write(ctx, dest, PREFIX_CABINET, withDirs(files, folders), print, note, stopped) { skipped }
            Outcome(res.summary, res.print, res.failed, goneUrls, res.cancelled)
        } finally {
            temp.deleteRecursively()
        }
    }

    /**
     * Есть ли папка на сервере: ищем её в списке родительской папки. Ошибка сессии
     * пробрасывается дальше, чтобы из-за неё не выкинуть из выбора все папки
     */
    private fun exists(folder: ntmt.schedule.data.CabinetFolder): Boolean {
        val parent = try {
            CabinetApi.files(folder.parent.ifBlank { CabinetApi.SHARE_ROOT })
        } catch (e: IllegalStateException) {
            if (e.message?.contains("Сессия") == true || e.message == "Нет сети") throw e
            // Родитель не открылся: считаем папку удалённой, только если сам кабинет отвечает
            CabinetApi.files(CabinetApi.SHARE_ROOT)
            return false
        }
        val want = folder.url.trimEnd('/')
        val name = folder.path.substringAfterLast('/')
        return parent.entries.any { it.folder && (it.url.trimEnd('/') == want || it.name.equals(name, true)) }
    }

    private fun crawl(
        url: String,
        path: String,
        depth: Int,
        seen: MutableSet<String>,
        out: MutableList<Pair<String, ntmt.schedule.data.ShareEntry>>,
        folders: MutableList<String>,
        stopped: () -> Boolean,
    ) {
        if (stopped() || depth > 8 || out.size >= MAX_FILES || !seen.add(url.trimEnd('/'))) return
        folders += path
        val page = CabinetApi.files(url)
        val names = mutableSetOf<String>()
        for (entry in page.entries) {
            if (!CabinetApi.inShare(entry.url)) continue
            val name = unique(names, clean(entry.name))
            if (entry.folder) {
                crawl(entry.url, "$path/$name", depth + 1, seen, out, folders, stopped)
            } else if (out.size < MAX_FILES) {
                out += "$path/$name" to entry
            }
        }
    }

    private fun write(
        ctx: Context,
        dest: DocumentFile,
        prefix: String,
        items: List<Item>,
        print: String,
        note: String,
        stopped: () -> Boolean,
        skippedLater: () -> Int = { 0 },
    ): Outcome {
        if (stopped()) return Outcome("${stamp(System.currentTimeMillis())} · отменено", null, false, cancelled = true)
        val now = System.currentTimeMillis()
        val name = prefix + LocalDateTime.now().format(FILE_TIME) + ".zip"
        val target = dest.createFile("application/zip", name) ?: return Outcome("Не удалось создать архив в выбранной папке", null, true)
        var count = 0
        var skipped = 0
        try {
            val stream = ctx.contentResolver.openOutputStream(target.uri) ?: error("Не удалось открыть архив")
            ZipOutputStream(stream.buffered(64 * 1024)).use { zip ->
                for (item in items) {
                    if (stopped()) throw Stop()
                    if (item.path.endsWith("/")) {
                        zip.putNextEntry(ZipEntry(item.path))
                        zip.closeEntry()
                        continue
                    }
                    val input = runCatching { item.open() }.getOrNull()
                    if (input == null) {
                        skipped++
                        continue
                    }
                    input.use {
                        zip.putNextEntry(ZipEntry(item.path).apply { if (item.stamp > 0) time = item.stamp })
                        it.copyTo(zip, 64 * 1024)
                        zip.closeEntry()
                    }
                    count++
                }
            }
        } catch (_: Stop) {
            // Отмена: недописанный архив удаляется, прежние остаются
            runCatching { target.delete() }
            return Outcome("${stamp(now)} · отменено", null, false, cancelled = true)
        } catch (e: Exception) {
            runCatching { target.delete() }
            return Outcome("Ошибка записи архива: ${e.message ?: e.javaClass.simpleName}", null, true)
        }
        if (count == 0) {
            runCatching { target.delete() }
            return Outcome("Не удалось прочитать ни одного файла", null, true)
        }
        prune(dest, prefix, target)
        val size = target.length()
        val missed = maxOf(skipped, skippedLater())
        val tail = if (missed > 0) " · пропущено: $missed" else ""
        return Outcome("${stamp(now)} · ${files(count)} · ${mb(size)}$tail$note", print, false)
    }

    /** Хранится только последний архив каждого вида: предыдущие, в том числе ручные, удаляются после удачной записи */
    private fun prune(dest: DocumentFile, prefix: String, fresh: DocumentFile) {
        dest.listFiles()
            .filter { it.isFile && it.uri != fresh.uri && it.name.orEmpty().startsWith(prefix) && it.name.orEmpty().endsWith(".zip") }
            .forEach { runCatching { it.delete() } }
    }

    private fun fingerprint(lines: List<String>): String {
        val md = MessageDigest.getInstance("SHA-256")
        lines.sorted().forEach { md.update(it.toByteArray(Charsets.UTF_8)); md.update(10) }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun clean(raw: String): String =
        raw.replace(Regex("""[\\/:*?"<>|\u0000-\u001f]"""), "_").trim().trim('.').ifBlank { "_" }.take(150)

    private fun unique(taken: MutableSet<String>, name: String): String {
        if (taken.add(name.lowercase())) return name
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var n = 2
        while (!taken.add("$base ($n)$ext".lowercase())) n++
        return "$base ($n)$ext"
    }

    private class Stop : RuntimeException()

    private val FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm", Locale.ROOT)
    private val SHOW_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ROOT)

    private fun stamp(ms: Long) = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), java.time.ZoneId.systemDefault()).format(SHOW_TIME)

    private fun mb(bytes: Long): String =
        if (bytes < 1024 * 1024) "${(bytes / 1024).coerceAtLeast(1)} КБ" else String.format(Locale("ru"), "%.1f МБ", bytes / 1048576.0)

    private fun files(n: Int): String {
        val word = when {
            n % 100 in 11..14 -> "файлов"
            n % 10 == 1 -> "файл"
            n % 10 in 2..4 -> "файла"
            else -> "файлов"
        }
        return "$n $word"
    }
}
