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
    /**
     * Репозиторий GitHub в виде owner/name, откуда берутся релизы.
     * Если строку очистить, приложение не обращается к GitHub. Релиз публикуется с ветки stable или beta.
     * Имя файла: NTMT-название-versionCode.apk
     */
    const val GITHUB_REPO = "currentslot/ntmt-schedule-app"

    const val ERR_GITHUB_EMPTY = "github-empty"

    fun installedCode(ctx: Context): Int {
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        return code.toInt()
    }

    fun installedName(ctx: Context): String {
        return ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "—"
    }

    fun check(channel: String, versionCode: Int): UpdateCheck = checkGithub(channel, versionCode)

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
                    sha256 = asset.optString("digest").removePrefix("sha256:").takeIf { it.length == 64 }.orEmpty(),
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
            setRequestProperty("User-Agent", ntmt.schedule.data.NtmtApi.USER_AGENT)
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
