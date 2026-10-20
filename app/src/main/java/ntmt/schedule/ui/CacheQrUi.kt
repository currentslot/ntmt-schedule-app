package ntmt.schedule.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import ntmt.schedule.data.CacheQr
import ntmt.schedule.data.Catalog
import ntmt.schedule.data.FileQr
import ntmt.schedule.data.GroupWeeks
import ntmt.schedule.data.NtmtApi
import ntmt.schedule.data.ScheduleCache
import ntmt.schedule.nfc.NfcShare
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@Composable
internal fun LinkQrDialog(url: String, onClose: () -> Unit) {
    val image = remember(url) { qrBitmap(url) }
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("GitHub", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                Image(
                    image.asImageBitmap(),
                    contentDescription = "QR-код",
                    // Код хранится точка-в-модуль: без сглаживания он растягивается чётким, а не размытым
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(androidx.compose.ui.graphics.Color.White)
                        .padding(8.dp),
                )
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text("Закрыть") }
                }
            }
        }
    }
}

@Composable
internal fun CacheShareDialog(method: Transfer = Transfer.Qr, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val nfc = method == Transfer.Nfc
    var frames by remember { mutableStateOf<List<String>>(emptyList()) }
    var missing by remember { mutableStateOf(false) }
    var nfcReady by remember { mutableStateOf(false) }
    var bright by remember { mutableStateOf(false) }
    DisposableEffect(bright) {
        val window = (ctx as? Activity)?.window
        if (!bright || window == null) return@DisposableEffect onDispose {}
        val previous = window.attributes.screenBrightness
        window.attributes = window.attributes.apply { screenBrightness = 1f }
        onDispose {
            window.attributes = window.attributes.apply { screenBrightness = previous }
        }
    }
    LaunchedEffect(Unit) {
        var sig = ""
        while (isActive) {
            val file = File(ctx.filesDir, "schedule-cache.json")
            val next = if (!file.exists() || file.length() == 0L) "" else "${file.length()}:${file.lastModified()}"
            if (next != sig) {
                sig = next
                if (next.isEmpty()) {
                    frames = emptyList()
                    missing = true
                } else if (nfc) {
                    val text = withContext(Dispatchers.IO) { file.readText() }
                    nfcReady = withContext(Dispatchers.Default) { NfcShare.offerSchedule(text) }
                    missing = !nfcReady
                } else {
                    val text = withContext(Dispatchers.IO) { file.readText() }
                    val built = withContext(Dispatchers.Default) { CacheQr.frames(text) }
                    missing = built.isEmpty()
                    frames = built
                }
            }
            delay(1200)
        }
    }
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Поделиться расписанием", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                Text(
                    if (nfc) "Передача по NFC. Экран этого телефона должен быть включён." else "Покажите коды другому телефону. Порядок сканирования не важен.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp).fillMaxWidth(),
                )
                when {
                    missing -> Text("Нет сохранённого расписания", modifier = Modifier.padding(vertical = 24.dp))
                    nfc -> NfcSendPanel(nfcReady)
                    frames.isEmpty() -> Text("Подготовка…", modifier = Modifier.padding(vertical = 24.dp))
                    else -> {
                        QrPager(frames)
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Подсветка экрана", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = bright, onCheckedChange = { bright = it })
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text("Закрыть") }
                }
            }
        }
    }
}

