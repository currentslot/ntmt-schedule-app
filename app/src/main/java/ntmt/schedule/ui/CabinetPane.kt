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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.SideEffect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.IntOffset
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
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Share
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
import ntmt.schedule.data.Downloads
import ntmt.schedule.data.SavedFile
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
fun CabinetPane(active: Boolean = true, home: Int = 0, toSettings: Int = 0, modifier: Modifier = Modifier.fillMaxSize()) {
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
    var savedFiles by remember { mutableStateOf<List<SavedFile>>(emptyList()) }
    var qrFile by remember { mutableStateOf<Pair<java.io.File, Transfer>?>(null) }
    var pickFile by remember { mutableStateOf<java.io.File?>(null) }
    var receiveFile by remember { mutableStateOf<Transfer?>(null) }
    var pickReceive by remember { mutableStateOf(false) }
    val nfcSend = remember { nfcPossible(ctx, sending = true) }
    val nfcReceive = remember { nfcPossible(ctx, sending = false) }
    var notice by remember { mutableStateOf<Notice?>(null) }
    var toastTick by remember { mutableIntStateOf(0) }
    var ask by remember { mutableStateOf<Ask?>(null) }
    var draft by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    var signedAt by remember { mutableLongStateOf(0L) }
    var prompt by remember { mutableIntStateOf(0) }
    var background by remember { mutableStateOf(false) }
    val cabinetPrefs = remember { ntmt.schedule.data.Prefs(ctx) }
    val scroll = rememberScrollState()
    var settingsAt by remember { mutableIntStateOf(-1) }
    var avatar by remember { mutableIntStateOf(CabinetAvatar.get(ctx)) }
    var pickAvatar by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf<SavedFile?>(null) }
    var askForget by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf<SavedFile?>(null) }
    // Из «Настроек»: на главную страницу кабинета и плавно к блоку настроек
    LaunchedEffect(toSettings) {
        if (toSettings == 0) return@LaunchedEffect
        downloads = false
        files = null
        pages = emptyList()
        crumbs = emptyList()
        ask = null
        val at = withTimeoutOrNull(3_000) { snapshotFlow { settingsAt }.first { it >= 0 } } ?: return@LaunchedEffect
        delay(120)
        scroll.animateScrollTo(at, tween(700, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(home) {
        if (home == 0) return@LaunchedEffect
        downloads = false
        files = null
        pages = emptyList()
        crumbs = emptyList()
        ask = null
        error = null
    }
    fun savedText(file: SavedFile): String =
        if (file.doc != null) "Скачано в «${Downloads.folderName(ctx) ?: "папку"}»: ${file.name}" else "Скачано: ${file.name}"
    fun flash(text: String, toFolder: Boolean = false) {
        toastTick += 1
        notice = Notice(text, toFolder)
    }
    val keyguard = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val next = pending
        pending = null
        if (res.resultCode == RESULT_OK) next?.invoke()
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

    fun signIn(user: String, pass: String, store: Boolean, fromVault: Boolean = false) {
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
                    } else if (fromVault) {
                        saved = true
                        login = user.trim()
                        password = ""
                    } else {
                        // Ключ хранилища открывается только после подтверждения
                        unlockThen {
                            if (CabinetVault.save(ctx, user.trim(), pass)) {
                                saved = true
                                login = user.trim()
                                password = ""
                            } else {
                                error = "Не удалось запомнить вход"
                                saved = false
                                rememberMe = false
                            }
                        }
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
                signIn(pair.first, pair.second, true, fromVault = true)
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

    fun reloadSaved() {
        scope.launch { savedFiles = withContext(Dispatchers.IO) { runCatching { Downloads.list(ctx) }.getOrDefault(emptyList()) } }
    }

    fun mimeOf(name: String): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase()) ?: "application/octet-stream"

    fun openSaved(file: SavedFile) {
        val uri = runCatching { Downloads.shareUri(ctx, file) }.getOrElse {
            error = "Файл не найден"
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mimeOf(file.name)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            ctx.startActivity(Intent.createChooser(intent, file.name))
        } catch (_: ActivityNotFoundException) {
            error = "Нет приложения для этого файла"
        }
    }

    fun shareSaved(file: SavedFile) {
        val mime = mimeOf(file.name)
        val uri = runCatching { Downloads.shareUri(ctx, file) }.getOrElse {
            error = "Файл не найден"
            return
        }
        // ClipData обязательна: без неё панель «Поделиться» Android 10+ не передаёт доступ к файлу
        // приложениям вроде Files by Google, и они не показываются среди вариантов
        val send = Intent(Intent.ACTION_SEND)
            .setType(mime)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TITLE, file.name)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = android.content.ClipData.newUri(ctx.contentResolver, file.name, uri)
        val chooser = Intent.createChooser(send, file.name).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            ctx.startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            error = "Нечем поделиться"
        }
    }

    // «Сохранить в…»: системный выбор места, в том числе «Загрузки», которые видит Files by Google
    val saveAs = rememberLauncherForActivityResult(SaveAsDocument()) { target ->
        val file = saving
        saving = null
        if (target == null || file == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val input = Downloads.input(ctx, file) ?: error("no input")
                    input.use { i -> ctx.contentResolver.openOutputStream(target, "w")!!.use { o -> i.copyTo(o, 64 * 1024) } }
                }.isSuccess
            }
            if (ok) flash("Сохранено: ${file.name}") else error = "Не удалось сохранить файл"
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
                        val where = next.dropLast(1).map { it.name }
                        val stored = withContext(Dispatchers.IO) { runCatching { Downloads.store(ctx, hit.file, where) } }
                        reloadSaved()
                        stored.onSuccess { flash(savedText(it), toFolder = true) }
                            .onFailure { error = it.message ?: "Не удалось сохранить файл" }
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
        val where = crumbs.map { it.name }
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
            val before = withContext(Dispatchers.IO) { runCatching { Downloads.findSame(ctx, name, where) }.getOrNull() }
            if (before != null) {
                if (ticket === current) {
                    ticket = null
                    loadingUrl = null
                    loadingName = null
                    CabinetTransfer.clear()
                }
                Notify.fileClear(ctx)
                reloadSaved()
                flash("Файл уже скачан", toFolder = true)
                return@launch
            }
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
                        val stored = withContext(Dispatchers.IO) { runCatching { Downloads.store(ctx, hit.file, where) } }
                        reloadSaved()
                        stored.onSuccess { got ->
                            flash(savedText(got), toFolder = true)
                            if (leftApp) Notify.fileDone(ctx, got.name) else Notify.fileClear(ctx)
                        }.onFailure {
                            Notify.fileClear(ctx)
                            error = it.message ?: "Не удалось сохранить файл"
                        }
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
        // Снизу такой же отступ, как между строками списка
        Modifier.fillMaxSize().verticalScroll(scroll).padding(bottom = 12.dp),
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
        val cabinetBack = downloads || (shown != null && page != null)
        BackHandler(enabled = cabinetBack) { leaveFolder() }
        val screen: CabScreen = when {
            downloads -> CabScreen.Downloads
            shown != null && page != null -> CabScreen.Folder(page, crumbs)
            shown != null -> CabScreen.Home(shown)
            saved && !other -> CabScreen.Quick
            else -> CabScreen.Login
        }
        // Переходы между экранами: вглубь — сдвиг влево, назад — вправо, остальное — плавная смена
        AnimatedContent(
            targetState = screen,
            contentKey = { it.key },
            transitionSpec = {
                val from = initialState.depth
                val to = targetState.depth
                // Новый экран въезжает и проявляется за то же время, что старый уезжает
                val move = tween<IntOffset>(340, easing = FastOutSlowInEasing)
                val show = tween<Float>(260, delayMillis = 80)
                val hide = tween<Float>(180)
                when {
                    to > from -> (slideInHorizontally(move) { it / 3 } + fadeIn(show)) togetherWith (slideOutHorizontally(move) { -it / 3 } + fadeOut(hide))
                    to < from -> (slideInHorizontally(move) { -it / 3 } + fadeIn(show)) togetherWith (slideOutHorizontally(move) { it / 3 } + fadeOut(hide))
                    else -> fadeIn(show) togetherWith fadeOut(hide)
                } using SizeTransform(clip = false) { _, _ -> tween(340, easing = FastOutSlowInEasing) }
            },
            label = "cabinet-screen",
        ) { target ->
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (target) {
        CabScreen.Downloads -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { downloads = false }) { Icon(Icons.Outlined.ChevronLeft, "Назад") }
                Text("Скачанные файлы", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            OutlinedButton(
                onClick = { if (nfcReceive) pickReceive = true else receiveFile = Transfer.Qr },
                modifier = Modifier.fillMaxWidth().height(CAB_SECONDARY),
                shape = CabShape,
            ) {
                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Получить файл")
            }
            if (savedFiles.isEmpty()) {
                Text("Нет скачанных файлов", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            savedFiles.forEach { file ->
                Surface(
                    shape = CabShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(start = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Description, contentDescription = null)
                        Text(
                            file.name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp).clickable { openSaved(file) },
                        )
                        RowAction(Icons.Outlined.Upload, if (nfcSend) "Передать по QR или NFC" else "Передать через QR") {
                            scope.launch {
                                val local = withContext(Dispatchers.IO) { Downloads.localCopy(ctx, file) }
                                when {
                                    local == null -> error = "Не удалось прочитать файл"
                                    nfcSend -> pickFile = local
                                    else -> qrFile = local to Transfer.Qr
                                }
                            }
                        }
                        RowAction(Icons.Outlined.Share, "Поделиться") { shareSaved(file) }
                        Box {
                            var menu by remember { mutableStateOf(false) }
                            RowAction(Icons.Outlined.MoreVert, "Ещё") { menu = true }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Сохранить в…") },
                                    leadingIcon = { Icon(Icons.Outlined.SaveAlt, contentDescription = null) },
                                    onClick = {
                                        menu = false
                                        saving = file
                                        saveAs.launch(file.name to mimeOf(file.name))
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Удалить") },
                                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                                    onClick = {
                                        menu = false
                                        askDelete = file
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
        }
        is CabScreen.Folder -> {
            // Пока экран уезжает, он показывает свой снимок; та же папка берёт свежий список
            val page = files?.takeIf { it.url == target.page.url } ?: target.page
            // Заголовок и путь тоже из снимка, иначе уезжающий экран сразу показывает новую папку
            val trail = target.crumbs
            fun clean(url: String) = url.trim().trimEnd('/').substringBefore("#")
            val here = clean(page.url)
            val parent = page.up?.let(::clean)
            val been = trail.map { it.name.trim() }.filter { it.isNotEmpty() }.toSet()
            val beenUrl = trail.map { clean(it.url) }.toSet()
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
                    trail.lastOrNull()?.name ?: page.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedButton(
                onClick = {
                    reloadSaved()
                    downloads = true
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(CAB_SECONDARY),
                shape = CabShape,
            ) { Text("Скачанные файлы") }
            Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                trail.forEachIndexed { index, crumb ->
                    if (index > 0) {
                        Text(" / ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        crumb.name,
                        color = if (index == trail.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                        // Пока файл скачивается, действия с папкой запрещены: сервер ответит только после загрузки
                        enabled = !busy && loadingUrl == null,
                        modifier = Modifier.weight(1f).height(CAB_SECONDARY),
                        shape = CabShape,
                    ) {
                        Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Папка")
                    }
                    OutlinedButton(
                        onClick = { pick.launch(arrayOf("*/*")) },
                        enabled = !busy && loadingUrl == null,
                        modifier = Modifier.weight(1f).height(CAB_SECONDARY),
                        shape = CabShape,
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
                    shape = CabShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth().clip(CabShape).clickable(enabled = !busy && (entry.folder || loadingUrl == null)) {
                        if (entry.folder) {
                            loadFolder(entry.name, entry.url, trail + Crumb(entry.name, entry.url))
                        } else {
                            saveRemote(entry.url, entry.name, entry.detail)
                        }
                    },
                ) {
                    Column {
                        Row(Modifier.padding(start = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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
                                RowAction(Icons.Outlined.Download, "Скачать", enabled = !busy && loadingUrl == null) {
                                    saveRemote(entry.url, entry.name, entry.detail)
                                }
                            }
                            if (page.manage) {
                                RowAction(Icons.Outlined.Edit, "Переименовать", enabled = !busy && loadingUrl == null) {
                                    draft = entry.name
                                    ask = Ask.Rename(entry.name, entry.folder)
                                }
                                RowAction(Icons.Outlined.Delete, "Удалить", enabled = !busy && loadingUrl == null) {
                                    ask = Ask.Remove(entry.name, entry.folder)
                                }
                            }
                            Spacer(Modifier.width(4.dp))
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
        }
        is CabScreen.Home -> {
            val shown = target.profile
            Surface(
                shape = CabShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = { pickAvatar = true },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    CabinetAvatar.icon(avatar),
                                    contentDescription = "Сменить значок профиля",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Column {
                            Text(shown.name, style = MaterialTheme.typography.titleMedium)
                            Text(shown.login, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = { loadFolder("Мои файлы", SHARE_ROOT, listOf(Crumb("Мои файлы", SHARE_ROOT))) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(CAB_PRIMARY),
                        shape = CabShape,
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
                        modifier = Modifier.fillMaxWidth().height(CAB_SECONDARY),
                        shape = CabShape,
                    ) { Text("Скачанные файлы") }
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
                modifier = Modifier.fillMaxWidth().height(CAB_SECONDARY),
                shape = CabShape,
            ) { Text("Выйти") }
            if (saved) {
                TextButton(onClick = { askForget = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Забыть вход") }
            }
        }
        CabScreen.Quick -> {
            Surface(
                shape = CabShape,
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
                                    signIn(pair.first, pair.second, true, fromVault = true)
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(CAB_PRIMARY),
                        shape = CabShape,
                    ) { Text(if (busy) "Вход…" else "Быстрый вход") }
                    OutlinedButton(
                        onClick = {
                            reloadSaved()
                            downloads = true
                        },
                        modifier = Modifier.fillMaxWidth().height(CAB_SECONDARY),
                        shape = CabShape,
                    ) { Text("Скачанные файлы") }
                    TextButton(onClick = { other = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Другое имя") }
                }
            }
        }
        CabScreen.Login -> {
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
                modifier = Modifier.fillMaxWidth().height(CAB_PRIMARY),
                shape = CabShape,
            ) { Text(if (busy) "Вход…" else "Войти") }
            OutlinedButton(
                onClick = {
                    reloadSaved()
                    downloads = true
                },
                modifier = Modifier.fillMaxWidth().height(CAB_SECONDARY),
                shape = CabShape,
            ) { Text("Скачанные файлы") }
            if (saved) {
                TextButton(onClick = { other = false }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Быстрый вход") }
            }
        }
        }
        }
        }
        // Настройки файлов живут на главной странице кабинета и доступны без входа
        val prefState = cabinetPrefs.state.collectAsState(initial = null).value
        if (!downloads && files == null && prefState != null) {
            Column(Modifier.onGloballyPositioned { settingsAt = it.positionInParent().y.toInt() }) {
                DownloadsBlock(prefState.backup)
                BackupBlock(prefState.backup, cabinetPrefs, vaultKey = saved, inCabinet = true)
            }
        } else {
            SideEffect { settingsAt = -1 }
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
    if (askForget) {
        AlertDialog(
            onDismissRequest = { askForget = false },
            title = { Text("Забыть вход?") },
            text = { Text("Быстрый вход и фоновые резервные копии кабинета отключатся. Чтобы вернуть их, войдите снова с галочкой «Запомнить»") },
            confirmButton = {
                TextButton(onClick = {
                    askForget = false
                    CabinetVault.clear(ctx)
                    saved = false
                    rememberMe = false
                }) { Text("Забыть") }
            },
            dismissButton = { TextButton(onClick = { askForget = false }) { Text("Отмена") } },
        )
    }
    val deleting = askDelete
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { askDelete = null },
            title = { Text("Удалить файл?") },
            text = { Text(deleting.name) },
            confirmButton = {
                TextButton(onClick = {
                    askDelete = null
                    scope.launch {
                        withContext(Dispatchers.IO) { Downloads.remove(ctx, deleting) }
                        reloadSaved()
                    }
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { askDelete = null }) { Text("Отмена") } },
        )
    }
    if (pickAvatar) {
        AlertDialog(
            onDismissRequest = { pickAvatar = false },
            title = { Text("Значок профиля") },
            text = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CabinetAvatar.all.forEachIndexed { index, icon ->
                        val chosen = index == avatar
                        Surface(
                            onClick = {
                                avatar = index
                                CabinetAvatar.set(ctx, index)
                                pickAvatar = false
                            },
                            shape = CircleShape,
                            color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = "Значок ${index + 1}",
                                    tint = if (chosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickAvatar = false }) { Text("Закрыть") } },
        )
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
    val picking = pickFile
    if (picking != null) {
        TransferPickDialog("Передать файл", sending = true, onPick = { pickFile = null; qrFile = picking to it }) { pickFile = null }
    }
    if (pickReceive) {
        TransferPickDialog("Получить файл", sending = false, onPick = { pickReceive = false; receiveFile = it }) { pickReceive = false }
    }
    val sending = qrFile
    if (sending != null) FileShareDialog(sending.first, sending.second) { qrFile = null }
    val receiving = receiveFile
    if (receiving != null) {
        FileReceiveDialog(
            method = receiving,
            onClose = { receiveFile = null },
            onSaved = { name ->
                reloadSaved()
                flash("Файл получен: $name")
            },
        )
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

/** Экран вкладки «Кабинет» со снимком данных, нужным пока экран уезжает при переходе */
private sealed class CabScreen(val key: String, val depth: Int) {
    data object Login : CabScreen("login", 0)
    data object Quick : CabScreen("quick", 0)
    class Home(val profile: CabinetProfile) : CabScreen("home", 1)
    data object Downloads : CabScreen("downloads", 2)
    class Folder(val page: SharePage, val crumbs: List<Crumb>) : CabScreen("folder:" + page.url.trimEnd('/'), 2 + crumbs.size)
}

// Одинаковые размеры кнопок по всему кабинету
private val CAB_PRIMARY = 52.dp
private val CAB_SECONDARY = 48.dp
private val CabShape = RoundedCornerShape(16.dp)

/** Значок профиля в кабинете: пять вариантов на выбор, выбор хранится на телефоне */
private object CabinetAvatar {
    val all = listOf(
        Icons.Outlined.Person,
        Icons.Outlined.Face,
        Icons.Outlined.School,
        Icons.Outlined.Engineering,
        Icons.Outlined.RocketLaunch,
    )

    fun icon(index: Int) = all.getOrElse(index) { all.first() }

    fun get(ctx: android.content.Context): Int =
        ctx.getSharedPreferences("cabinet_ui", android.content.Context.MODE_PRIVATE).getInt("avatar", 0).coerceIn(0, all.lastIndex)

    fun set(ctx: android.content.Context, index: Int) {
        ctx.getSharedPreferences("cabinet_ui", android.content.Context.MODE_PRIVATE).edit().putInt("avatar", index).apply()
    }
}

/** Компактная кнопка действия в строке файла: 36 dp вместо стандартных 48 */
@Composable
private fun RowAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
    }
}

/** Создать файл в выбранном месте: имя и тип передаются при запуске */
private class SaveAsDocument : androidx.activity.result.contract.ActivityResultContract<Pair<String, String>, android.net.Uri?>() {
    override fun createIntent(context: android.content.Context, input: Pair<String, String>): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.second)
            .putExtra(Intent.EXTRA_TITLE, input.first)

    override fun parseResult(resultCode: Int, intent: Intent?): android.net.Uri? =
        if (resultCode == android.app.Activity.RESULT_OK) intent?.data else null
}
