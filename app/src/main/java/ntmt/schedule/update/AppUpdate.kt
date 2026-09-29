package ntmt.schedule.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class AppRelease(
    val versionCode: Int,
    val versionName: String,
    val channel: String,
    val notes: String,
    val size: Long,
    val sha256: String,
    val url: String,
)

data class UpdateCheck(
    val release: AppRelease?,
    val newer: Boolean,
    val error: String?,
)

object AppUpdate {
    const val HOST = "https://currentslot.mikata.ru"

    /**
     * Репозиторий GitHub в виде owner/name. Пока пусто — приложение не обращается к GitHub.
     * Релиз публикуется с ветки stable или beta.
     * Имя файла: NTMT-название-versionCode.apk
     */
    const val GITHUB_REPO = "currentslot/ntmt-schedule-app"

    const val SOURCE_GITHUB = "github"
    const val SOURCE_SERVER = "server"
    const val ERR_GITHUB_EMPTY = "github-empty"

    private const val ZEALOT_STABLE = "333a40164e980bdc43d21fecf39bbf67"
    private const val ZEALOT_BETA = "22cbe109a484fa2214888daeac4a341b"
    private const val PACKAGE_ID = "ntmt.schedule"

    fun installedCode(ctx: Context): Int {
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        return code.toInt()
    }

    fun installedName(ctx: Context): String {
        return ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "—"
    }

    fun check(source: String, channel: String, versionCode: Int): UpdateCheck {
        return when (source) {
            SOURCE_GITHUB -> checkGithub(channel, versionCode)
            else -> checkZealot(channel, versionCode)
        }
    }

    fun downloadedFile(ctx: Context): File? {
        val file = File(ctx.cacheDir, "updates/ntmt-update.apk")
        return file.takeIf { it.isFile && it.length() > 0L }
    }

    fun archiveCode(ctx: Context, file: File): Int {
        @Suppress("DEPRECATION")
        val info = ctx.packageManager.getPackageArchiveInfo(file.absolutePath, 0) ?: return 0
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode.toInt() else info.versionCode
    }

    fun archiveName(ctx: Context, file: File): String {
        @Suppress("DEPRECATION")
        val info = ctx.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
        return info?.versionName?.takeIf { it.isNotBlank() } ?: file.name
    }

    fun deleteDownloaded(ctx: Context) {
        File(ctx.cacheDir, "updates").listFiles()?.forEach { it.delete() }
    }

    fun sweepInstalled(ctx: Context): File? {
        val file = downloadedFile(ctx) ?: return null
        val code = archiveCode(ctx, file)
        if (code > 0 && code <= installedCode(ctx)) {
            deleteDownloaded(ctx)
            return null
        }
        return file
    }