@Composable
internal fun CacheReceiveDialog(
    method: Transfer = Transfer.Qr,
    onClose: () -> Unit,
    onImported: (Catalog, Map<String, GroupWeeks>) -> Unit,
) {
    val ctx = LocalContext.current
    val nfc = method == Transfer.Nfc
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val bag = remember { CacheQr.Bag() }
    var got by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var ready by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val fraction by animateFloatAsState(
        targetValue = if (total <= 0) 0f else got.toFloat() / total.toFloat(),
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "scan-ring",
    )
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)) {
                Text(
                    if (ready == null) "Получить расписание" else "Применить расписание?",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    when {
                        ready != null -> scheduleSummary(ready ?: "")
                        nfc -> "Приём по NFC"
                        total == 0 -> "Наведите камеру на код. Жест увеличения приближает."
                        else -> "Принято $got из $total"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                )
                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                }
                if (nfc) {
                    if (ready == null) {
                        NfcReceivePanel(
                            kind = NfcShare.KIND_SCHEDULE,
                            onData = { bytes ->
                                ready = bytes.toString(Charsets.UTF_8)
                                error = null
                            },
                            onError = { error = it },
                        )
                    }
                } else if (!granted) {
                    Text("Чтобы сканировать код, нужен доступ к камере.", textAlign = TextAlign.Start)
                    TextButton(onClick = { ask.launch(Manifest.permission.CAMERA) }) { Text("Разрешить камеру") }
                } else if (ready == null) {
                    ScanFrame(fraction) { text ->
                        if (!bag.accept(text)) return@ScanFrame
                        got = bag.got
                        total = bag.total
                        if (bag.complete() && ready == null) {
                            ready = bag.json()
                            if (ready == null) error = "Не удалось прочитать расписание"
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text(if (ready == null) "Закрыть" else "Отмена") }
                    if (ready != null) {
                        TextButton(
                            enabled = !busy,
                            onClick = {
                                val json = ready ?: return@TextButton
                                busy = true
                                val loaded = ScheduleCache.import(ctx, json)
                                if (loaded == null) {
                                    error = "Расписание повреждено"
                                    busy = false
                                } else {
                                    onImported(loaded.first, loaded.second)
                                    onClose()
                                }
                            },
                        ) { Text("Применить") }
                    }
                }
            }
        }
    }
}

private fun roundWindowPath(w: Float, h: Float, radius: Float, inset: Float): Path {
    val left = inset
    val top = inset
    val right = w - inset
    val bottom = h - inset
    val rad = radius.coerceAtMost(minOf(right - left, bottom - top) / 2f)
    val path = Path()
    path.moveTo((left + right) / 2f, top)
    path.lineTo(right - rad, top)
    path.arcTo(Rect(right - 2 * rad, top, right, top + 2 * rad), -90f, 90f, false)
    path.lineTo(right, bottom - rad)
    path.arcTo(Rect(right - 2 * rad, bottom - 2 * rad, right, bottom), 0f, 90f, false)
    path.lineTo(left + rad, bottom)
    path.arcTo(Rect(left, bottom - 2 * rad, left + 2 * rad, bottom), 90f, 90f, false)
    path.lineTo(left, top + rad)
    path.arcTo(Rect(left, top, left + 2 * rad, top + 2 * rad), 180f, 90f, false)
    path.close()
    return path
}

