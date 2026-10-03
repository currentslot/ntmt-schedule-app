package ntmt.schedule.ui

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
                AppUpdate.check(st.updateSource, st.updateChannel, versionCode)
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

    LaunchedEffect(st.updateChannel, st.updateSource) { runCheck() }
    LaunchedEffect(fetch.done, fetch.error) {
        if (fetch.done) refreshSaved()
        if (!fetch.error.isNullOrBlank()) Toast.makeText(ctx, fetch.error, Toast.LENGTH_LONG).show()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = { Text("Обновления") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { pad ->
    Column(
        Modifier
            .padding(pad)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            "Установлено $versionName",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        )

        Text("Источник", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
            Column {
                SourceRow(
                    selected = st.updateSource == AppUpdate.SOURCE_GITHUB,
                    title = "GitHub",
                    icon = {
                        Icon(
                            painterResource(R.drawable.ic_github),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = { scope.launch { prefs.setUpdateSource(AppUpdate.SOURCE_GITHUB) } },
                )
                SourceRow(
                    selected = st.updateSource != AppUpdate.SOURCE_GITHUB,
                    title = "Сервер разработчика (если GitHub недоступен)",
                    icon = null,
                    onClick = { scope.launch { prefs.setUpdateSource(AppUpdate.SOURCE_SERVER) } },
                )
            }
        }

        Text("Канал", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 8.dp))
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = st.updateChannel == "stable",
                onClick = { scope.launch { prefs.setUpdateChannel("stable") } },
                label = { Text("Stable") },
            )
            FilterChip(
                selected = st.updateChannel != "stable",
                onClick = { scope.launch { prefs.setUpdateChannel("beta") } },
                label = { Text("βeta") },
            )
        }
        if (st.updateChannel != "stable") {
            Text(
                "Канал βeta имеет нестабильные пакеты с новыми функциями в раннем доступе. В случае если приложение перестало запускаться, откат можно сделать только перестановкой приложения.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
            )
        }

        Spacer(Modifier.height(8.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
            Column {
                Line("Уведомление об обновлении", "Когда на канале есть более новая сборка", st.updateNotify) {
                    scope.launch { prefs.setUpdateNotify(it) }
                }
                Line("Автопроверка", "При запуске и каждые 6 часов с виджетом. Только сервер разработчика", st.updateAuto) {
                    scope.launch { prefs.setUpdateAuto(it) }
                }
            }
        }

        Button(
            onClick = { runCheck() },
            enabled = !checking && progress < 0,
            modifier = Modifier.padding(top = 16.dp, start = 12.dp),
        ) {
            Text(if (checking) "Проверка…" else "Проверить сейчас")
        }
        if (checking) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp),
            )
        }

        val found = result
        if (!checking && found != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        found.error == AppUpdate.ERR_GITHUB_EMPTY -> {
                            Text("GitHub ещё не подключён", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Страница с ветками пока не задана. Можно взять сборку с другого источника.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        found.error != null -> {
                            Text("Не удалось проверить", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Попробуйте другой источник или повторите позже.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        found.release == null || !found.newer -> {
                            Text("Обновления отсутствуют", style = MaterialTheme.typography.bodyLarge)
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
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Загружено ${AppUpdate.archiveName(ctx, file)}", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${"%.1f".format(file.length() / 1048576f)} МБ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { installFile(ctx, file) }) { Text("Установить") }
                            OutlinedButton(onClick = {
                                AppUpdate.deleteDownloaded(ctx)
                                saved = null
                            }) { Text("Удалить") }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun SourceRow(selected: Boolean, title: String, icon: (@Composable () -> Unit)?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        if (icon != null) {
            icon()
            Spacer(Modifier.size(8.dp))
        }
        Text(title, style = MaterialTheme.typography.bodyLarge)
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
    Text("Доступно ${release.versionName}", style = MaterialTheme.typography.bodyLarge)
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
        OutlinedButton(onClick = onCancel) { Text("Отменить загрузку") }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onInstall) { Text(if (downloaded) "Установить" else "Скачать и установить") }
            if (downloaded) OutlinedButton(onClick = onDelete) { Text("Удалить") }
        }
    }
}
