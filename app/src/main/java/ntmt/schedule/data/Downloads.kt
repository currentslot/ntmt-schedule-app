package ntmt.schedule.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import ntmt.schedule.backup.ShareOrigin
import java.io.File
import java.io.InputStream

/** Скачанный файл: лежит либо в кэше приложения ([file]), либо в выбранной папке ([doc]) */
class SavedFile(val name: String, val size: Long, val modified: Long, val file: File?, val doc: Uri?)

/**
 * Где хранятся «Скачанные». Если в настройках выбрана папка, файлы сохраняются туда,
 * иначе в кэш приложения. Из выбранной папки показываются только файлы, сохранённые приложением
 */
object Downloads {
    private const val PREFS = "downloads"
    private const val KEY_DIR = "dir"
    private const val KEY_NAMES = "names"
    private const val PART = ".part"
    private val lock = Any()

    private fun sp(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun cacheDir(ctx: Context): File = File(ctx.cacheDir, "share").apply { mkdirs() }

    /** Сюда пишется файл, пока он скачивается или принимается */
    fun incoming(ctx: Context): File = File(ctx.cacheDir, "incoming").apply { mkdirs() }

    fun folderUri(ctx: Context): String? = sp(ctx).getString(KEY_DIR, null)

    /** Выбранная папка, если к ней ещё есть доступ */
    fun folder(ctx: Context): DocumentFile? {
        val raw = folderUri(ctx) ?: return null
        val dir = runCatching { DocumentFile.fromTreeUri(ctx, Uri.parse(raw)) }.getOrNull() ?: return null
        return dir.takeIf { runCatching { it.exists() && it.isDirectory && it.canWrite() }.getOrDefault(false) }
    }

    fun folderName(ctx: Context): String? {
        val raw = folderUri(ctx) ?: return null
        return runCatching { DocumentFile.fromTreeUri(ctx, Uri.parse(raw))?.name }.getOrNull()
            ?: Uri.parse(raw).lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')?.ifBlank { null }
            ?: "Папка"
    }

    fun setFolder(ctx: Context, uri: String?) = synchronized(lock) {
        val old = folderUri(ctx)
        sp(ctx).edit().apply {
            if (uri == null) remove(KEY_DIR) else putString(KEY_DIR, uri)
            if (uri != old) remove(KEY_NAMES)
        }.apply()
    }

    private fun names(ctx: Context): MutableSet<String> = sp(ctx).getStringSet(KEY_NAMES, emptySet()).orEmpty().toMutableSet()

    private fun saveNames(ctx: Context, names: Set<String>) {
        sp(ctx).edit().putStringSet(KEY_NAMES, names.toSet()).apply()
    }

    fun list(ctx: Context): List<SavedFile> {
        val out = mutableListOf<SavedFile>()
        cacheDir(ctx).listFiles()?.filter { it.isFile && !it.name.endsWith(PART) }?.forEach { f ->
            out += SavedFile(f.name, f.length(), f.lastModified(), f, null)
        }
        val dir = folder(ctx)
        if (dir != null) {
            val ours = names(ctx)
            if (ours.isNotEmpty()) {
                val found = runCatching { dir.listFiles().toList() }.getOrDefault(emptyList())
                    .filter { it.isFile && it.name in ours }
                found.forEach { d -> out += SavedFile(d.name.orEmpty(), d.length(), d.lastModified(), null, d.uri) }
                val alive = found.mapNotNull { it.name }.toSet()
                // Файлы, удалённые из папки вручную, забываются
                if (alive.size != ours.size) synchronized(lock) { saveNames(ctx, names(ctx).filter { it in alive }.toSet()) }
            }
        }
        return out.sortedByDescending { it.modified }
    }

    fun allNames(ctx: Context): Set<String> = list(ctx).map { it.name }.toSet()

    /**
     * Уже скачанный файл из той же папки сервера: имя совпадает (или «имя (2).ext»), а
     * запомненная папка та же. Для файлов без записи о папке достаточно совпадения имени
     */
    fun findSame(ctx: Context, name: String, where: List<String>): SavedFile? {
        val clean = CabinetApi.safeName(name)
        if (clean.isBlank()) return null
        val dot = clean.lastIndexOf('.')
        val base = if (dot > 0) clean.substring(0, dot) else clean
        val ext = if (dot > 0) clean.substring(dot) else ""
        val numbered = Regex("^" + Regex.escape(base) + """ \(\d+\)""" + Regex.escape(ext) + "$", RegexOption.IGNORE_CASE)
        val origin = ShareOrigin.all(ctx)
        return list(ctx).firstOrNull { f ->
            if (f.size <= 0L) return@firstOrNull false
            val known = origin[f.name]
            when {
                f.name.equals(clean, true) -> known == null || known == where
                numbered.matches(f.name) -> known == where
                else -> false
            }
        }
    }

    /**
     * Переносит готовый [temp] в «Скачанные». Совпадение имени с файлом из другой папки сервера
     * (или с полученным по QR/NFC) даёт имя «имя (2).ext», а тот же файл из той же папки заменяется
     */
    fun store(ctx: Context, temp: File, origin: List<String>?): SavedFile = synchronized(lock) {
        val wanted = temp.name
        val existing = list(ctx)
        val known = ShareOrigin.all(ctx)
        val same = existing.firstOrNull { it.name.equals(wanted, true) }
        val replace = same != null && origin != null && known[same.name] == origin
        val finalName = if (same == null || replace) wanted else unique(existing.map { it.name.lowercase() }.toSet(), wanted)
        if (replace && same != null) remove(ctx, same)
        val dir = folder(ctx)
        val saved = if (dir != null) {
            runCatching { toFolder(ctx, dir, temp, finalName) }.getOrNull() ?: toCache(ctx, temp, finalName)
        } else {
            toCache(ctx, temp, finalName)
        }
        if (origin != null) ShareOrigin.remember(ctx, saved.name, origin)
        saved
    }

    private fun toFolder(ctx: Context, dir: DocumentFile, temp: File, name: String): SavedFile {
        dir.findFile(name)?.takeIf { it.isFile }?.delete()
        val doc = dir.createFile("application/octet-stream", name) ?: error("Нет доступа к папке")
        try {
            val out = ctx.contentResolver.openOutputStream(doc.uri, "w") ?: error("Нет доступа к папке")
            out.use { o -> temp.inputStream().use { it.copyTo(o, 64 * 1024) } }
            if (doc.length() != temp.length()) error("Файл записан не полностью")
        } catch (e: Exception) {
            runCatching { doc.delete() }
            throw e
        }
        val real = doc.name ?: name
        names(ctx).also { it += real; saveNames(ctx, it) }
        temp.delete()
        return SavedFile(real, doc.length(), doc.lastModified(), null, doc.uri)
    }

    private fun toCache(ctx: Context, temp: File, name: String): SavedFile {
        val target = File(cacheDir(ctx), name)
        if (target.exists()) target.delete()
        if (!temp.renameTo(target)) {
            try {
                temp.copyTo(target, overwrite = true)
            } catch (e: Exception) {
                target.delete()
                throw IllegalStateException(CabinetApi.saveError(e))
            }
            temp.delete()
        }
        return SavedFile(target.name, target.length(), target.lastModified(), target, null)
    }

    private fun unique(taken: Set<String>, name: String): String {
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var n = 2
        while ("$base ($n)$ext".lowercase() in taken && n < 1000) n++
        return "$base ($n)$ext"
    }

    fun remove(ctx: Context, f: SavedFile): Boolean = synchronized(lock) {
        val ok = when {
            f.file != null -> f.file.delete()
            f.doc != null -> runCatching { DocumentFile.fromSingleUri(ctx, f.doc)?.delete() == true }.getOrDefault(false)
            else -> false
        }
        if (f.doc != null) names(ctx).also { it -= f.name; saveNames(ctx, it) }
        ok
    }

    fun input(ctx: Context, f: SavedFile): InputStream? = when {
        f.file != null -> runCatching { f.file.inputStream() }.getOrNull()
        f.doc != null -> runCatching { ctx.contentResolver.openInputStream(f.doc) }.getOrNull()
        else -> null
    }

    /** Ссылка для открытия или отправки в другое приложение */
    fun shareUri(ctx: Context, f: SavedFile): Uri =
        f.doc ?: FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f.file!!)