@Composable
private fun ScanFrame(fraction: Float, onText: (String) -> Unit) {
    val ctx = LocalContext.current
    val life = LocalLifecycleOwner.current
    val zoom = remember { ZoomHold() }
    val onTextState = rememberUpdatedState(onText)
    val previewView = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val track = MaterialTheme.colorScheme.outlineVariant
    val fill = MaterialTheme.colorScheme.primary
    DisposableEffect(previewView) {
        val future = ProcessCameraProvider.getInstance(ctx)
        val exec = Executors.newSingleThreadExecutor()
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            val tick = AtomicInteger(0)
            analysis.setAnalyzer(exec) { image ->
                val text = decodeQr(image, tick.getAndIncrement())
                image.close()
                if (text != null) previewView.post { onTextState.value(text) }
            }
            runCatching {
                provider.unbindAll()
                val camera = provider.bindToLifecycle(life, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                zoom.control = camera.cameraControl
                zoom.max = camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 5f
                camera.cameraControl.setZoomRatio(zoom.ratio.coerceIn(1f, zoom.max))
            }
        }, ContextCompat.getMainExecutor(ctx))
        onDispose {
            zoom.control = null
            exec.shutdown()
            runCatching { ProcessCameraProvider.getInstance(ctx).get().unbindAll() }
        }
    }
    Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp))) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            Box(
                Modifier.fillMaxSize().pointerInput(zoom) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        if (zoomChange == 1f) return@detectTransformGestures
                        val next = (zoom.ratio * zoomChange).coerceIn(1f, zoom.max)
                        if (next == zoom.ratio) return@detectTransformGestures
                        zoom.ratio = next
                        zoom.control?.setZoomRatio(next)
                    }
                },
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 5.dp.toPx()
            val outline = roundWindowPath(size.width, size.height, (22.dp.toPx() - stroke / 2f).coerceAtLeast(0f), stroke / 2f)
            drawPath(outline, track, style = Stroke(stroke, cap = StrokeCap.Butt))
            if (fraction > 0f) {
                val measure = PathMeasure()
                measure.setPath(outline, false)
                val part = Path()
                measure.getSegment(0f, measure.length * fraction.coerceIn(0f, 1f), part, true)
                drawPath(part, fill, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
internal fun FileShareDialog(file: File, method: Transfer = Transfer.Qr, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val nfc = method == Transfer.Nfc
    var frames by remember { mutableStateOf<List<String>>(emptyList()) }
    var tooBig by remember { mutableStateOf(false) }
    var nfcReady by remember { mutableStateOf(false) }
    var bright by remember { mutableStateOf(false) }
    DisposableEffect(bright) {
        val window = (ctx as? Activity)?.window
        if (!bright || window == null) return@DisposableEffect onDispose {}
        val previous = window.attributes.screenBrightness
        window.attributes = window.attributes.apply { screenBrightness = 1f }
        onDispose { window.attributes = window.attributes.apply { screenBrightness = previous } }
    }
    LaunchedEffect(file) {
        if (nfc) {
            nfcReady = withContext(Dispatchers.IO) {
                file.isFile && file.length() <= NfcShare.MAX_FILE && NfcShare.offerFile(file.name, file.readBytes())
            }
            tooBig = !nfcReady
            return@LaunchedEffect
        }
        val built = withContext(Dispatchers.IO) {
            // Заведомо большой файл не читаем целиком: даже сжатый он не влезет в коды
            if (!file.isFile || file.length() > QR_FILE_READ_LIMIT) return@withContext null
            FileQr.frames(file.name, file.readBytes())
        }
        tooBig = built.isNullOrEmpty()
        frames = built.orEmpty()
    }
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Поделиться файлом", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                Text(
                    file.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp).fillMaxWidth(),
                )
                when {
                    tooBig && nfc -> Text("По NFC можно передать файл до 1 МБ", modifier = Modifier.padding(vertical = 24.dp))
                    tooBig -> Text(
                        "Файл не помещается в ${FileQr.MAX_FRAMES} QR-кодов. Передайте его по NFC или через «Поделиться»",
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                    nfc -> NfcSendPanel(nfcReady)
                    frames.isEmpty() -> Text("Подготовка…", modifier = Modifier.padding(vertical = 24.dp))
                    else -> {
                        QrPager(frames)
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Подсветка экрана", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = bright, onCheckedChange = { bright = it })
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text("Закрыть") }
                }
            }
        }
    }
}

