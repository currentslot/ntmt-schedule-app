package ntmt.schedule.data

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class ShareEntry(val name: String, val url: String, val folder: Boolean, val detail: String = "")

data class SharePage(
    val title: String,
    val hint: String,
    val entries: List<ShareEntry>,
    val up: String?,
    val url: String,
    val manage: Boolean = false,
)

sealed class ShareHit {
    data class Dir(val page: SharePage) : ShareHit()
    data class Saved(val file: java.io.File) : ShareHit()
}

class DownloadTicket {
    @Volatile var stopped = false
    @Volatile private var live: java.net.HttpURLConnection? = null

    fun bind(conn: java.net.HttpURLConnection) {
        live = conn
    }

    fun stop() {
        stopped = true
        runCatching { live?.disconnect() }
    }

    fun check() {
        if (stopped) throw DownloadStopped()
    }
}

class DownloadStopped : RuntimeException()

data class CabinetLink(val title: String, val url: String, val logout: Boolean = false)

data class CabinetGroup(val title: String, val items: List<CabinetLink>)

data class CabinetProfile(
    val name: String,
    val login: String,
    val groups: List<CabinetGroup>,
)

object CabinetApi {
    private const val HOST = "https://old.ntiustu.ru"
    private const val STUDENT_DOMAIN = "edu.ntiustu.local"

    private val sharedCookies = linkedMapOf<String, String>()

    // Фоновая копия ходит в кабинет со своей сессией: её куки не видны экрану «Кабинет» и наоборот
    private val ownCookies = ThreadLocal<LinkedHashMap<String, String>?>()
    private val cookies: LinkedHashMap<String, String> get() = ownCookies.get() ?: sharedCookies

    /** Выполнить [block] в отдельной сессии кабинета на этом потоке (для резервной копии) */
    fun <T> isolated(block: () -> T): T {
        val before = ownCookies.get()
        ownCookies.set(linkedMapOf())
        try {
            return block()
        } finally {
            ownCookies.set(before)
        }
    }

