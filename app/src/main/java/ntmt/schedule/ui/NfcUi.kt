package ntmt.schedule.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ntmt.schedule.nfc.NfcHostService
import ntmt.schedule.nfc.NfcShare

/** Способ передачи: QR-коды через камеру или NFC касанием */
internal enum class Transfer { Qr, Nfc }

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Есть ли у телефона NFC для нужного направления. Без NFC выбор не показывается, сразу открываются QR-коды */
internal fun nfcPossible(ctx: Context, sending: Boolean): Boolean =
    runCatching { if (sending) NfcShare.canSend(ctx) else NfcShare.canReceive(ctx) }.getOrDefault(false)

@Composable
private fun rememberNfcOn(): Boolean {
    val ctx = LocalContext.current
    val life = LocalLifecycleOwner.current
    var on by remember { mutableStateOf(NfcShare.enabled(ctx)) }
    DisposableEffect(life) {
        val watch = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) on = NfcShare.enabled(ctx)
        }
        life.lifecycle.addObserver(watch)
        onDispose { life.lifecycle.removeObserver(watch) }
    }
    return on
}

private fun openNfcSettings(ctx: Context) {
    runCatching { ctx.startActivity(Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { runCatching { ctx.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
}

@Composable
internal fun TransferPickDialog(title: String, sending: Boolean, onPick: (Transfer) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val nfcOn = rememberNfcOn()
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Выберите способ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                )
                TransferOption(
                    icon = { Icon(if (sending) Icons.Outlined.QrCode else Icons.Outlined.QrCodeScanner, contentDescription = null) },
                    title = "QR-коды",
                    hint = if (sending) "Показать коды на экране, другой телефон сканирует их камерой" else "Сканировать коды камерой с экрана другого телефона",
                    onClick = { onPick(Transfer.Qr) },
                )
                TransferOption(
                    icon = { Icon(Icons.Outlined.Nfc, contentDescription = null) },
                    title = "NFC",
                    hint = if (nfcOn) "Приложить телефоны друг к другу спинками" else "NFC выключен. Нажмите, чтобы включить",
                    onClick = { if (nfcOn) onPick(Transfer.Nfc) else openNfcSettings(ctx) },
                )
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text("Отмена") }
                }
            }
        }
    }
}

@Composable
private fun TransferOption(icon: @Composable () -> Unit, title: String, hint: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Row(
            Modifier.clickable(onClick = onClick).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { icon() }
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Отдающий телефон: держит экран включённым и просит систему направлять NFC в наш сервис */
@Composable
internal fun NfcSendPanel(ready: Boolean) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val nfcOn = rememberNfcOn()
    val sent by NfcShare.sent.collectAsState()
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        val activity = ctx.findActivity()
        val emulation = runCatching { CardEmulation.getInstance(NfcAdapter.getDefaultAdapter(ctx)) }.getOrNull()
        val service = ComponentName(ctx, NfcHostService::class.java)
        if (activity != null) runCatching { emulation?.setPreferredService(activity, service) }
        onDispose {
            view.keepScreenOn = false
            if (activity != null) runCatching { emulation?.unsetPreferredService(activity) }
            NfcShare.clear()
        }
    }
    val fraction by animateFloatAsState(sent, tween(300, easing = FastOutSlowInEasing), label = "nfc-sent")
    NfcFrame(
        fraction = fraction,
        text = when {
            !nfcOn -> "NFC выключен"
            !ready -> "Подготовка…"
            sent >= 1f -> "Отправлено"
            sent > 0f -> "Передача ${(sent * 100).toInt()}%. Держите телефоны рядом"
            else -> "Приложите телефоны спинками. На другом: «Получить» → NFC"
        },
        pulse = ready && nfcOn && sent < 1f,
    )
    if (!nfcOn) {
        TextButton(onClick = { openNfcSettings(ctx) }) { Text("Включить NFC") }
    }
}

/**
 * Принимающий телефон: включает режим ридера, пока виден экран. [onData] получает
 * распакованные данные. Если связь оборвалась, приложить ещё раз: приём продолжится.
 */
@Composable
internal fun NfcReceivePanel(kind: Byte, onData: (ByteArray) -> Unit, onError: (String?) -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val nfcOn = rememberNfcOn()
    val pull = remember(kind) { NfcShare.Pull(kind) }
    var progress by remember { mutableFloatStateOf(0f) }
    var touching by remember { mutableStateOf(false) }
    val dataState = rememberUpdatedState(onData)
    val errorState = rememberUpdatedState(onError)
    DisposableEffect(nfcOn) {
        val activity = ctx.findActivity()
        val adapter = NfcAdapter.getDefaultAdapter(ctx)
        if (activity == null || adapter == null || !nfcOn) return@DisposableEffect onDispose {}
        view.keepScreenOn = true
        val main = Handler(Looper.getMainLooper())
        val callback = NfcAdapter.ReaderCallback { tag ->
            val dep = IsoDep.get(tag) ?: return@ReaderCallback
            main.post { touching = true; errorState.value(null) }
            try {
                val bytes = pull.read(dep) { p -> main.post { progress = p } }
                main.post {
                    touching = false
                    if (bytes != null) dataState.value(bytes) else errorState.value("Связь прервалась. Приложите телефоны ещё раз, приём продолжится")
                }
            } catch (e: IllegalStateException) {
                main.post { touching = false; errorState.value(e.message) }
            }
        }
        val flags = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        val extras = Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250) }
        runCatching { adapter.enableReaderMode(activity, callback, flags, extras) }
            .onFailure { errorState.value("Не удалось включить NFC") }
        onDispose {
            view.keepScreenOn = false
            runCatching { adapter.disableReaderMode(activity) }
        }
    }
    val fraction by animateFloatAsState(progress, tween(300, easing = FastOutSlowInEasing), label = "nfc-got")
    NfcFrame(
        fraction = fraction,
        text = when {
            !nfcOn -> "NFC выключен"
            touching -> "Приём ${(progress * 100).toInt()}%. Держите телефоны рядом"
            progress > 0f -> "Принято ${(progress * 100).toInt()}%. Приложите ещё раз"
            else -> "Приложите телефон спинкой к другому, где открыто «Поделиться» → NFC"
        },
        pulse = nfcOn,
    )
    if (!nfcOn) {
        TextButton(onClick = { openNfcSettings(ctx) }) { Text("Включить NFC") }
    }
}

@Composable
private fun NfcFrame(fraction: Float, text: String, pulse: Boolean) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val fill = MaterialTheme.colorScheme.primary
    val wave = rememberInfiniteTransition(label = "nfc-wave")
    val beat by wave.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nfc-beat",
    )
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(200.dp).padding(8.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 6.dp.toPx()
                drawArc(track, 0f, 360f, false, style = Stroke(stroke))
                if (fraction > 0f) {
                    drawArc(fill, -90f, 360f * fraction.coerceIn(0f, 1f), false, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(112.dp).scale(if (pulse) beat else 1f),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Outlined.Nfc,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(56.dp).alpha(if (pulse) 1f else 0.6f),
                    )
                }
            }
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp).fillMaxWidth(),
        )
    }
}