@Composable
internal fun FileReceiveDialog(method: Transfer = Transfer.Qr, onClose: () -> Unit, onSaved: (String) -> Unit) {
    val ctx = LocalContext.current
    val nfc = method == Transfer.Nfc
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val bag = remember { FileQr.Bag() }
    var got by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var ready by remember { mutableStateOf<Pair<String, ByteArray>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val fraction by animateFloatAsState(
        targetValue = if (total <= 0) 0f else got.toFloat() / total.toFloat(),
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "file-ring",
    )
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)) {
                val gotFile = ready
                Text(if (gotFile == null) "Получить файл" else "Сохранить файл?", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        gotFile != null -> "${gotFile.first}\n${(gotFile.second.size / 1024).coerceAtLeast(1)} КБ"
                        nfc -> "Приём по NFC"
                        total == 0 -> "Наведите камеру на код. Жест увеличения приближает."
                        else -> "Принято $got из $total"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                )
                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                }
                if (nfc) {
                    if (gotFile == null) {
                        NfcReceivePanel(
                            kind = NfcShare.KIND_FILE,
                            onData = { bytes ->
                                ready = FileQr.unblob(bytes)
                                error = if (ready == null) "Не удалось прочитать файл" else null
                            },
                            onError = { error = it },
                        )
                    }
                } else if (!granted) {
                    Text("Чтобы сканировать код, нужен доступ к камере.")
                    TextButton(onClick = { ask.launch(Manifest.permission.CAMERA) }) { Text("Разрешить камеру") }
                } else if (gotFile == null) {
                    ScanFrame(fraction) { text ->
                        if (!bag.accept(text)) return@ScanFrame
                        got = bag.got
                        total = bag.total
                        if (bag.complete()) {
                            ready = bag.file()
                            if (ready == null) error = "Не удалось прочитать файл"
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text(if (gotFile == null) "Закрыть" else "Отмена") }
                    if (gotFile != null) {
                        TextButton(
                            enabled = !busy,
                            onClick = {
                                busy = true
                                val stored = storeReceived(ctx, gotFile.first, gotFile.second)
                                if (stored == null) {
                                    error = "Не удалось сохранить"
                                    busy = false
                                } else {
                                    onSaved(stored.name)
                                    onClose()
                                }
                            },
                        ) { Text("Сохранить") }
                    }
                }
            }
        }
    }
}

private fun storeReceived(ctx: android.content.Context, name: String, bytes: ByteArray): ntmt.schedule.data.SavedFile? {
    val clean = ntmt.schedule.data.CabinetApi.safeName(name)
    val temp = File(ntmt.schedule.data.Downloads.incoming(ctx), clean)
    return try {
        temp.writeBytes(bytes)
        ntmt.schedule.data.Downloads.store(ctx, temp, null)
    } catch (_: Exception) {
        temp.delete()
        null
    }
}

private class ZoomHold {
    var control: androidx.camera.core.CameraControl? = null
    var ratio = 1f
    var max = 5f
}

private fun scheduleSummary(json: String): String = try {
    val root = JSONObject(json)
    val catalog = root.optJSONObject("catalog")
    val dt = NtmtApi.formatUpdate(catalog?.optString("updateDt").orEmpty())
    val groups = catalog?.optJSONArray("groups")?.length() ?: root.optJSONObject("groups")?.length() ?: 0
    val kb = (json.toByteArray(Charsets.UTF_8).size / 1024).coerceAtLeast(1)
    "Обновлено $dt\nГрупп: $groups · $kb КБ"
} catch (_: Exception) {
    "Расписание получено"
}

private const val QR_FILE_READ_LIMIT = 8L * 1024 * 1024

/**
 * QR-код в размере «1 точка = 1 модуль»: строится за миллисекунды и занимает килобайты,
 * а на экран растягивается без сглаживания. Крупным кодам хватает уровня коррекции L:
 * экран не царапается, а меньшая плотность делает модули крупнее и код читается легче
 */
private fun qrBitmap(text: String): Bitmap {
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to if (text.length > 900) ErrorCorrectionLevel.L else ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1,
        EncodeHintType.CHARACTER_SET to "ISO-8859-1",
    )
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
    val w = matrix.width
    val px = IntArray(w * w)
    for (y in 0 until w) {
        val row = y * w
        for (x in 0 until w) px[row + x] = if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    }
    return Bitmap.createBitmap(px, w, w, Bitmap.Config.ARGB_8888)
}

/**
 * Листает коды по кругу. Коды строятся параллельно, первый показывается сразу, не дожидаясь остальных.
 * Нажатие на код ставит показ на паузу, стрелками можно листать вручную
 */
