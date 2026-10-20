package ntmt.schedule.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ntmt.schedule.data.BackupState
import ntmt.schedule.data.Downloads

/** Куда сохранять файлы, скачанные из кабинета и полученные по QR/NFC */
@Composable
internal fun DownloadsBlock(backup: BackupState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf<String?>(null) }
    val raw = remember(tick) { Downloads.folderUri(ctx) }
    val reachable = remember(tick) { raw != null && Downloads.folder(ctx) != null }
    val name = remember(tick) { Downloads.folderName(ctx) }

    fun release(old: String) {
        if (old == backup.dest || old in backup.dirs) return
        runCatching {
            ctx.contentResolver.releasePersistableUriPermission(
                Uri.parse(old),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            ctx.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        val old = Downloads.folderUri(ctx)
        Downloads.setFolder(ctx, uri.toString())
        if (old != null && old != uri.toString()) release(old)
        tick++
        scope.launch {
            // Уже скачанные файлы из кэша переносятся в новую папку
            val moved = withContext(Dispatchers.IO) { runCatching { Downloads.moveCacheToFolder(ctx) }.getOrDefault(0) }
            note = if (moved > 0) "Перенесено в папку: $moved" else null
        }
    }

    SettingsBlock("Скачанные файлы") {
        SettingsTap(
            title = "Папка для скачанных",
            hint = when {
                raw == null -> "Не выбрана: файлы хранятся в кэше приложения. Система может очистить кэш, если мало места"
                !reachable -> "«$name» недоступна, файлы сохраняются в кэш. Выберите папку заново"
                else -> note?.let { "$name · $it" } ?: "$name · файлы из кабинета и полученные по QR/NFC"
            },
            trailing = {
                if (raw != null) {
                    IconButton(onClick = {
                        Downloads.setFolder(ctx, null)
                        release(raw)
                        note = null
                        tick++
                    }) { Icon(Icons.Outlined.Close, "Хранить в кэше", tint = MaterialTheme.colorScheme.primary) }
                } else {
                    Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            },
            onClick = { pick.launch(null) },
        )
    }
}
