package ntmt.schedule.ui

import android.app.Activity
import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import android.content.ActivityNotFoundException
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.core.content.FileProvider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ntmt.schedule.data.CabinetApi
import ntmt.schedule.data.CabinetProfile
import ntmt.schedule.data.CabinetVault
import ntmt.schedule.data.DownloadStopped
import ntmt.schedule.data.DownloadTicket
import ntmt.schedule.data.ShareEntry
import ntmt.schedule.data.ShareHit
import ntmt.schedule.data.SharePage
import ntmt.schedule.notify.Notify

internal object CabinetTransfer {
    var name by mutableStateOf<String?>(null)
    var fraction by mutableFloatStateOf(0f)
    var cancel: (() -> Unit)? = null

    fun clear() {
        name = null
        fraction = 0f
        cancel = null
    }
}

private data class Crumb(val name: String, val url: String)

private sealed class Ask {
    data object Create : Ask()
    data class Rename(val name: String, val folder: Boolean) : Ask()
    data class Remove(val name: String, val folder: Boolean) : Ask()
}

private const val SHARE_ROOT = "https://old.ntiustu.ru/dashboard/MyShare"
private const val SESSION_MS = 10 * 60 * 1000L

private data class Notice(val text: String, val toFolder: Boolean = false)

@Composable
fun CabinetPane(active: Boolean = true, modifier: Modifier = Modifier.fillMaxSize()) {
    val ctx = LocalContext.current
    val activity = ctx as Activity
    val scope = rememberCoroutineScope()
    var login by remember { mutableStateOf(CabinetVault.peekLogin(ctx).orEmpty()) }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(CabinetVault.has(ctx)) }
    var saved by remember { mutableStateOf(CabinetVault.has(ctx)) }
    var other by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var loadingUrl by remember { mutableStateOf<String?>(null) }
    var loadingName by remember { mutableStateOf<String?>(null) }
    var loadingFraction by remember { mutableFloatStateOf(0f) }
    var ticket by remember { mutableStateOf<DownloadTicket?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var profile by remember { mutableStateOf<CabinetProfile?>(null) }
    var files by remember { mutableStateOf<SharePage?>(null) }
    var pages by remember { mutableStateOf<List<SharePage>>(emptyList()) }
    var crumbs by remember { mutableStateOf<List<Crumb>>(emptyList()) }
    var downloads by remember { mutableStateOf(false) }
    var savedFiles by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    var notice by remember { mutableStateOf<Notice?>(null) }
    var toastTick by remember { mutableIntStateOf(0) }
    var ask by remember { mutableStateOf<Ask?>(null) }
    var draft by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    var signedAt by remember { mutableLongStateOf(0L) }
    var prompt by remember { mutableIntStateOf(0) }
    var background by remember { mutableStateOf(false) }
    fun flash(text: String, toFolder: Boolean = false) {
        toastTick += 1
        notice = Notice(text, toFolder)
    }
    val keyguard = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val next = pending
        pending = null
        if (res.resultCode == RESULT_OK) next?.invoke()
    }

    fun signIn(user: String, pass: String, store: Boolean) {
        if (busy) return
        busy = true
        error = null
        notice = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { CabinetApi.login(user, pass) }
            }
            busy = false
            result.onSuccess { got ->
                profile = got
                files = null
                pages = emptyList()
                crumbs = emptyList()
                downloads = false
                other = false
                signedAt = System.currentTimeMillis()
                if (store) {
                    if (!CabinetLock.available(activity)) {
                        error = "Включите блокировку экрана, чтобы запомнить вход"
                        CabinetVault.clear(ctx)
                        saved = false
                        rememberMe = false
                    } else {
                        CabinetVault.save(ctx, user.trim(), pass)
                        saved = true
                        login = user.trim()
                        password = ""
                    }
                } else {
                    CabinetVault.clear(ctx)
                    saved = false
                }
            }.onFailure {
                error = it.message ?: "Не удалось войти"
            }
        }
    }

    fun unlockThen(block: () -> Unit) {
        error = null
        if (!CabinetLock.available(activity)) {
            error = "На устройстве не включена блокировка экрана"
            return
        }
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            CabinetLock.biometric(activity, block) { error = it }
        } else {
            val intent = CabinetLock.credentialIntent(activity)
            if (intent == null) {
                error = "На устройстве не включена блокировка экрана"
            } else {
                pending = block
                keyguard.launch(intent)
            }
        }
    }

    LaunchedEffect(active, saved, prompt) {
        if (!active || !saved || other || profile != null) return@LaunchedEffect
        delay(300)
        if (!active || !saved || other || profile != null || busy) return@LaunchedEffect
        unlockThen {
            val pair = CabinetVault.read(ctx)
            if (pair == null) {
                saved = false
                error = "Сохранённый вход не найден"
            } else {
                signIn(pair.first, pair.second, true)
            }
        }
    }

    LaunchedEffect(signedAt) {
        val mark = signedAt
        if (mark == 0L) return@LaunchedEffect
        while (true) {
            if (signedAt != mark || profile == null) return@LaunchedEffect
            val left = SESSION_MS - (System.currentTimeMillis() - mark)
            if (left <= 0L) break
            delay(left.coerceAtMost(20_000L))
        }
        if (signedAt != mark || profile == null) return@LaunchedEffect
        profile = null
        files = null
        pages = emptyList()
        crumbs = emptyList()
        downloads = false
        ask = null
        withContext(Dispatchers.IO) { runCatching { CabinetApi.logout() } }
        prompt += 1
    }

    LaunchedEffect(active) {
        if (!active) ask = null
    }

    val life = LocalLifecycleOwner.current
    DisposableEffect(life) {
        val watch = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    background = true
                    val label = loadingName
                    if (label != null) Notify.fileProgress(ctx, label, loadingFraction)
                }
                Lifecycle.Event.ON_START -> {
                    background = false
                    Notify.fileClear(ctx)
                }
                else -> Unit
            }
        }
        life.lifecycle.addObserver(watch)
        onDispose { life.lifecycle.removeObserver(watch) }
    }

    fun savedDir(): java.io.File = java.io.File(ctx.cacheDir, "share").apply { mkdirs() }

    fun reloadSaved() {
        savedFiles = savedDir().listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }.orEmpty()
    }

    fun alreadySaved(name: String): Boolean {
        val clean = name.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
        if (clean.isEmpty()) return false
        return savedDir().listFiles()?.any { it.isFile && it.name.equals(clean, true) } == true
    }

    fun openSaved(file: java.io.File) {
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "application/octet-stream"
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            ctx.startActivity(Intent.createChooser(intent, file.name))
        } catch (_: ActivityNotFoundException) {
            error = "Нет приложения для этого файла"
        }
    }

    fun shareSaved(file: java.io.File) {
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "application/octet-stream"
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(send, "Поделиться").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            ctx.startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            error = "Нечем поделиться"
        }
    }

    fun loadFolder(name: String, url: String, next: List<Crumb>) {
        if (busy) return
        busy = true
        error = null
        notice = null
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { CabinetApi.open(ctx, url, name) } }
            busy = false
            result.onSuccess { hit ->
                when (hit) {
                    is ShareHit.Dir -> {
                        files = hit.page
                        val kept = if (next.size <= 1) emptyList() else pages.take(next.size - 1)
                        pages = if (next.size <= 1 || kept.size != next.size - 1) listOf(hit.page) else kept + hit.page
                        crumbs = next.ifEmpty { listOf(Crumb(name, url)) }
                        downloads = false
                    }
                    is ShareHit.Saved -> {
                        reloadSaved()
                        flash("Скачано: ${hit.file.name}")
                    }
                }
            }.onFailure { error = it.message ?: "Не удалось открыть папку" }
        }
    }

    fun refresh(url: String) {
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { CabinetApi.files(url) } }
            result.onSuccess { page ->
                files = page
                if (pages.isNotEmpty()) pages = pages.dropLast(1) + page
            }.onFailure { error = it.message }
        }
    }

    fun runManage(done: String, block: () -> Unit) {
        val url = files?.url ?: return
        if (busy) return
        busy = true
        error = null
        notice = null
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching(block) }
            busy = false
            result.onSuccess {
                flash(done)
                refresh(url)
            }.onFailure { error = it.message ?: "Не получилось" }
        }
    }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val url = files?.url ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        runManage("Файл загружен") { CabinetApi.upload(ctx, url, uri) }
    }

    fun leaveFolder() {
        if (downloads) {
            downloads = false
            return
        }
        if (pages.size <= 1) {
            files = null
            pages = emptyList()
            crumbs = emptyList()
            return
        }
        val prevPages = pages.dropLast(1)
        pages = prevPages
        crumbs = crumbs.dropLast(1)
        files = prevPages.last()
    }

    fun cancelDownload() {
        ticket?.stop()
    }

    fun saveRemote(url: String, name: String, more: String = "") {
        if (loadingUrl != null) return
        if (alreadySaved(name)) {
            reloadSaved()
            flash("Файл уже скачан", toFolder = true)
            return
        }
        val current = DownloadTicket()
        ticket = current
        loadingUrl = url
        loadingName = name
        loadingFraction = 0f
        error = null
        CabinetTransfer.name = name
        CabinetTransfer.fraction = 0f
        CabinetTransfer.cancel = { current.stop() }
        var noted = 0L
        if (background) Notify.fileProgress(ctx, name, 0f)
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    CabinetApi.open(ctx, url, name, more, onProgress = { fraction ->
                        loadingFraction = fraction
                        CabinetTransfer.fraction = fraction
                        if (background) {
                            val now = System.currentTimeMillis()
                            if (now - noted >= 700L) {
                                noted = now
                                Notify.fileProgress(ctx, name, fraction)
                            }
                        }
                    }, ticket = current)
                }
            }
            if (ticket === current) {
                ticket = null
                loadingUrl = null
                loadingName = null
                CabinetTransfer.clear()
            }
            val leftApp = background
            result.onSuccess { hit ->
                when (hit) {
                    is ShareHit.Saved -> {
                        reloadSaved()
                        flash("Скачано: ${hit.file.name}", toFolder = true)
                        if (leftApp) Notify.fileDone(ctx, hit.file.name) else Notify.fileClear(ctx)
                    }
                    is ShareHit.Dir -> {
                        Notify.fileClear(ctx)
                        files = hit.page
                        pages = pages + hit.page
                        crumbs = crumbs + Crumb(name, url)
                        downloads = false
                    }
                }
            }.onFailure { err ->
                Notify.fileClear(ctx)
                if (err is DownloadStopped || current.stopped) return@onFailure
                notice = null
                error = err.message ?: "Не удалось скачать"
            }
        }
    }

    val message = notice
    if (message != null) {
        LaunchedEffect(toastTick) {
            delay(if (message.toFolder) 5_000 else 3_000)
            notice = null
        }
    }
    Box(
        modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (!active || profile == null || event.changes.none { it.pressed }) continue
                    val now = System.currentTimeMillis()
                    if (now - signedAt >= 5_000L) signedAt = now
                }
            }
        },
    ) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Личный кабинет",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }
        }
        if (profile != null && error != null) {
            SelectionContainer {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        val shown = profile
        val page = files
        val cabinetBack = shown != null && (downloads || page != null)
        BackHandler(enabled = cabinetBack) { leaveFolder() }
        if (shown != null && downloads) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { downloads = false }) { Icon(Icons.Outlined.ChevronLeft, "Назад") }
                Text("Скаченные файлы", style = MaterialTheme.typography.titleMedium)
            }
            if (savedFiles.isEmpty()) {
                Text("Нет скаченных файлов", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            savedFiles.forEach { file ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Description, contentDescription = null)
                        Text(
                            file.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp).clickable { openSaved(file) },
                        )
                        IconButton(onClick = { shareSaved(file) }) { Icon(Icons.Outlined.Share, "Поделиться") }
                        IconButton(onClick = {
                            file.delete()
                            reloadSaved()
                        }) { Icon(Icons.Outlined.Delete, "Удалить") }
                    }
                }
            }
        } else if (shown != null && page != null) {
            fun clean(url: String) = url.trim().trimEnd('/').substringBefore("#")
            val here = clean(page.url)
            val parent = page.up?.let(::clean)
            val been = crumbs.map { it.name.trim() }.filter { it.isNotEmpty() }.toSet()
            val beenUrl = crumbs.map { clean(it.url) }.toSet()
            val items = page.entries.filter { entry ->
                val link = clean(entry.url)
                if (link == here || link == parent || link in beenUrl) return@filter false
                if (!entry.folder) return@filter true
                val name = entry.name.trim()
                name !in been && name != page.title.trim() && name != ".." &&
                    !name.equals("назад", true) && !name.equals("вверх", true)
            }.sortedBy { if (it.folder) 0 else 1 }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { leaveFolder() }) { Icon(Icons.Outlined.ChevronLeft, "Назад") }
                Text(
                    crumbs.lastOrNull()?.name ?: page.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            Button(
                onClick = {
                    reloadSaved()
                    downloads = true
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Скаченные файлы") }
            Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                crumbs.forEachIndexed { index, crumb ->
                    if (index > 0) {
                        Text(" / ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        crumb.name,
                        color = if (index == crumbs.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable {
                            if (index < crumbs.lastIndex && index < pages.lastIndex) {
                                crumbs = crumbs.take(index + 1)
                                pages = pages.take(index + 1)
                                files = pages.last()
                            }
                        },
                    )
                }
            }
            if (page.manage) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            draft = ""
                            ask = Ask.Create
                        },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Папка")
                    }
                    OutlinedButton(
                        onClick = { pick.launch(arrayOf("*/*")) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Outlined.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Загрузить")
                    }
                }
            }
            if (items.isEmpty()) {
                Text("Папка пуста", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items.forEach { entry ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !busy && (entry.folder || loadingUrl == null)) {
                        if (entry.folder) {
                            loadFolder(entry.name, entry.url, crumbs + Crumb(entry.name, entry.url))
                        } else {
                            saveRemote(entry.url, entry.name, entry.detail)
                        }
                    },
                ) {
                    Column {
                        Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (entry.folder) Icons.Outlined.Folder else Icons.Outlined.Description,
                                contentDescription = null,
                            )
                            Row(Modifier.weight(1f).padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    entry.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (loadingUrl == entry.url) {
                                    IconButton(onClick = { cancelDownload() }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Остановить загрузку", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            if (!entry.folder) {
                                IconButton(onClick = { saveRemote(entry.url, entry.name, entry.detail) }, enabled = !busy && loadingUrl == null) {
                                    Icon(Icons.Outlined.Download, "Скачать")
                                }
                            }
                            if (page.manage) {
                                IconButton(
                                    onClick = {
                                        draft = entry.name
                                        ask = Ask.Rename(entry.name, entry.folder)
                                    },
                                    enabled = !busy,
                                ) { Icon(Icons.Outlined.Edit, "Переименовать") }
                                IconButton(
                                    onClick = { ask = Ask.Remove(entry.name, entry.folder) },
                                    enabled = !busy,
                                ) { Icon(Icons.Outlined.Delete, "Удалить") }
                            }
                        }
                        if (loadingUrl == entry.url) {
                            if (loadingFraction > 0f) {
                                LinearProgressIndicator(
                                    progress = { loadingFraction },
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                )
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp))
                            }
                        }
                    }
                }
            }
        } else if (shown != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Person, contentDescription = null)
                        Column {
                            Text(shown.name, style = MaterialTheme.typography.titleMedium)
                            Text(shown.login, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = { loadFolder("Мои файлы", SHARE_ROOT, listOf(Crumb("Мои файлы", SHARE_ROOT))) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Outlined.Folder, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Мои файлы")
                    }
                    OutlinedButton(
                        onClick = {
                            reloadSaved()
                            downloads = true
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text("Скаченные файлы") }
                }
            }
            OutlinedButton(
                onClick = {
                    scope.launch(Dispatchers.IO) { runCatching { CabinetApi.logout() } }
                    signedAt = 0L
                    profile = null
                    files = null
                    pages = emptyList()
                    crumbs = emptyList()
                    downloads = false
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text("Выйти") }
            if (saved) {
                TextButton(onClick = {
                    CabinetVault.clear(ctx)
                    saved = false
                    rememberMe = false
                }) { Text("Забыть вход") }
            }
        } else if (saved && !other) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(login.ifBlank { "Сохранённый вход" }, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Подтверждение кодом, отпечатком или лицом",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            unlockThen {
                                val pair = CabinetVault.read(ctx)
                                if (pair == null) {
                                    saved = false
                                    error = "Сохранённый вход не найден"
                                } else {
                                    signIn(pair.first, pair.second, true)
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (busy) "Вход…" else "Быстрый вход") }
                    TextButton(onClick = { other = true }) { Text("Другое имя") }
                }
            }
        } else {
            OutlinedTextField(
                value = login,
                onValueChange = { login = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Имя пользователя") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(16.dp),
            )
            Line(
                "Запомнить",
                "Быстрый вход кодом, отпечатком или лицом",
                rememberMe,
            ) { rememberMe = it }
            Button(
                onClick = { signIn(login, password, rememberMe) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "Вход…" else "Войти") }
            if (saved) {
                TextButton(onClick = { other = false }) { Text("Быстрый вход") }
            }
        }
        if (error != null && profile == null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        when (val pending = ask) {
            Ask.Create, is Ask.Rename -> AlertDialog(
                onDismissRequest = { ask = null },
                title = { Text(if (pending is Ask.Create) "Новая папка" else "Переименовать") },
                text = {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val url = files?.url
                            val current = pending
                            ask = null
                            if (url == null) return@TextButton
                            if (current is Ask.Create) runManage("Папка создана") { CabinetApi.createFolder(url, draft) }
                            if (current is Ask.Rename) runManage("Имя изменено") { CabinetApi.rename(url, current.name, draft, current.folder) }
                        },
                    ) { Text("Сохранить") }
                },
                dismissButton = { TextButton(onClick = { ask = null }) { Text("Отмена") } },
            )
            is Ask.Remove -> AlertDialog(
                onDismissRequest = { ask = null },
                title = { Text(if (pending.folder) "Удалить папку?" else "Удалить файл?") },
                text = { Text(pending.name) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val url = files?.url
                            val name = pending.name
                            val folder = pending.folder
                            ask = null
                            if (url != null) runManage("Удалено") { CabinetApi.remove(url, name, folder) }
                        },
                    ) { Text("Удалить") }
                },
                dismissButton = { TextButton(onClick = { ask = null }) { Text("Отмена") } },
            )
            null -> Unit
        }
    }
    val activeName = loadingName
    val onThisPage = !downloads && files?.entries?.any { it.url == loadingUrl } == true
    if (activeName != null && !onThisPage) {
        DownloadNotice(
            name = activeName,
            fraction = loadingFraction,
            onCancel = { cancelDownload() },
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }
    AnimatedContent(
        targetState = message,
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        transitionSpec = {
            (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 3 })
                .togetherWith(fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 3 })
        },
        label = "action",
    ) { item ->
        if (item != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 6.dp,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.text,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f, fill = false).padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = if (item.toFolder) 0.dp else 16.dp),
                    )
                    if (item.toFolder) {
                        TextButton(
                            onClick = {
                                reloadSaved()
                                downloads = true
                                notice = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.inversePrimary),
                        ) { Text("Перейти в папку") }
                    }
                }
            }
        }
    }
    }
}

@Composable
internal fun DownloadNotice(
    name: String,
    fraction: Float,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
    ) {
        Column {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 10.dp).weight(1f, fill = false),
                )
                IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Остановить загрузку", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.weight(1f))
            }
            if (fraction > 0f) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(3.dp))
            }
        }
    }
}