@Composable
private fun QrPager(frames: List<String>) {
    var images by remember(frames) { mutableStateOf<List<Bitmap?>>(List(frames.size) { null }) }
    var page by remember(frames) { mutableIntStateOf(0) }
    var paused by remember(frames) { mutableStateOf(false) }
    LaunchedEffect(frames) {
        frames.forEachIndexed { i, text ->
            launch {
                val bmp = withContext(Dispatchers.Default) { runCatching { qrBitmap(text) }.getOrNull() }
                images = images.toMutableList().also { it[i] = bmp }
            }
        }
    }
    // Мелкие коды камера ловит быстрее, их можно менять чаще
    val interval = if ((frames.firstOrNull()?.length ?: 0) > 900) 1000L else 750L
    fun step(by: Int) {
        val list = images
        if (list.isEmpty()) return
        var next = page
        repeat(list.size) {
            next = (next + by + list.size) % list.size
            if (list[next] != null) {
                page = next
                return
            }
        }
    }
    LaunchedEffect(frames, paused) {
        if (frames.size < 2 || paused) return@LaunchedEffect
        while (true) {
            delay(interval)
            step(1)
        }
    }
    val frame = images.getOrNull(page)
    Box(
        Modifier
            .size(280.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(androidx.compose.ui.graphics.Color.White)
            .clickable(enabled = frames.size > 1) { paused = !paused }
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (frame != null) {
            Image(
                frame.asImageBitmap(),
                contentDescription = "QR-код ${page + 1}",
                filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CircularProgressIndicator()
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        if (paused) {
            IconButton(onClick = { step(-1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Предыдущий код") }
        }
        Text(
            "Код ${page + 1} из ${frames.size}" + if (paused) " · пауза" else "",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (paused) {
            IconButton(onClick = { step(1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Следующий код") }
        }
    }
    if (frames.size > 1) {
        Text(
            if (paused) "Нажмите на код, чтобы продолжить" else "Нажмите на код, чтобы остановить",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val qrReader = QRCodeReader()
private val decodeHints = mapOf(
    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
    DecodeHintType.CHARACTER_SET to "ISO-8859-1",
)

// Буфер кадра переиспользуется: анализ идёт на одном потоке
private val qrScratch = ThreadLocal<ByteArray>()

/**
 * QR читается при любом повороте, поэтому кадр не разворачивается и не копируется лишний раз:
 * яркость берётся прямо из плоскости Y, а центр кадра вырезается без копии
 */
private fun decodeQr(image: ImageProxy, tick: Int): String? {
    return try {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val row = plane.rowStride
        val pixel = plane.pixelStride
        val w = image.width
        val h = image.height
        val data: ByteArray
        val dataWidth: Int
        if (pixel == 1) {
            dataWidth = row
            val need = row * h
            data = qrScratch.get()?.takeIf { it.size == need } ?: ByteArray(need).also { qrScratch.set(it) }
            buffer.position(0)
            buffer.get(data, 0, minOf(buffer.remaining(), need))
        } else {
            dataWidth = w
            data = ByteArray(w * h)
            var p = 0
            for (y in 0 until h) {
                val start = y * row
                for (x in 0 until w) data[p++] = buffer.get(start + x * pixel)
            }
        }
        val cw = (w * 0.78f).toInt().coerceIn(32, w)
        val ch = (h * 0.78f).toInt().coerceIn(32, h)
        decodeSource(PlanarYUVLuminanceSource(data, dataWidth, h, (w - cw) / 2, (h - ch) / 2, cw, ch, false))
            ?: if (tick % 3 == 0) decodeSource(PlanarYUVLuminanceSource(data, dataWidth, h, 0, 0, w, h, false)) else null
    } catch (_: Exception) {
        null
    }
}

private fun decodeSource(source: PlanarYUVLuminanceSource): String? = try {
    synchronized(qrReader) {
        try {
            qrReader.decode(BinaryBitmap(HybridBinarizer(source)), decodeHints).text
        } finally {
            qrReader.reset()
        }
    }
} catch (_: Exception) {
    null
}

