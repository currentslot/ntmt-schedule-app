package ntmt.schedule.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ntmt.schedule.data.CabinetApi
import ntmt.schedule.data.CabinetFolder
import ntmt.schedule.data.SharePage

/**
 * Выбор папок кабинета для резервной копии: галочки у папок, вход в папку по нажатию.
 * Сам корень «Мои файлы» выбрать нельзя, только папки внутри него.
 */
@Composable
internal fun CabinetFolderPicker(
    chosen: List<CabinetFolder>,
    onDone: (List<CabinetFolder>) -> Unit,
    onClose: () -> Unit,
) {
    // Выбор папок всегда подтверждается отпечатком или кодом: после этого копия может входить фоном
    var signed by remember { mutableStateOf(false) }
    val login = rememberCabinetQuickLogin(forBackup = true)
    LaunchedEffect(Unit) {
        if (login.quick) login.confirm { signed = true }
    }
    var stack by remember { mutableStateOf<List<Pair<String, SharePage?>>>(listOf("" to null)) }
    var want by remember { mutableStateOf(CabinetApi.SHARE_ROOT) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var picked by remember { mutableStateOf(chosen.associateBy { it.url.trimEnd('/') }) }

    LaunchedEffect(want, signed) {
        if (!signed) return@LaunchedEffect
        if (stack.last().second?.url == want) return@LaunchedEffect
        loading = true
        error = null
        val result = withContext(Dispatchers.IO) { runCatching { CabinetApi.files(want) } }
        loading = false
        result.onSuccess { page -> stack = stack.dropLast(1) + (stack.last().first to page) }
            .onFailure { error = it.message ?: "Кабинет не ответил" }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
        ) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 12.dp)) {
                val depth = stack.size
                val path = stack.drop(1).joinToString("/") { it.first }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (depth > 1) {
                        IconButton(onClick = {
                            stack = stack.dropLast(1)
                            want = stack.last().second?.url ?: CabinetApi.SHARE_ROOT
                            error = null
                        }) { Icon(Icons.Outlined.ChevronLeft, contentDescription = "Назад") }
                    }
                    Column(Modifier.padding(start = if (depth > 1) 0.dp else 8.dp).weight(1f)) {
                        Text("Папки для резервной копии", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (path.isEmpty()) "Мои файлы" else "Мои файлы/$path",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
                Column(
                    Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 420.dp).verticalScroll(rememberScrollState()).padding(top = 8.dp),
                ) {
                    val page = stack.last().second
                    when {
                        !signed -> SignInNeeded(
                            login = login,
                            onQuick = { login.confirm { signed = true } },
                            onLogin = {
                                login.openLogin()
                                onClose()
                            },
                        )
                        error != null -> Hint(error ?: "")
                        page == null -> Unit
                        else -> {
                            val folders = page.entries.filter { it.folder && CabinetApi.inShare(it.url) }
                            if (folders.isEmpty()) Hint("Здесь нет папок")
                            folders.forEach { entry ->
                                val key = entry.url.trimEnd('/')
                                val itemPath = if (path.isEmpty()) entry.name else "$path/${entry.name}"
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = key in picked,
                                        onCheckedChange = { on ->
                                            picked = if (on) picked + (key to CabinetFolder(entry.url, page.url, itemPath)) else picked - key
                                        },
                                    )
                                    Row(
                                        Modifier.weight(1f).clickable {
                                            stack = stack + (entry.name to null)
                                            want = entry.url
                                        }.padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            entry.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Открыть", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Выбрано: ${picked.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp).weight(1f),
                    )
                    TextButton(onClick = onClose) { Text("Отмена") }
                    TextButton(onClick = { onDone(picked.values.sortedBy { it.path.lowercase() }) }) { Text("Готово") }
                }
            }
        }
    }
}

/** Кабинет не открыт: кнопка быстрого входа или переход на страницу входа */
@Composable
internal fun SignInNeeded(login: CabinetQuickLogin, onQuick: () -> Unit, onLogin: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            when {
                login.busy -> "Вход в кабинет…"
                login.quick -> "Подтвердите вход отпечатком или кодом. После этого копии папок кабинета создаются фоном без подтверждения"
                else -> "Чтобы увидеть папки, войдите в кабинет"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        login.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
        if (login.busy) {
            CircularProgressIndicator(Modifier.padding(top = 12.dp).size(24.dp), strokeWidth = 2.dp)
        } else if (login.quick) {
            Button(onClick = onQuick, modifier = Modifier.padding(top = 12.dp)) {
                Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Быстрый вход", modifier = Modifier.padding(start = 8.dp))
            }
        } else {
            Button(onClick = onLogin, modifier = Modifier.padding(top = 12.dp)) {
                Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Перейти ко входу", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
