package ntmt.schedule.ui

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Notifications
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ntmt.schedule.R
import ntmt.schedule.data.PrefState
import ntmt.schedule.data.Prefs
import ntmt.schedule.update.AppRelease
import ntmt.schedule.update.AppUpdate
import ntmt.schedule.update.UpdateCheck
import ntmt.schedule.update.UpdateDownloadService
import ntmt.schedule.update.UpdateFetch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesPane(st: PrefState, prefs: Prefs, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val versionName = remember { AppUpdate.installedName(ctx) }
    val versionCode = remember { AppUpdate.installedCode(ctx) }
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<UpdateCheck?>(null) }
    var progress by remember { mutableIntStateOf(-1) }
    var saved by remember { mutableStateOf(AppUpdate.sweepInstalled(ctx)) }
    val fetch by UpdateFetch.state.collectAsState()

    fun refreshSaved() {
        saved = AppUpdate.sweepInstalled(ctx)
    }

    DisposableEffect(ctx) {
        val activity = ctx as? ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshSaved()
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    fun runCheck() {
        if (checking) return
        checking = true
        scope.launch {
            val found = withContext(Dispatchers.IO) {
                AppUpdate.check(st.updateChannel, versionCode)
            }
            result = found
            if (found.newer && found.release != null) {
                prefs.setAvailableUpdate(found.release.versionCode)
            } else if (found.error == null) {
                prefs.setAvailableUpdate(0)
            }
            checking = false
        }
    }

    LaunchedEffect(st.updateChannel) { runCheck() }
    LaunchedEffect(fetch.done, fetch.error) {
        if (fetch.done) refreshSaved()
        if (!fetch.error.isNullOrBlank()) Toast.makeText(ctx, fetch.error, Toast.LENGTH_LONG).show()
    }

    // Surface задаёт цвет текста по теме: без него в тёмной теме заголовки остались бы чёрными
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TonalIcon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", onClick = onBack)
            ScreenHeader("Обновления", "Установлено $versionName", Modifier.weight(1f))
        }

        SettingsBlock("Канал обновлений") {
            Segmented(
                options = listOf("Stable", "βeta"),
                selected = if (st.updateChannel == "stable") 0 else 1,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            ) { i ->
                val next = if (i == 0) "stable" else "beta"
                scope.launch {
                    prefs.setUpdateChannel(next)
                    // Пуш о новой версии приходит по топику канала
                    ntmt.schedule.notify.Push.syncUpdates(ctx, next, st.updateNotify)
                }
            }
            Text(
                if (st.updateChannel != "stable") {
                    "Канал βeta имеет нестабильные пакеты с новыми функциями в раннем доступе. В случае если приложение перестало запускаться, откат можно сделать только перестановкой приложения."
                } else {
                    "Проверенные сборки. Новые функции появляются позже, чем в βeta."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            )
        }

        SettingsBlock("Проверка") {
            Line("Уведомление об обновлении", "Сервер сообщает, когда на выбранном канале вышла новая сборка. Нужны сервисы Google", st.updateNotify, Icons.Outlined.Notifications) {
                scope.launch {
                    prefs.setUpdateNotify(it)
                    ntmt.schedule.notify.Push.syncUpdates(ctx, st.updateChannel, it)
                }
            }
            Line("Проверять при входе", "При каждом входе в приложение: GitHub, выпуски выбранного канала", st.updateAuto, Icons.Outlined.Autorenew) {
                scope.launch { prefs.setUpdateAuto(it) }
            }
        }

        Button(
            onClick = { runCheck() },
            enabled = !checking && progress < 0,
            shape = Ui.Inner,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).heightIn(min = 48.dp),
        ) {
            Text(if (checking) "Проверка…" else "Проверить сейчас")
        }
        if (checking) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 4.dp, end = 4.dp),
            )
        }

        val found = result
        if (!checking && found != null) {
            BlockCard(Modifier.padding(top = 16.dp).appear(), highlighted = found.newer && found.release != null) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when {
                        found.error == AppUpdate.ERR_GITHUB_EMPTY -> {
                            Text("GitHub ещё не подключён", style = Ui.CardTitle)
                            Text(
                                "Страница с выпусками пока не задана.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        found.error != null -> {
                            Text("Не удалось проверить", style = Ui.CardTitle)
                            Text(
                                "GitHub не ответил. Проверьте интернет и повторите позже.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        found.release == null || !found.newer -> {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconBadge(Icons.Outlined.Check)
                                Text("Обновления отсутствуют", style = Ui.CardTitle)
                            }
                        }
                        else -> {
                            val busy = UpdateFetch.running() && fetch.versionCode == found.release.versionCode && !fetch.done
                            val ready = saved != null && AppUpdate.archiveCode(ctx, saved!!) == found.release.versionCode
                            ReleaseCard(
                                found.release,
                                if (busy) fetch.progress.coerceAtLeast(0) else -1,
                                ready,
                                onInstall = {
                                    if (ready) {
                                        installFile(ctx, saved!!)
                                    } else if (!UpdateFetch.running()) {
                                        UpdateDownloadService.start(ctx, found.release)
                                    }
                                },
                                onCancel = { UpdateDownloadService.cancel(ctx) },
                                onDelete = {
                                    UpdateDownloadService.cancel(ctx)
                                    AppUpdate.deleteDownloaded(ctx)
                                    saved = null
                                },
                            )
                        }
                    }
                }
            }
        }
        saved?.let { file ->
            val waitingInstall = result?.release == null || AppUpdate.archiveCode(ctx, file) != result?.release?.versionCode
            if (waitingInstall && progress < 0) {
                BlockCard(Modifier.padding(top = 16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Загружено ${AppUpdate.archiveName(ctx, file)}", style = Ui.CardTitle)
                        Text(
                            "${"%.1f".format(file.length() / 1048576f)} МБ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { installFile(ctx, file) }, shape = Ui.Inner) { Text("Установить") }
                            OutlinedButton(onClick = {
                                AppUpdate.deleteDownloaded(ctx)
                                saved = null
                            }, shape = Ui.Inner) { Text("Удалить") }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
    }
}

private fun installFile(ctx: android.content.Context, file: File) {
    if (!AppUpdate.canInstall(ctx)) {
        Toast.makeText(ctx, "Разрешите установку из этого приложения", Toast.LENGTH_LONG).show()
        AppUpdate.openInstallPermission(ctx)
    } else {
        AppUpdate.install(ctx, file)
    }
}

@Composable
private fun ReleaseCard(
    release: AppRelease,
    progress: Int,
    downloaded: Boolean,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        val scheme = MaterialTheme.colorScheme
        androidx.compose.foundation.layout.Box(
            Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(scheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_system_update), contentDescription = null, tint = scheme.primary.readable(scheme.onPrimary), modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Доступно обновление", style = MaterialTheme.typography.labelLarge, color = scheme.primary)
            Text(release.versionName, style = Ui.CardTitle)
            if (release.size > 0) {
                Text("${"%.1f".format(release.size / 1048576f)} МБ · канал ${if (release.channel == "stable") "Stable" else "βeta"}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
    }
    if (release.notes.isNotBlank()) {
        Text(
            release.notes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (progress >= 0) {
        LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        Text("Загрузка $progress%", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Скачивание идёт в фоне. Можно выйти из этого экрана.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onCancel, shape = Ui.Inner) { Text("Отменить загрузку") }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onInstall, shape = Ui.Inner, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(if (downloaded) "Установить" else "Скачать и установить") }
            if (downloaded) OutlinedButton(onClick = onDelete, shape = Ui.Inner) { Text("Удалить") }
        }
    }
}