    private fun checkServer(channel: String, versionCode: Int): UpdateCheck {
        val safe = if (channel == "stable") "stable" else "beta"
        val link = "$HOST/api/v1/update?package=$PACKAGE_ID&channel=$safe&versionCode=$versionCode"
        val conn = (URL(link).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 15000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) return UpdateCheck(null, false, "Не удалось проверить")
            val json = JSONObject(text)
            if (json.isNull("latest")) return UpdateCheck(null, false, null)
            val o = json.getJSONObject("latest")
            val release = AppRelease(
                versionCode = o.getInt("versionCode"),
                versionName = o.optString("versionName"),
                channel = o.optString("channel", safe),
                notes = plainNotes(o.optString("notes")),
                size = o.optLong("size"),
                sha256 = o.optString("sha256"),
                url = o.optString("url"),
            )
            if (!release.url.startsWith("https://")) return UpdateCheck(null, false, "Не удалось проверить")
            val newer = json.optBoolean("newer") || release.versionCode > versionCode
            return UpdateCheck(release, newer, null)
        } catch (_: Exception) {
            return UpdateCheck(null, false, "Не удалось проверить")
        } finally {
            conn.disconnect()
        }
    }

    private fun checkZealot(channel: String, versionCode: Int): UpdateCheck {
        val safe = if (channel == "stable") "stable" else "beta"
        return checkZealotChannel(safe, versionCode)
    }

    private fun checkZealotChannel(channel: String, versionCode: Int): UpdateCheck {
        val key = if (channel == "stable") ZEALOT_STABLE else ZEALOT_BETA
        val encoded = java.net.URLEncoder.encode(key, Charsets.UTF_8.name())
        val conn = open("$HOST/api/apps/versions?channel_key=$encoded")
        try {
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) return UpdateCheck(null, false, "Не удалось проверить")
            val items = releaseItems(JSONObject(text))
            var best: JSONObject? = null
            var bestCode = -1
            for (item in items) {
                val bundle = item.optString("bundle_id")
                if (bundle.isNotBlank() && bundle != PACKAGE_ID && bundle != "*") continue
                val build = item.optString("build_version").toIntOrNull() ?: continue
                if (item.optLong("size") in 1..999_999) continue
                val url = item.optString("install_url")
                if (!url.startsWith("https://")) continue
                if (build > bestCode) {
                    best = item
                    bestCode = build
                }
            }
            val item = best ?: return UpdateCheck(null, false, null)
            return UpdateCheck(
                AppRelease(
                    versionCode = bestCode,
                    versionName = item.optString("release_version").ifBlank { bestCode.toString() },
                    channel = channel,
                    notes = notesOf(item),
                    size = item.optLong("size"),
                    sha256 = "",
                    url = item.optString("install_url"),
                ),
                bestCode > versionCode,
                null,
            )
        } catch (_: Exception) {
            return UpdateCheck(null, false, "Не удалось проверить")
        } finally {
            conn.disconnect()
        }
    }

    private fun notesOf(item: JSONObject): String {
        val parts = mutableListOf<String>()
        fun add(raw: String?) {
            val text = plainNotes(raw.orEmpty())
            if (text.isNotBlank()) parts += text
        }
        when (val raw = item.opt("changelog")) {
            is JSONArray -> for (i in 0 until raw.length()) {
                when (val el = raw.opt(i)) {
                    is JSONObject -> add(el.optString("message").ifBlank { el.optString("body") }.ifBlank { el.optString("text") })
                    is String -> add(el)
                }
            }
            is JSONObject -> add(raw.optString("message").ifBlank { raw.optString("body") }.ifBlank { raw.optString("text") })
            is String -> add(raw)
        }
        add(item.optString("text_changelog"))
        add(item.optString("changelog_text"))
        add(item.optString("release_notes"))
        if (parts.isEmpty()) return ""
        val kept = mutableListOf<String>()
        for (part in parts.sortedByDescending { it.length }) {
            if (kept.any { longer -> longer.contains(part) }) continue
            kept += part
        }
        return kept.sortedBy { parts.indexOf(it) }.joinToString("\n")
    }

    private fun plainNotes(raw: String): String {
        if (raw.isBlank()) return ""
        var s = raw.replace("\r\n", "\n")
        s = s.replace(Regex("(?i)<br\\s*/?>"), "\n")
        s = s.replace(Regex("(?i)</p>"), "\n")
        s = s.replace(Regex("(?i)</li>"), "\n")
        s = s.replace(Regex("(?i)<li[^>]*>"), "• ")
        s = s.replace(Regex("<[^>]+>"), "")
        s = decodeEntities(s)
        s = s.replace(Regex("(?m)^#{1,6}\\s*"), "")
        s = s.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        s = s.replace(Regex("__(.+?)__"), "$1")
        return s.lines().joinToString("\n") { it.trimEnd() }.replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun decodeEntities(raw: String): String {
        val nbsp = "&" + "nbsp;"
        val amp = "&" + "amp;"
        val lt = "&" + "lt;"
        val gt = "&" + "gt;"
        val quot = "&" + "quot;"
        val apos = "&" + "#39;"
        return raw.replace(nbsp, " ")
            .replace(amp, "&")
            .replace(lt, "<")
            .replace(gt, ">")
            .replace(quot, "\"")
            .replace(apos, "'")
    }

    private fun releaseItems(root: JSONObject): List<JSONObject> {
        if (root.isNull("releases")) return emptyList()
        return when (val raw = root.opt("releases")) {
            is JSONArray -> (0 until raw.length()).map { raw.getJSONObject(it) }
            is JSONObject -> listOf(raw)
            else -> emptyList()
        }
    }

    private fun checkGithub(channel: String, versionCode: Int): UpdateCheck {
        val repo = GITHUB_REPO.trim().trim('/')
        if (!repo.contains("/")) return UpdateCheck(null, false, ERR_GITHUB_EMPTY)
        val safe = if (channel == "stable") "stable" else "beta"
        val conn = open("https://api.github.com/repos/$repo/releases?per_page=30").apply {
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) return UpdateCheck(null, false, "Не удалось проверить")
            val arr = JSONArray(text)
            var best: JSONObject? = null
            var bestCode = -1
            for (i in 0 until arr.length()) {
                val rel = arr.getJSONObject(i)
                if (rel.optBoolean("draft")) continue
                if (isBetaRelease(rel) != (safe == "beta")) continue
                val asset = newestApk(rel) ?: continue
                val parsed = parseCode(asset.optString("name"), rel.optString("name") + " " + rel.optString("tag_name"), rel.optString("body"))
                if (parsed <= 0) continue
                if (parsed > bestCode) {
                    best = rel
                    bestCode = parsed
                }
            }
            val release = best ?: return UpdateCheck(null, false, null)
            val asset = newestApk(release) ?: return UpdateCheck(null, false, null)
            val parsed = bestCode
            val url = asset.optString("browser_download_url")
            if (!url.startsWith("https://")) return UpdateCheck(null, false, "Не удалось проверить")
            return UpdateCheck(
                AppRelease(
                    versionCode = parsed,
                    versionName = release.optString("name").ifBlank { release.optString("tag_name") },
                    channel = safe,
                    notes = plainNotes(release.optString("body")),
                    size = asset.optLong("size"),
                    sha256 = "",
                    url = url,
                ),
                parsed > versionCode,
                null,
            )
        } catch (_: Exception) {
            return UpdateCheck(null, false, "Не удалось проверить")
        } finally {
            conn.disconnect()
        }
    }

    private fun isBetaRelease(rel: JSONObject): Boolean {
        if (rel.optBoolean("prerelease")) return true
        val label = (rel.optString("tag_name") + " " + rel.optString("name")).lowercase()
        return label.contains("β") || Regex("(?:^|[^a-z])beta(?:[^a-z]|$)").containsMatchIn(label)
    }

    private fun newestApk(rel: JSONObject): JSONObject? {
        val assets = rel.optJSONArray("assets") ?: return null
        var best: JSONObject? = null
        var bestCode = -1
        val title = rel.optString("name") + " " + rel.optString("tag_name")
        val body = rel.optString("body")
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name")
            if (!name.endsWith(".apk", true)) continue
            val code = parseCode(name, title, body)
            if (best == null || code > bestCode) {
                best = asset
                bestCode = code
            }
        }
        return best
    }

    private fun parseCode(name: String, title: String, body: String): Int {
        if (name.equals("NTMT-1.8.1-Stable.apk", true)) return 58
        Regex("\\((\\d{2,})\\)").find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
        Regex("(\\d+)\\.apk$", RegexOption.IGNORE_CASE).find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
        return Regex("versionCode\\s*[:=]\\s*(\\d+)", RegexOption.IGNORE_CASE).find(body)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }

    private fun open(link: String): HttpURLConnection {
        return (URL(link).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "NTMT-Android")
            setRequestProperty("Accept", "application/json")
        }
    }

    @Volatile
    private var activeDownload: HttpURLConnection? = null

    fun abortDownload() {
        activeDownload?.disconnect()
        activeDownload = null
    }

    fun download(release: AppRelease, dest: File, onProgress: (Int) -> Unit): File {
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, dest.name + ".part")
        val conn = open(release.url).apply {
            readTimeout = 60000
            setRequestProperty("Accept", "application/octet-stream")
        }
        activeDownload = conn
        try {
            if (conn.responseCode !in 200..299) error("Не удалось скачать (${conn.responseCode})")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.size
            conn.inputStream.use { input ->
                FileOutputStream(tmp).use { out ->
                    val buf = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        if (activeDownload == null) error("Загрузка отменена")
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (total > 0) onProgress(((read * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            if (activeDownload == null) error("Загрузка отменена")
            if (release.sha256.length == 64) {
                val sum = sha256(tmp)
                if (!sum.equals(release.sha256, true)) {
                    tmp.delete()
                    error("Контрольная сумма не совпала")
                }
            }
            if (dest.exists()) dest.delete()
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
            onProgress(100)
            return dest
        } catch (e: Exception) {
            tmp.delete()
            throw e
        } finally {
            if (activeDownload == conn) activeDownload = null
            conn.disconnect()
        }
    }

    fun canInstall(ctx: Context): Boolean {
        return Build.VERSION.SDK_INT < 26 || ctx.packageManager.canRequestPackageInstalls()
    }

    fun openInstallPermission(ctx: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${ctx.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    fun install(ctx: Context, file: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