    /** Обычный файл для передачи по QR/NFC: файл из папки копируется во временный */
    fun localCopy(ctx: Context, f: SavedFile): File? {
        if (f.file != null) return f.file
        val dir = File(ctx.cacheDir, "send").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, f.name)
        return runCatching {
            input(ctx, f)!!.use { i -> out.outputStream().use { i.copyTo(it) } }
            out
        }.getOrElse {
            out.delete()
            null
        }
    }

    /** После выбора папки переносит в неё файлы из кэша. Возвращает число перенесённых */
    fun moveCacheToFolder(ctx: Context): Int = synchronized(lock) {
        val dir = folder(ctx) ?: return 0
        val taken = runCatching { dir.listFiles().mapNotNull { it.name?.lowercase() }.toMutableSet() }.getOrDefault(mutableSetOf())
        val origin = ShareOrigin.all(ctx)
        var moved = 0
        cacheDir(ctx).listFiles()?.filter { it.isFile && !it.name.endsWith(PART) }?.forEach { f ->
            val name = if (f.name.lowercase() in taken) unique(taken, f.name) else f.name
            val ok = runCatching {
                val doc = dir.createFile("application/octet-stream", name) ?: error("no access")
                try {
                    ctx.contentResolver.openOutputStream(doc.uri, "w")!!.use { o -> f.inputStream().use { it.copyTo(o, 64 * 1024) } }
                    if (doc.length() != f.length()) error("short")
                } catch (e: Exception) {
                    runCatching { doc.delete() }
                    throw e
                }
                doc.name ?: name
            }.getOrNull()
            if (ok != null) {
                taken += ok.lowercase()
                names(ctx).also { it += ok; saveNames(ctx, it) }
                origin[f.name]?.let { ShareOrigin.remember(ctx, ok, it) }
                f.delete()
                moved++
            }
        }
        moved
    }
}