    // Регулярные выражения собираются один раз, а не на каждой строке таблицы
    private val RX_LOGIN_TOKEN = Regex("""name="nti_auth_login\[_token\]" value="([^"]+)"""")
    private val RX_UPLOAD_TOKEN = Regex("""name="nti_dashboard_bundle_share_upload_type\[_token\]" value="([^"]+)"""")
    private val RX_SITE_JSON = Regex(""""(?:message|error|detail)"\s*:\s*"([^"\\]{1,180})"""")
    private val RX_SITE_ALERT = Regex("""(?is)<div[^>]*class="[^"]*(?:alert-danger|alert-error|callout-danger)[^"]*"[^>]*>(.*?)</div>""")
    private val RX_WRAPPER = Regex("""(?is)<div class="content-wrapper"[^>]*>(.*)<footer""")
    private val RX_LIST_BODY = Regex("""(?is)<table[^>]*id="ListBody"[^>]*>(.*?)</table>""")
    private val RX_ROW = Regex("""(?is)<tr[^>]*>(.*?)</tr>""")
    private val RX_LINK = Regex("""(?is)<a([^>]*)href="([^"]+)"([^>]*)>(.*?)</a>""")
    private val RX_FILE_EXT = Regex(
        """\.(pdf|docx?|docm|dotx?|xlsx?|xlsm|xltx?|pptx?|ppsx?|potx?|od[tspg]|rtf|txt|csv|md|log|xml|json|html?|djvu|epub|fb2|chm|""" +
            """zip|rar|7z|tar|gz|tgz|bz2|xz|iso|png|jpe?g|gif|webp|bmp|tiff?|svg|heic|psd|ai|cdr|""" +
            """mp4|mp3|avi|mkv|mov|wmv|webm|wav|ogg|flac|m4a|aac|""" +
            """cdw|frw|m3d|a3d|spw|kdw|dwg|dxf|step|stp|igs|stl|sldprt|sldasm|slddrw|ipt|iam|vsdx?|mpp|""" +
            """exe|msi|apk|jar|bat|ps1|sh|py|ipynb|java|kt|cpp|c|h|cs|js|ts|css|php|sql|db|mdb|accdb|pas|dpr|lpr|vb|""" +
            """ini|cfg|conf|yaml|yml|pkt|cap|pcap|ino|hex|bin|lib|dll|ggb|mcd|xmcd|mw|m|mat|sav)$""",
        RegexOption.IGNORE_CASE,
    )
    private val RX_DOWNLOAD_WORD = Regex("""\bdownload\b""", RegexOption.IGNORE_CASE)
    private val RX_FRAME = Regex("""(?is)<(?:iframe|embed|object)\b[^>]*\s(?:src|data)="([^"]+)"""")
    private val RX_DISP_UTF = Regex("""filename\*=UTF-8''([^;]+)""", RegexOption.IGNORE_CASE)
    private val RX_DISP_PLAIN = Regex("""filename="?([^";]+)"?""", RegexOption.IGNORE_CASE)
    private val RX_BAD_NAME = Regex("""[\\/:*?"<>|\u0000-\u001F\u007F]""")
    private val RX_PERCENT = Regex("""%[0-9A-Fa-f]{2}""")
    private const val NAME_BYTES = 180
    private val RX_SCRIPT = Regex("(?is)<script\\b[^>]*>.*?</script>")
    private val RX_STYLE = Regex("(?is)<style\\b[^>]*>.*?</style>")
    private val RX_SIDEBAR = Regex("""(?is)<ul[^>]*class="[^"]*sidebar-menu[^"]*"[^>]*>(.*?)</ul>""")
    private val RX_LI = Regex("""(?is)<li([^>]*)>(.*?)</li>""")
    private val RX_HREF = Regex("""(?is)href="([^"]+)"""")
    private val RX_SPAN = Regex("""(?is)<span[^>]*>(.*?)</span>""")
    private val RX_TAG = Regex("<[^>]+>")
    private val RX_SPACES = Regex("\\s+")
    private val RX_TITLE = Regex("""(?is)<title[^>]*>(.*?)</title>""")
    private val RX_H1 = Regex("""(?is)<h1[^>]*>(.*?)</h1>""")
    private val RX_H2 = Regex("""(?is)<h2[^>]*>(.*?)</h2>""")
    private val RX_H3 = Regex("""(?is)<h3[^>]*>(.*?)</h3>""")
    private val RX_NAME_SPAN = Regex("""(?is)<span[^>]*class="[^"]*hidden-xs[^"]*"[^>]*>(.*?)</span>""")
    private val RX_FATAL = Regex("""(?i)(?:fatal error|uncaught (?:exception|error)|exception|warning)\s*[: ].{0,280}""")
    private val cookieLock = Any()

    /** Корень «Мои файлы»: с него резервная копия обходит папки кабинета */
    const val SHARE_ROOT = "$HOST/dashboard/MyShare"

    /** Есть ли в этом процессе сессия кабинета (после входа и до выхода) */
    fun signedIn(): Boolean = synchronized(cookieLock) { cookies.isNotEmpty() }

    /** Только ссылки на файлы кабинета, чтобы обход для бэкапа не ушёл на чужие страницы */
    fun inShare(url: String): Boolean = url.startsWith(SHARE_ROOT)

    fun login(userRaw: String, password: String): CabinetProfile {
        try {
            return loginStudent(userRaw, password)
        } catch (e: IllegalStateException) {
            throw e
        } catch (_: Exception) {
            error("Нет сети")
        }
    }

    private fun loginStudent(userRaw: String, password: String): CabinetProfile {
        val user = userRaw.trim()
        if (user.isEmpty() || password.isEmpty()) error("Введите имя и пароль")
        val short = user.substringBefore("@").trim()
        if (short.isEmpty()) error("Введите имя и пароль")
        val username = "$short@$STUDENT_DOMAIN"
        synchronized(cookieLock) { cookies.clear() }
        val page = exchange("GET", "$HOST/login", null)
        val token = RX_LOGIN_TOKEN
            .find(page.body)?.groupValues?.getOrNull(1)
            ?: error("Сайт не отдал форму входа")
        val result = exchange(
            "POST",
            "$HOST/login_check",
            form(
                "user" to short,
                "_password" to password,
                "_ldap_domain" to STUDENT_DOMAIN,
                "_username" to username,
                "_target_path" to "$HOST/dashboard",
                "nti_auth_login[_token]" to token,
            ),
        )
        if (failed(result)) error("Неверное имя или пароль")
        // Сайт после входа сам переводит на /dashboard: если меню уже пришло, второй запрос не нужен
        if (result.url.contains("/dashboard") && RX_SIDEBAR.containsMatchIn(result.body)) return parse(username, result.body)
        val dash = exchange("GET", "$HOST/dashboard/", null)
        if (failed(dash)) error("Неверное имя или пароль")
        return parse(username, dash.body)
    }

    fun files(url: String = "$HOST/dashboard/MyShare"): SharePage {
        try {
            val page = exchange("GET", url, null)
            if (failed(page)) error("Сессия истекла. Войдите снова")
            return parseShare(page.body, page.url)
        } catch (e: IllegalStateException) {
            throw e
        } catch (_: Exception) {
            error("Нет сети")
        }
    }

    fun upload(context: android.content.Context, pageUrl: String, uri: android.net.Uri) {
        try {
            requirePersonal(pageUrl)
            val resolver = context.contentResolver
            var name = "file"
            resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME, android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    name = cursor.getString(0) ?: name
                    if (!cursor.isNull(1) && cursor.getLong(1) > MAX_UPLOAD) error("Файл больше 50 МБ")
                }
            }
            name = name.substringAfterLast('/').substringAfterLast('\\').trim()
            val ext = name.substringAfterLast('.', "").lowercase()
            if (ext !in ALLOWED) error("Этот тип файла сайт не принимает")
            // Файл копируется во временный и отправляется потоком: в памяти не держим до 100 МБ
            val temp = java.io.File.createTempFile("upload", ".part", context.cacheDir)
            try {
                val input = resolver.openInputStream(uri) ?: error("Не удалось прочитать файл")
                val size = input.use { src -> temp.outputStream().use { dst -> copyLimited(src, dst, MAX_UPLOAD.toLong()) } }
                if (size < 0) error("Файл больше 50 МБ")
                val mime = resolver.getType(uri)?.substringBefore(';')?.trim()?.takeIf { it.isNotEmpty() } ?: mimeFor(ext)
                val page = exchange("GET", pageUrl, null)
                if (failed(page)) error("Сессия истекла. Войдите снова")
                val token = RX_UPLOAD_TOKEN
                    .find(page.body)?.groupValues?.getOrNull(1)
                    ?: error("Сайт не отдал форму загрузки")
                val result = multipart(action(pageUrl, "Upload"), pageUrl, name, temp, mime, token)
                checkUpload(result)
            } finally {
                temp.delete()
            }
            return
        } catch (e: IllegalStateException) {
            throw e
        } catch (_: Exception) {
            error("Нет сети")
        }
    }

    private fun checkUpload(result: HttpResult) {
        if (result.url.contains("/login")) error("Сессия истекла. Войдите снова")
        if (result.code == 450) error("Файл с таким именем уже существует")
        if (result.code >= 400 || result.body.contains("Internal Server Error")) error(explain(result, "Сайт не принял файл"))
    }

    /** Копирует не больше [limit] байт; -1, если файл оказался больше */
    private fun copyLimited(input: java.io.InputStream, output: java.io.OutputStream, limit: Long): Long {
        val buf = ByteArray(32 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n < 0) return total
            total += n
            if (total > limit) return -1
            output.write(buf, 0, n)
        }
    }

    fun createFolder(pageUrl: String, raw: String) {
        val name = raw.trim().trim('/', '\\')
        if (name.isEmpty()) error("Введите имя папки")
        if (name.contains('/') || name.contains('\\')) error("Имя не должно содержать слэш")
        try {
            requirePersonal(pageUrl)
            val url = ajaxUrl("CreateFolder", pageUrl, "FolderName" to name)
            val result = getOnce(url, pageUrl)
            if (result.code >= 400) error(explain(result, "Не удалось создать папку"))
            if (files(pageUrl).entries.any { it.name.equals(name, true) }) return
            error(explain(result, "Не удалось создать папку"))
        } catch (e: IllegalStateException) {
            throw e
        } catch (_: Exception) {
            error("Нет сети")
        }
    }

    fun rename(pageUrl: String, from: String, raw: String, folder: Boolean) {
        val to = raw.trim().trim('/', '\\')
        if (to.isEmpty()) error("Введите новое имя")
        if (to.contains('/') || to.contains('\\')) error("Имя не должно содержать слэш")
        try {
            requirePersonal(pageUrl)
            val kind = if (folder) "dir" else "file"
            val url = ajaxUrl("Rename", pageUrl, "Item" to from, "ItemType" to kind, "NewItemName" to to)
            val result = getOnce(url, pageUrl)
            if (result.code >= 400) error(explain(result, "Не удалось переименовать"))
            val entries = files(pageUrl).entries
            val renamed = entries.any { it.name.equals(to, true) } &&
                (from.equals(to, true) || entries.none { it.name.equals(from, true) })
            if (renamed) return
            error(explain(result, "Не удалось переименовать"))
        } catch (e: IllegalStateException) {
            throw e
        } catch (_: Exception) {
            error("Нет сети")
        }
    }

    fun remove(pageUrl: String, name: String, folder: Boolean) {
        if (name.isBlank()) error(if (folder) "Не выбрана папка" else "Не выбран файл")
        try {
            requirePersonal(pageUrl)
            val before = files(pageUrl).entries.map { it.name }
            val others = before.filter { !it.equals(name, true) }
            val kind = if (folder) "dir" else "file"
            val url = ajaxUrl("Delete", pageUrl, "Item" to name, "ItemType" to kind)
            if (!url.contains("Item=") || !url.contains("ItemType=")) error("Не удалось удалить")
            val result = getOnce(url, pageUrl)
            if (result.code >= 400) error(explain(result, "Не удалось удалить"))
            val after = files(pageUrl).entries.map { it.name }
            val gone = after.none { it.equals(name, true) }
            val kept = others.all { other -> after.any { it.equals(other, true) } }
            if (gone && kept) return
            if (!kept) error("Удаление остановлено: нельзя стирать всю папку")
            error(explain(result, "Не удалось удалить"))
        } catch (e: IllegalStateException) {
            throw e
        } catch (_: Exception) {
            error("Нет сети")
        }
    }

    private fun action(pageUrl: String, name: String): String {
        return "$HOST/dashboard/MyShare/1/$name?${targetQuery(pageUrl)}"
    }

    private fun ajaxUrl(action: String, pageUrl: String, vararg pairs: Pair<String, String>): String {
        val query = (listOf("Target" to targetForAction(pageUrl)) + pairs).joinToString("&") { (key, value) ->
            URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
        }
        return "$HOST/dashboard/MyShare/1/$action?$query"
    }

    private fun targetForAction(pageUrl: String): String {
        val value = targetValue(pageUrl).trim()
        return if (value.isEmpty() || value == "\\") "" else value
    }

    private fun requirePersonal(url: String) {
        if (!personal(url)) error("Это можно только в личной папке")
    }

    private fun personal(url: String): Boolean {
        val path = url.substringAfter("://").substringAfter("/").substringBefore("?").substringBefore("#").trim('/')
        val parts = path.split('/')
        return parts.size >= 3 && parts[0] == "dashboard" && parts[1] == "MyShare" && parts[2] == "1"
    }

    private fun targetQuery(url: String): String {
        val raw = url.substringAfter("?", "").split("&").firstOrNull { it.startsWith("Target=") }
        return raw ?: "Target="
    }

    private fun targetValue(url: String): String {
        val raw = targetQuery(url).substringAfter("Target=", "")
        return runCatching { java.net.URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
    }

    private fun siteError(html: String): String? {
        val json = RX_SITE_JSON.find(html)?.groupValues?.getOrNull(1)
        if (!json.isNullOrBlank()) return json
        val alert = RX_SITE_ALERT
            .find(html)?.groupValues?.getOrNull(1)
        return alert?.let(::strip)?.take(180)?.takeIf { it.isNotBlank() }
    }

    private fun multipart(
        url: String,
        referer: String,
        filename: String,
        file: java.io.File,
        mime: String,
        token: String,
    ): HttpResult {
        val boundary = "ntmt${System.currentTimeMillis()}"
        val safe = filename.replace("\"", "").replace("\r", "").replace("\n", "")
        val text = (
            "--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"MAX_FILE_SIZE\"\r\n\r\n" +
                "52428800\r\n"
            ).toByteArray(Charsets.UTF_8)
        val head = (
            "--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"nti_dashboard_bundle_share_upload_type[UploadFiles][]\"; filename=\"$safe\"\r\n" +
                "Content-Type: $mime\r\n\r\n"
            ).toByteArray(Charsets.UTF_8)
        val tokenPart = (
            "\r\n--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"nti_dashboard_bundle_share_upload_type[_token]\"\r\n\r\n" +
                token + "\r\n"
            ).toByteArray(Charsets.UTF_8)
        val tail = "--$boundary--\r\n".toByteArray(Charsets.UTF_8)
        val length = text.size + head.size + file.length() + tokenPart.size + tail.size
        val conn = connect(url, "POST", accept = "*/*", ajax = true, referer = referer, connectMs = 20000, readMs = 60000).apply {
            doOutput = true
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setFixedLengthStreamingMode(length)
        }
        try {
            conn.outputStream.use { out ->
                out.write(text)
                out.write(head)
                file.inputStream().use { it.copyTo(out, 32 * 1024) }
                out.write(tokenPart)
                out.write(tail)
            }
            val code = conn.responseCode
            absorb(conn)
            if (code in 300..399) {
                val loc = conn.getHeaderField("Location").orEmpty()
                val next = if (loc.isBlank()) url else absolute(url, loc).replace("http://old.ntiustu.ru", HOST)
                if (next.contains("/login")) error("Сессия истекла. Войдите снова")
                return HttpResult(next, "", code, sniff(conn))
            }
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = readLimited(stream)
            return HttpResult(url, text, code, sniff(conn))
        } finally {
            release(conn)
        }
    }

    private fun getOnce(url: String, referer: String): HttpResult {
        val conn = connect(url, "GET", accept = "application/json, text/javascript, */*; q=0.01", ajax = true, referer = referer)
        try {
            val code = conn.responseCode
            absorb(conn)
            if (code in 300..399) {
                val loc = conn.getHeaderField("Location").orEmpty()
                val next = if (loc.isBlank()) url else absolute(url, loc).replace("http://old.ntiustu.ru", HOST)
                if (next.contains("/login")) error("Сессия истекла. Войдите снова")
                return HttpResult(next, "", code, sniff(conn))
            }
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = readLimited(stream)
            if (text.contains("/login") && text.contains("_password")) error("Сессия истекла. Войдите снова")
            return HttpResult(url, text, code, sniff(conn))
        } finally {
            release(conn)
        }
    }

    fun download(context: android.content.Context, url: String, name: String = ""): java.io.File {
        return when (val hit = open(context, url, name)) {
            is ShareHit.Saved -> hit.file
            is ShareHit.Dir -> error("Это папка, а не файл")
        }
    }

    fun open(
        context: android.content.Context,
        url: String,
        name: String = "",
        more: String = "",
        onProgress: (Float) -> Unit = {},
        ticket: DownloadTicket? = null,
        into: java.io.File? = null,
    ): ShareHit {
        val urls = (listOf(url) + more.split('\n')).map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        var last = "Файл не найден"
        try {
            for (candidate in urls) {
                ticket?.check()
                try {
                    val hit = fetch(context, candidate, name, 0, onProgress, ticket, into)
                    if (hit is ShareHit.Saved || !fileExt(name)) return hit
                } catch (e: DownloadStopped) {
                    throw e
                } catch (e: IllegalStateException) {
                    if (e.message?.contains("Сессия") == true) throw e
                    last = e.message ?: last
                }
            }
            error(last)
        } catch (e: DownloadStopped) {
            throw e
        } catch (e: IllegalStateException) {
            throw e
        } catch (_: Exception) {
            if (ticket?.stopped == true) throw DownloadStopped()
            error("Нет сети")
        }
    }

    private fun fetch(
        context: android.content.Context,
        start: String,
        suggested: String,
        hop: Int,
        onProgress: (Float) -> Unit,
        ticket: DownloadTicket?,
        into: java.io.File? = null,
    ): ShareHit {
        ticket?.check()
        if (hop > 5) error("Не удалось скачать файл")
        val conn = connect(start, "GET", accept = null, connectMs = 20000, readMs = 40000)
        ticket?.bind(conn)
        try {
            ticket?.check()
            val code = conn.responseCode
            absorb(conn)
            if (code in 300..399) {
                val loc = conn.getHeaderField("Location") ?: error("Файл не найден")
                val next = absolute(start, loc).replace("http://old.ntiustu.ru", HOST)
                return fetch(context, next, suggested, hop + 1, onProgress, ticket, into)
            }
            if (code !in 200..299) error("Файл не найден")
            val type = conn.contentType.orEmpty()
            val attachment = conn.getHeaderField("Content-Disposition")?.contains("attachment", true) == true
            if (type.contains("text/html", true) && !attachment) {
                val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                ticket?.check()
                if (text.contains("/login") && text.contains("_password")) error("Сессия истекла. Войдите снова")
                val listing = !start.contains("/Download", true) &&
                    (text.contains("id=\"ListBody\"") || text.contains("id='ListBody'") ||
                        (start.contains("/dashboard/MyShare", true) && !fileExt(start.substringBefore("?"))))
                if (!listing && fileExt(suggested)) {
                    val next = findDownloadHref(text, start, suggested)
                    if (next != null && next.trimEnd('/') != start.trimEnd('/')) return fetch(context, next, suggested, hop + 1, onProgress, ticket, into)
                    error("Не удалось скачать файл")
                }
                val page = parseShare(text, start)
                if (listing || page.entries.isNotEmpty()) return ShareHit.Dir(page)
                val next = findDownloadHref(text, start, suggested)
                if (next != null && next.trimEnd('/') != start.trimEnd('/')) return fetch(context, next, suggested, hop + 1, onProgress, ticket, into)
                error("Не удалось скачать файл")
            }
            val saved = writeBody(context, conn, start, suggested, type, onProgress, ticket, into)
            return ShareHit.Saved(saved)
        } finally {
            if (ticket?.stopped != true) conn.disconnect()
        }
    }

    private fun writeBody(
        context: android.content.Context,
        conn: HttpURLConnection,
        url: String,
        suggested: String,
        type: String,
        onProgress: (Float) -> Unit,
        ticket: DownloadTicket?,
        into: java.io.File?,
    ): java.io.File {
        val name = pickName(conn.getHeaderField("Content-Disposition"), url, suggested, type)
        val dir = (into ?: Downloads.incoming(context)).apply { mkdirs() }
        val file = java.io.File(dir, name)
        // Пишем во временный файл: оборванная загрузка не оставит «скачанный» обрубок
        val part = java.io.File(dir, "$name.part")
        val total = conn.contentLengthLong
        if (total <= 0L) onProgress(-1f)
        var sent = 0L
        val buffer = ByteArray(16 * 1024)
        try {
            conn.inputStream.use { input ->
                val output = try {
                    part.outputStream()
                } catch (e: Exception) {
                    throw IllegalStateException(saveError(e))
                }
                output.use {
                    while (true) {
                        ticket?.check()
                        val n = input.read(buffer)
                        if (n < 0) break
                        try {
                            output.write(buffer, 0, n)
                        } catch (e: java.io.IOException) {
                            throw IllegalStateException(saveError(e))
                        }
                        sent += n
                        if (total > 0L) onProgress((sent.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                    }
                }
            }
            ticket?.check()
            if (total > 0L && sent < total) error("Файл скачан не полностью. Попробуйте ещё раз")
            if (file.exists()) file.delete()
            if (!part.renameTo(file)) {
                try {
                    part.copyTo(file, overwrite = true)
                } catch (e: Exception) {
                    file.delete()
                    throw IllegalStateException(saveError(e))
                } finally {
                    part.delete()
                }
            }
            return file
        } catch (e: Exception) {
            part.delete()
            if (ticket?.stopped == true || e is DownloadStopped) throw DownloadStopped()
            throw e
        }
    }

    /** Понятная причина, почему файл не записался на телефон */
    fun saveError(e: Throwable): String {
        val text = e.message.orEmpty()
        return when {
            text.contains("ENOSPC") || text.contains("No space", true) -> "Не удалось сохранить файл: нет места на телефоне"
            text.contains("ENAMETOOLONG") || text.contains("name too long", true) -> "Не удалось сохранить файл: слишком длинное имя"
            else -> "Не удалось сохранить файл"
        }
    }

    private fun folderMark(blob: String): Boolean {
        return blob.contains("fa-folder") || blob.contains("folder-open") ||
            blob.contains("folder.png") || blob.contains("folder.gif") || blob.contains("/folder")
    }

    private fun fileMark(blob: String): Boolean {
        return blob.contains("fa-file") || blob.contains("fa-download") ||
            blob.contains("glyphicon-file") || blob.contains("glyphicon-download") ||
            blob.contains("glyphicon-cloud-download")
    }

    private fun parseShare(html: String, pageUrl: String): SharePage {
        val wrapper = RX_WRAPPER
            .find(html)?.groupValues?.getOrNull(1)
            ?: html
        val title = firstText(wrapper, RX_H1) ?: "Мои файлы"
        val hint = firstText(wrapper, RX_H3).orEmpty()
        val listBody = RX_LIST_BODY
            .find(wrapper)?.groupValues?.getOrNull(1)
        val source = listBody ?: wrapper
        val entries = RX_ROW.findAll(source).mapNotNull { tr ->
            val row = tr.groupValues[1]
            val links = RX_LINK.findAll(row).toList()
            if (links.isEmpty()) return@mapNotNull null
            val parsed = links.map { link ->
                val href = unescapeHref(link.groupValues[2])
                val inner = link.groupValues[1] + link.groupValues[3] + link.groupValues[4]
                val body = link.groupValues[4]
                href to (inner to body)
            }.filter { (href, _) ->
                href.isNotBlank() && !href.startsWith("#") && !href.contains("/logout") &&
                    !href.contains("/dashboard/Schedule") && !href.contains("/dashboard/MyMaterial")
            }
            if (parsed.isEmpty()) return@mapNotNull null
            val folderLink = parsed.firstOrNull { (_, parts) ->
                val blob = parts.first + parts.second
                blob.contains("fa-folder") || blob.contains("folder-open")
            }
            val named = parsed.firstOrNull { (_, parts) -> strip(parts.second).isNotBlank() } ?: parsed.first()
            val name = strip(named.second.second).ifBlank { strip(named.second.first) }
            if (name.isBlank() || name == title) return@mapNotNull null
            val download = parsed.firstOrNull { (href, _) -> href.contains("/Download", true) }
            val folderRow = download == null && !fileExt(name) &&
                (folderMark(row) || folderLink != null || numericFolder(named.first) || row.contains("folder", true))
            if (folderRow) {
                ShareEntry(name, absolute(pageUrl, folderLink?.first ?: named.first), true)
            } else {
                val href = download?.first ?: named.first
                ShareEntry(name, absolute(pageUrl, href), false)
            }
        }.distinctBy { it.url + it.name }.toList()
        val resolved = if (listBody != null) entries else entries.ifEmpty { looseEntries(wrapper, pageUrl, title) }
        return SharePage(title, hint, resolved, shareUp(pageUrl), pageUrl, personal(pageUrl))
    }

    private fun looseEntries(wrapper: String, pageUrl: String, title: String): List<ShareEntry> {
        return RX_LINK.findAll(wrapper).mapNotNull { link ->
            val href = unescapeHref(link.groupValues[2])
            if (href.startsWith("#") || href.contains("/logout") || href.contains("/dashboard/Schedule") || href.contains("/dashboard/MyMaterial")) {
                return@mapNotNull null
            }
            val blob = link.groupValues[1] + link.groupValues[3] + link.groupValues[4]
            val name = strip(link.groupValues[4])
            if (name.isBlank() || name == title) return@mapNotNull null
            val fileLike = fileExt(name) || fileExt(href) || fileMark(blob)
            val folder = !fileLike && (blob.contains("fa-folder") || blob.contains("folder-open") || numericFolder(href))
            ShareEntry(name, absolute(pageUrl, href), folder)
        }.distinctBy { it.url }.toList()
    }

    private fun unescapeHref(raw: String): String {
        return raw.replace("&" + "amp;", "&")
            .replace("&" + "quot;", "\"")
            .replace("&" + "#039;", "'")
            .replace("&" + "#39;", "'")
            .replace("&" + "lt;", "<")
            .replace("&" + "gt;", ">")
    }

    private fun shareUp(url: String): String? {
        val path = url.substringAfter("://").substringAfter("/").substringBefore("?").trimEnd('/')
        if (path == "dashboard/MyShare") return null
        if (!path.startsWith("dashboard/MyShare/")) return null
        return "$HOST/${path.substringBeforeLast("/")}"
    }

    private fun numericFolder(href: String): Boolean {
        if (fileExt(href)) return false
        val last = href.substringBefore("?").trimEnd('/').substringAfterLast('/')
        return last.isNotEmpty() && last.all { it.isDigit() }
    }

    private fun fileExt(href: String): Boolean {
        val path = href.substringBefore("?").substringAfterLast("/")
        return RX_FILE_EXT
            .containsMatchIn(path)
    }

    private fun findDownloadHref(html: String, pageUrl: String, suggested: String = ""): String? {
        val wrapper = RX_WRAPPER
            .find(html)?.groupValues?.getOrNull(1)
            ?: html
        val links = RX_LINK.findAll(wrapper).map { link ->
            val href = absolute(pageUrl, unescapeHref(link.groupValues[2]))
            val blob = link.groupValues[1] + link.groupValues[3] + link.groupValues[4]
            href to blob
        }.filter { (href, blob) ->
            !href.contains("/logout") && !blob.contains("fa-folder")
        }.toList()
        val wanted = suggested.trim()
        if (wanted.isNotEmpty()) {
            val byName = links.firstOrNull { (_, blob) ->
                strip(blob).equals(wanted, true) || blob.contains(wanted, true)
            }
            if (byName != null) return byName.first
            val encoded = URLEncoder.encode(wanted, "UTF-8").replace("+", "%20")
            val byHref = links.firstOrNull { (href, _) -> href.contains(wanted, true) || href.contains(encoded, true) }
            if (byHref != null) return byHref.first
        }
        val marked = links.filter { (_, blob) ->
            blob.contains("fa-download") || blob.contains("glyphicon-download") ||
                blob.contains("скачать", true) || RX_DOWNLOAD_WORD.containsMatchIn(blob)
        }
        if (marked.size == 1) return marked.first().first
        val files = links.filter { (href, blob) -> fileExt(href) || fileExt(strip(blob)) }
        if (files.size == 1) return files.first().first
        val frame = RX_FRAME
            .find(wrapper)?.groupValues?.getOrNull(1)
        if (frame != null) return absolute(pageUrl, frame)
        return null
    }

    private fun pickName(disposition: String?, url: String, suggested: String, mime: String): String {
        val fromHeader = disposition?.let { header ->
            RX_DISP_UTF.find(header)?.groupValues?.getOrNull(1)?.let(::percentDecode)
                ?: RX_DISP_PLAIN.find(header)?.groupValues?.getOrNull(1)?.let { plain ->
                    if (RX_PERCENT.containsMatchIn(plain)) percentDecode(plain) else latinToUtf(plain)
                }
        }?.trim().orEmpty()
        val raw = when {
            fileExt(fromHeader) -> fromHeader
            fileExt(suggested) -> suggested
            fromHeader.isNotBlank() -> fromHeader
            suggested.isNotBlank() -> suggested
            else -> url.substringBefore("?").substringAfterLast("/").ifBlank { "file" }
        }
        var clean = safeName(raw)
        if (!fileExt(clean)) {
            val ext = mimeExt(mime)
            if (ext != null) clean = safeName("$clean.$ext")
        }
        return clean
    }

    /**
     * Имя, которое точно можно записать: без запрещённых символов, не «.» и не «..»,
     * не длиннее [NAME_BYTES] байт (русская буква занимает 2 байта, предел системы 255)
     */
    fun safeName(raw: String): String {
        var clean = raw.replace(RX_BAD_NAME, "_").trim().trim('.').trim()
        if (clean.isBlank()) clean = "file"
        if (clean.toByteArray(Charsets.UTF_8).size > NAME_BYTES) {
            val dot = clean.lastIndexOf('.')
            val ext = if (dot > 0 && clean.length - dot <= 10) clean.substring(dot) else ""
            var base = clean.dropLast(ext.length)
            val room = NAME_BYTES - ext.toByteArray(Charsets.UTF_8).size
            while (base.isNotEmpty() && base.toByteArray(Charsets.UTF_8).size > room) base = base.dropLast(1)
            if (base.isNotEmpty() && Character.isHighSurrogate(base.last())) base = base.dropLast(1)
            clean = base.trimEnd() + ext
        }
        return clean
    }

    private fun percentDecode(raw: String): String =
        runCatching { java.net.URLDecoder.decode(raw.replace("+", "%2B"), "UTF-8") }.getOrDefault(raw)

    // Сервер может отдать имя в UTF-8 без кодирования, а HttpURLConnection читает заголовок как Latin-1
    private fun latinToUtf(raw: String): String {
        if (raw.none { it.code in 0x80..0xFF } || raw.any { it.code > 0xFF }) return raw
        val bytes = raw.toByteArray(Charsets.ISO_8859_1)
        val decoded = String(bytes, Charsets.UTF_8)
        return if (decoded.contains('\uFFFD')) raw else decoded
    }

    private fun mimeExt(mime: String): String? {
        val type = mime.substringBefore(";").trim().lowercase()
        return when (type) {
            "application/pdf" -> "pdf"
            "application/zip" -> "zip"
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "text/plain" -> "txt"
            "application/msword" -> "doc"
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
            "application/vnd.ms-excel" -> "xls"
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
            "application/vnd.ms-powerpoint" -> "ppt"
            "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx"
            else -> null
        }
    }

    private fun mimeFor(ext: String): String {
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "rtf" -> "application/rtf"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "djvu" -> "image/vnd.djvu"
            "zip" -> "application/zip"
            "rar" -> "application/vnd.rar"
            "7z" -> "application/x-7z-compressed"
            else -> "application/octet-stream"
        }
    }

    fun logout() {
        try {
            exchange("GET", "$HOST/logout", null)
        } catch (_: Exception) {
        }
        synchronized(cookieLock) { cookies.clear() }
    }

    private fun failed(res: HttpResult): Boolean {
        return res.url.contains("/login") || res.body.contains("Invalid credentials", true)
    }

    private fun parse(login: String, html: String): CabinetProfile {
        val clean = html
            .replace(RX_SCRIPT, " ")
            .replace(RX_STYLE, " ")
        val name = firstText(clean, RX_NAME_SPAN)
            ?: login.substringBefore("@")
        return CabinetProfile(name, login, menu(clean))
    }

    private fun menu(html: String): List<CabinetGroup> {
        val block = RX_SIDEBAR
            .find(html)?.groupValues?.getOrNull(1)
            ?: return emptyList()
        val groups = mutableListOf<CabinetGroup>()
        var title = "Разделы"
        val items = mutableListOf<CabinetLink>()
        fun flush() {
            if (items.isNotEmpty()) {
                groups += CabinetGroup(title, items.toList())
                items.clear()
            }
        }
        RX_LI.findAll(block).forEach { li ->
            val attrs = li.groupValues[1]
            val inner = li.groupValues[2]
            if (attrs.contains("header")) {
                flush()
                title = strip(inner).ifBlank { "Разделы" }
                return@forEach
            }
            val href = RX_HREF.find(inner)?.groupValues?.getOrNull(1) ?: return@forEach
            val label = RX_SPAN
                .find(inner)?.groupValues?.getOrNull(1)?.let(::strip)
                ?: strip(inner)
            if (label.isBlank()) return@forEach
            val url = absolute("$HOST/dashboard/", href)
            items += CabinetLink(label, url, url.contains("/logout"))
        }
        flush()
        return groups
    }

    private fun firstText(html: String, pattern: Regex): String? {
        return pattern.find(html)?.groupValues?.getOrNull(1)?.let(::strip)?.takeIf { it.isNotBlank() }
    }

    private fun strip(raw: String): String {
        val s = raw.replace(RX_TAG, " ")
            .replace("&" + "nbsp;", " ")
            .replace("&" + "amp;", "&")
            .replace("&" + "quot;", "\"")
            .replace("&" + "#39;", "'")
            .replace("&" + "lt;", "<")
            .replace("&" + "gt;", ">")
        return s.replace(RX_SPACES, " ").trim()
    }

    private data class HttpResult(val url: String, val body: String, val code: Int = 200, val extra: String = "")

    private fun sniff(conn: HttpURLConnection): String {
        val names = listOf("X-Debug-Exception", "X-Debug-Exception-File", "X-Debug-Token-Link")
        return names.mapNotNull { name ->
            conn.getHeaderField(name)?.trim()?.takeIf { it.isNotEmpty() }?.let { "$name: ${it.take(200)}" }
        }.joinToString("\n")
    }

    private fun readLimited(stream: java.io.InputStream?): String {
        if (stream == null) return ""
        return stream.bufferedReader(Charsets.UTF_8).use { reader ->
            val buf = CharArray(24000)
            val n = reader.read(buf)
            if (n <= 0) "" else String(buf, 0, n)
        }
    }

    private fun explain(result: HttpResult, fallback: String): String {
        val path = result.url.substringAfter("://").substringAfter("/").substringBefore("?").let { raw ->
            if (raw.isBlank()) "" else "/$raw"
        }
        val head = buildString {
            if (result.code > 0) append("Код ${result.code}")
            if (path.isNotBlank()) append(" · ").append(path)
        }
        val text = replyText(result.body)
        val parts = listOf(head, result.extra, text).filter { it.isNotBlank() }
        if (parts.isEmpty()) return fallback
        if (text.isBlank() && result.code in 200..399) return (parts + fallback).joinToString("\n").take(1200)
        return parts.joinToString("\n").take(1200)
    }

    private fun replyText(html: String): String {
        if (html.isBlank()) return ""
        siteError(html)?.let { return it }
        val title = RX_TITLE.find(html)?.groupValues?.getOrNull(1)?.let(::strip).orEmpty()
        val h1 = RX_H1.find(html)?.groupValues?.getOrNull(1)?.let(::strip).orEmpty()
        val h2 = RX_H2.find(html)?.groupValues?.getOrNull(1)?.let(::strip).orEmpty()
        val plain = strip(html)
        val fatal = RX_FATAL
            .find(plain)?.value?.trim().orEmpty()
        val picked = listOf(fatal, h1, h2, title).map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val excerpt = plain.take(700)
        val head = picked.joinToString("\n")
        if (head.isBlank()) return excerpt
        if (excerpt.isBlank() || excerpt.startsWith(head.take(40))) return head.take(900)
        return (head + "\n" + excerpt).take(900)
    }

    private fun exchange(method: String, start: String, body: String?, referer: String? = null, ajax: Boolean = false): HttpResult {
        var current = start
        var currentMethod = method
        var currentBody = body
        repeat(6) {
            val payload = currentBody?.toByteArray(Charsets.UTF_8)
            val conn = connect(current, currentMethod, accept = if (ajax) "text/html,application/json" else "text/html", ajax = ajax).apply {
                if (payload != null) {
                    doOutput = true
                    setFixedLengthStreamingMode(payload.size)
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    setRequestProperty("Referer", referer ?: "$HOST/login")
                    setRequestProperty("Origin", HOST)
                }
            }
            try {
                if (payload != null) conn.outputStream.use { it.write(payload) }
                val code = conn.responseCode
                absorb(conn)
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location") ?: error("Сайт не ответил")
                    current = absolute(current, loc).replace("http://old.ntiustu.ru", HOST)
                    currentMethod = "GET"
                    currentBody = null
                    return@repeat
                }
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error(if (ajax) siteError(text) ?: "Сайт не ответил" else "Сайт не ответил")
                return HttpResult(current, text)
            } finally {
                release(conn)
            }
        }
        error("Сайт не ответил")
    }

    /** Общая настройка запроса к кабинету: заголовки, куки, таймауты, без автоперехода по редиректам */
    private fun connect(
        url: String,
        method: String,
        accept: String?,
        ajax: Boolean = false,
        referer: String? = null,
        connectMs: Int = 15000,
        readMs: Int = 20000,
    ): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = false
        requestMethod = method
        connectTimeout = connectMs
        readTimeout = readMs
        setRequestProperty("User-Agent", "Mozilla/5.0 NTMT")
        if (accept != null) setRequestProperty("Accept", accept)
        if (ajax) setRequestProperty("X-Requested-With", "XMLHttpRequest")
        if (referer != null) setRequestProperty("Referer", referer)
        val cookie = cookieHeader(this.url)
        if (cookie.isNotEmpty()) setRequestProperty("Cookie", cookie)
    }

    /**
     * Закрывает ответ, но не рвёт соединение: следующий запрос к кабинету идёт по тому же
     * TLS-соединению (keep-alive), без нового рукопожатия
     */
    private fun release(conn: HttpURLConnection) {
        runCatching { conn.inputStream.close() }
        runCatching { conn.errorStream?.close() }
    }

    private fun absolute(from: String, loc: String): String {
        if (loc.startsWith("http://") || loc.startsWith("https://")) return loc
        if (loc.startsWith("?")) return from.substringBefore("?") + loc
        if (loc.startsWith("/")) return HOST + loc
        return from.substringBefore("?").substringBeforeLast("/") + "/" + loc
    }

    // Куки сессии принадлежат только сайту кабинета: не берём и не отдаём их другим хостам
    private val COOKIE_HOST = URL(HOST).host

    private fun ours(url: URL): Boolean = url.protocol == "https" && url.host.equals(COOKIE_HOST, true)

    private fun absorb(conn: HttpURLConnection) {
        if (!ours(conn.url)) return
        val values = conn.headerFields.entries
            .filter { it.key != null && it.key.equals("Set-Cookie", true) }
            .flatMap { it.value }
        synchronized(cookieLock) {
            for (raw in values) {
                val pair = raw.substringBefore(";").trim()
                val name = pair.substringBefore("=").trim()
                val value = pair.substringAfter("=", "")
                if (name.isNotEmpty()) cookies[name] = value
            }
        }
    }

    private fun cookieHeader(url: URL): String = if (!ours(url)) "" else synchronized(cookieLock) {
        cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    private fun form(vararg pairs: Pair<String, String>): String {
        return pairs.joinToString("&") { (k, v) ->
            URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
        }
    }

    private const val MAX_UPLOAD = 50 * 1024 * 1024

    private val ALLOWED = setOf(
        "jpeg", "jpg", "png", "pdf", "txt", "doc", "docx", "ppt", "pptx", "rtf", "xls", "xlsx", "djvu", "zip", "rar", "7z",
    )
}
