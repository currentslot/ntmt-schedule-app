package ntmt.schedule.ui

import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ntmt.schedule.WidgetOpen
import ntmt.schedule.data.CabinetApi
import ntmt.schedule.data.CabinetVault

/**
 * Вход в кабинет там, где он нужен вне вкладки «Кабинет» (резервная копия).
 * Если настроен быстрый вход, сразу спрашивает отпечаток/код; если нет — [quick] = false,
 * и экран показывает кнопку перехода на страницу входа ([openLogin]).
 */
internal class CabinetQuickLogin(
    val quick: Boolean,
    val busy: Boolean,
    val error: String?,
    private val start: (after: () -> Unit) -> Unit,
) {
    /** Выполнить [after] с действующей сессией: сразу, после быстрого входа или никогда (нет быстрого входа) */
    fun withSession(after: () -> Unit) {
        if (CabinetApi.signedIn()) after() else if (quick) start(after)
    }

    /** Всегда спрашивает отпечаток или код, даже если кабинет уже открыт (выбор папок для копии) */
    fun confirm(after: () -> Unit) {
        if (quick) start(after)
    }

    fun openLogin() {
        WidgetOpen.tabs.tryEmit("Cabinet")
    }
}

@Composable
internal fun rememberCabinetQuickLogin(forBackup: Boolean = false): CabinetQuickLogin {
    val ctx = LocalContext.current
    val activity = ctx.findActivity()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val quick = remember { CabinetVault.has(ctx) } && activity != null && CabinetLock.available(activity)

    fun signIn(after: () -> Unit) {
        val pair = CabinetVault.read(ctx)
        if (pair == null) {
            error = "Быстрый вход не найден. Войдите на вкладке «Кабинет»"
            return
        }
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    CabinetApi.login(pair.first, pair.second).also {
                        // Подтверждено отпечатком: копия для фоновой резервной копии кабинета
                        if (forBackup) ntmt.schedule.backup.BackupVault.save(ctx, pair.first, pair.second)
                    }
                }
            }
            busy = false
            result.onSuccess { after() }.onFailure { error = it.message ?: "Не удалось войти" }
        }
    }

    val keyguard = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val next = pending
        pending = null
        if (res.resultCode == RESULT_OK && next != null) signIn(next)
    }

    return CabinetQuickLogin(quick, busy, error) { after ->
        if (busy || activity == null) return@CabinetQuickLogin
        error = null
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            CabinetLock.biometric(activity, { signIn(after) }) { error = it }
        } else {
            val intent = CabinetLock.credentialIntent(activity)
            if (intent == null) {
                error = "На устройстве не включена блокировка экрана"
            } else {
                pending = after
                keyguard.launch(intent)
            }
        }
    }
}
