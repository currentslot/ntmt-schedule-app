package ntmt.schedule.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.outlined.ChecklistRtl
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.outlined.DeleteSweep
import ntmt.schedule.backup.Backup
import ntmt.schedule.data.BackupState
import ntmt.schedule.data.Prefs

/** Блок настроек «Резервная копия»: папка для архивов, что сохранять, как часто и сколько хранить */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BackupBlock(backup: BackupState, prefs: Prefs, vaultKey: Any? = null, inCabinet: Boolean = false) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var enableAfterPick by remember { mutableStateOf(false) }
    var pickCabinet by remember { mutableStateOf(false) }
    var askClear by remember { mutableStateOf(false) }
    var cleared by remember { mutableStateOf<String?>(null) }
    val login = rememberCabinetQuickLogin()
    val life = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var quickReady by remember(vaultKey) { mutableStateOf(ntmt.schedule.data.CabinetVault.has(ctx)) }
    androidx.compose.runtime.DisposableEffect(life) {
        val watch = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) quickReady = ntmt.schedule.data.CabinetVault.has(ctx)
        }
        life.lifecycle.addObserver(watch)
        onDispose { life.lifecycle.removeObserver(watch) }
    }
    val running by remember { WorkManager.getInstance(ctx).getWorkInfosForUniqueWorkFlow(Backup.NOW) }
        .collectAsState(initial = emptyList())
    val busy = running.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }

    // Расписание WorkManager обновляется только после изменения настроек, по свежему состоянию
    fun reschedule(block: suspend () -> Unit) = scope.launch {
        block()
        Backup.schedule(ctx, prefs.state.first().backup)
    }

    fun keep(uri: Uri, write: Boolean) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or (if (write) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
        runCatching { ctx.contentResolver.takePersistableUriPermission(uri, flags) }
    }

    fun release(raw: String) {
        runCatching {
            ctx.contentResolver.releasePersistableUriPermission(
                Uri.parse(raw),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    val pickDest = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val turnOn = enableAfterPick
        enableAfterPick = false
        if (uri == null) return@rememberLauncherForActivityResult
        keep(uri, write = true)
        val old = backup.dest
        reschedule {
            prefs.setBackupDest(uri.toString())
            if (turnOn) prefs.setBackupOn(true)
            if (old != null && old != uri.toString() && old !in backup.dirs && old != ntmt.schedule.data.Downloads.folderUri(ctx)) release(old)
        }
    }
    SettingsBlock("Резервная копия") {
        // Без быстрого входа копии недоступны: фоновый вход берётся из подтверждённого отпечатком быстрого входа
        if (!quickReady) {
            Column(Modifier.padding(12.dp)) {
                Text("Архив учебных файлов", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Доступно только с быстрым входом. Войдите в кабинет с галочкой «Запомнить» и подтвердите отпечатком или кодом",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // В кабинете форма входа и так на этой странице
                if (!inCabinet) {
                    OutlinedButton(onClick = { login.openLogin() }, shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Text("Перейти ко входу")
                    }
                }
            }
            return@SettingsBlock
        }
        Line(
            "Архив учебных файлов",
            "Периодически сохранять скачанные и выбранные папки кабинета в zip. Хранится только последний архив, предыдущие удаляются",
            backup.on,
        ) { on ->
            if (on && backup.dest == null) {
                enableAfterPick = true
                pickDest.launch(null)
            } else {
                if (!on) ntmt.schedule.backup.BackupVault.clear(ctx)
                reschedule { prefs.setBackupOn(on) }
            }
        }
        SettingsTap(
            title = "Куда сохранять архивы",
            hint = backup.dest?.let { Backup.folderName(ctx, it) } ?: "Папка не выбрана",
            trailing = { Icon(Icons.Outlined.FolderZip, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { pickDest.launch(null) },
        )
        if (!backup.on) return@SettingsBlock

        Text("Что сохранять", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp, top = 8.dp))
        Line("Скачанные", "Файлы из «Скачанных файлов» кабинета, разложенные по папкам, как на сервере", backup.saved) {
            scope.launch { prefs.setBackupSaved(it) }
        }
        Line(
            "Папки с сайта кабинета",
            "Выбранные папки «Моих файлов» с old.ntiustu.ru, отдельным архивом. Собирается после входа в кабинет, когда подошёл срок",
            backup.cabinet,
        ) { scope.launch { prefs.setBackupCabinet(it) } }
        if (backup.cabinet) {
            backup.cabinetFolders.forEach { folder ->
                Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Cloud, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        folder.path,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 10.dp).weight(1f),
                    )
                    IconButton(onClick = {
                        reschedule { prefs.setCabinetFolders(backup.cabinetFolders.filter { it.url != folder.url }) }
                    }) { Icon(Icons.Outlined.Close, contentDescription = "Убрать папку") }
                }
            }
            SettingsTap(
                title = "Выбрать папки кабинета",
                hint = if (backup.cabinetFolders.isEmpty()) "Ничего не выбрано. Нужен вход в кабинет" else "Выбрано: ${backup.cabinetFolders.size}",
                trailing = { Icon(Icons.Outlined.ChecklistRtl, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { pickCabinet = true },
            )
        }

        Text("Как часто", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp, top = 8.dp))
        // Кнопки переносятся целиком на следующую строку, а не по словам внутри кнопки
        FlowRow(
            Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(1 to "Каждый день", 7 to "Раз в неделю", 30 to "Раз в месяц").forEach { (days, label) ->
                FilterChip(
                    selected = backup.days == days,
                    onClick = { reschedule { prefs.setBackupDays(days) } },
                    label = { Text(label, maxLines = 1, softWrap = false) },
                )
            }
        }

        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp)) {
            Text(
                "Скачанные: " + (backup.last ?: "архивов ещё не было"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (backup.cabinet) {
                Text(
                    "Кабинет: " + (backup.cabinetLast ?: "архивов ещё не было"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (backup.cabinet && backup.cabinetFolders.isNotEmpty() && !ntmt.schedule.backup.BackupVault.has(ctx)) {
            Text(
                "Чтобы папки кабинета сохранялись фоном, откройте «Выбрать папки кабинета» и подтвердите вход отпечатком",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (busy) {
                OutlinedButton(
                    onClick = { WorkManager.getInstance(ctx).cancelUniqueWork(Backup.NOW) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Отменить копию", maxLines = 1)
                }
            } else {
                OutlinedButton(
                    // Копия идёт фоном со своим входом, подтверждение не нужно
                    onClick = { Backup.runNow(ctx, cabinet = false) },
                    enabled = backup.dest != null,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text("Сделать копию сейчас", maxLines = 1)
                }
            }
        }
        TextButton(
            onClick = { askClear = true },
            enabled = backup.dest != null && !busy,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp),
        ) {
            Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Удалить все копии", color = MaterialTheme.colorScheme.error)
        }
    }
    if (askClear) {
        ClearBackupsDialog(
            onConfirm = {
                askClear = false
                scope.launch {
                    val n = withContext(Dispatchers.IO) { Backup.clearAll(ctx, prefs.state.first().backup) }
                    prefs.clearBackupResults()
                    cleared = if (n == 0) "Архивов не найдено" else "Удалено архивов: $n"
                }
            },
            onClose = { askClear = false },
        )
    }
    cleared?.let { text ->
        LaunchedEffect(text) {
            kotlinx.coroutines.delay(3000)
            cleared = null
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, top = 4.dp))
    }
    if (pickCabinet) {
        CabinetFolderPicker(
            chosen = backup.cabinetFolders,
            onDone = { picked ->
                pickCabinet = false
                reschedule { prefs.setCabinetFolders(picked) }
            },
            onClose = { pickCabinet = false },
        )
    }
}

/** Подтверждение удаления всех архивов: ползунок нужно провести вправо до конца */
@Composable
private fun ClearBackupsDialog(onConfirm: () -> Unit, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        androidx.compose.material3.Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)) {
                Text("Удалить все копии?", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Все архивы NTMT в папке для архивов будут удалены. Это нельзя отменить.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
                )
                SwipeToConfirm("Проведите вправо, чтобы удалить", onConfirm)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text("Отмена") }
                }
            }
        }
    }
}

@Composable
private fun SwipeToConfirm(label: String, onConfirm: () -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val knob = 52.dp
    var width by remember { mutableStateOf(0f) }
    var offset by remember { mutableStateOf(0f) }
    var done by remember { mutableStateOf(false) }
    val knobPx = with(density) { knob.toPx() }
    val max = (width - knobPx).coerceAtLeast(1f)
    val shown by androidx.compose.animation.core.animateFloatAsState(offset, label = "swipe")
    val error = MaterialTheme.colorScheme.error
    androidx.compose.foundation.layout.Box(
        Modifier
            .fillMaxWidth()
            .height(knob)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.errorContainer)
            .onSizeChanged { width = it.width.toFloat() }
            .draggable(
                state = rememberDraggableState { delta -> if (!done) offset = (offset + delta).coerceIn(0f, max) },
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    if (!done && offset >= max * 0.92f) {
                        done = true
                        offset = max
                        onConfirm()
                    } else if (!done) {
                        offset = 0f
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(start = knob).alpha(1f - (shown / max).coerceIn(0f, 1f)),
        )
        androidx.compose.foundation.layout.Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(shown.roundToInt(), 0) }
                .size(knob)
                .padding(4.dp)
                .clip(CircleShape).background(error),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onError)
        }
    }
}
