package ntmt.schedule.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ntmt.schedule.R
import ntmt.schedule.data.Catalog
import ntmt.schedule.data.FilterCache
import ntmt.schedule.data.GroupWeeks
import ntmt.schedule.data.Lesson
import ntmt.schedule.data.NtmtApi
import ntmt.schedule.data.PrefState
import ntmt.schedule.data.Prefs
import ntmt.schedule.data.ScheduleCache
import ntmt.schedule.data.ru
import ntmt.schedule.notify.Notify
import ntmt.schedule.WidgetOpen
import ntmt.schedule.update.AppUpdate
import ntmt.schedule.update.UpdateAuto
import ntmt.schedule.widget.ScheduleWidget
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Вкладка «Настройки» вынесена из NtmtAppUi.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MorePane(
    st: PrefState,
    catalog: Catalog?,
    prefs: Prefs,
    group: String?,
    onChangeGroup: () -> Unit,
    onChangeGroup2: () -> Unit,
    onOpenUpdates: () -> Unit,
    onImported: (Catalog, Map<String, GroupWeeks>) -> Unit,
    onOpenCabinet: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var timeEdit by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf(false) }
    var shareCache by remember { mutableStateOf<Transfer?>(null) }
    var receiveCache by remember { mutableStateOf<Transfer?>(null) }
    var pickShare by remember { mutableStateOf(false) }
    var pickReceive by remember { mutableStateOf(false) }
    val nfcSend = remember { nfcPossible(ctx, sending = true) }
    val nfcReceive = remember { nfcPossible(ctx, sending = false) }
    var githubQr by remember { mutableStateOf(false) }
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) sendTestNotify(ctx, group)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text("Настройки", style = MaterialTheme.typography.headlineSmall)
            Text("Уведомления, обновление и оформление", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Column(Modifier.animateContentSize(tween(280, easing = FastOutSlowInEasing))) {
                Row(
                    Modifier.fillMaxWidth().clickable { notes = !notes }.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Новое в версии 2.1", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    val turn by animateFloatAsState(
                        targetValue = if (notes) 180f else 0f,
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        label = "notes",
                    )
                    Icon(
                        Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.rotate(turn),
                    )
                }
                if (notes) {
                    Text(
                        "• Расписание на завтра: после последней пары и в пустое воскресенье сразу показывается следующий учебный день\n• Передача расписания и файлов по QR-кодам и NFC\n• Резервная копия учебных файлов в zip: папки кабинета и скачанные, в фоне раз в день, неделю или месяц\n• Скачанные файлы можно сохранять в выбранную папку на телефоне\n• Значок профиля в кабинете на выбор",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    )
                }
            }
        }

        SettingsBlock("Приложение") {
            SettingsTap(
                title = "Обновления",
                hint = buildString {
                    append(
                        when (st.updateSource) {
                            "github" -> "GitHub"
                            else -> "Сервер разработчика"
                        },
                    )
                    append(" · ")
                    append(if (st.updateChannel == "stable") "Stable" else "βeta")
                },
                trailing = { Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = onOpenUpdates,
            )
        }

        SettingsBlock("Данные расписания") {
            SettingsTap(
                title = "Поделиться",
                hint = if (nfcSend) "Сохранённое расписание через QR-коды или NFC" else "Показать QR-коды сохранённого расписания",
                trailing = { Icon(Icons.Outlined.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { if (nfcSend) pickShare = true else shareCache = Transfer.Qr },
            )
            SettingsTap(
                title = "Получить",
                hint = if (nfcReceive) "С другого телефона: QR-коды или NFC" else "Сканировать QR-коды с другого телефона",
                trailing = { Icon(Icons.Outlined.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { if (nfcReceive) pickReceive = true else receiveCache = Transfer.Qr },
            )
        }

        SettingsBlock("Файлы") {
            SettingsTap(
                title = "Резервная копия и скачанные",
                hint = "Настройки перенесены на главную страницу вкладки «Кабинет», они доступны без входа",
                trailing = { Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = onOpenCabinet,
            )
        }

        SettingsBlock("Уведомления") {
            Line("Изменения расписания", "Только уведомление, если слежение нашло правку", st.notify) { scope.launch { prefs.setNotify(it) } }
            Line("Напоминание о парах", "Сколько пар сегодня, с какого времени и до какого", st.morning) { scope.launch { prefs.setMorning(it) } }
            if (st.morning) {
                SettingsTap(
                    title = "Время напоминания",
                    hint = "Каждый день в",
                    trailing = { Text(st.morningTime, style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary) },
                    onClick = { timeEdit = "morning" },
                )
            }
            SettingsTap(
                title = "Проверить уведомление",
                hint = "Тестовое сообщение в шторке",
                trailing = { Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        sendTestNotify(ctx, group)
                    }
                },
            )
        }

        SettingsBlock("Обновление") {
            Line(
                "Следить по активности",
                "Каждые 30 минут, когда обновляется виджет или открыт рабочий стол",
                st.liveWatch,
            ) { scope.launch { prefs.setLiveWatch(it) } }
            Line("Автообновление расписания", "Подтянуть пары с сайта в выбранное время", st.autoRefresh) { scope.launch { prefs.setAutoRefresh(it) } }
            if (st.autoRefresh) {
                SettingsTap(
                    title = "Время автообновления",
                    hint = "Каждый день в",
                    trailing = { Text(st.autoRefreshTime, style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary) },
                    onClick = { timeEdit = "auto" },
                )
            }
        }

        SettingsBlock("Персонализация") {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onChangeGroup),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Outlined.Group, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("Группа по умолчанию", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(group ?: "Не выбрана", style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Monospace)
                    }
                    Text("Сменить", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth().clickable(onClick = onChangeGroup2)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Outlined.Group, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("Вторая группа", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(st.group2 ?: "Не выбрано", style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Monospace)
                    }
                    if (st.group2 != null) {
                        Text(
                            "Сбросить",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { scope.launch { prefs.setGroup2(null); ScheduleWidget.refresh(ctx) } },
                        )
                    }
                    Text("Выбрать", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            Line("Сетка курса", "Подсветить звонки курса по номеру группы", st.highlightCourse) { scope.launch { prefs.setHighlightCourse(it) } }
            Line("Выделять сегодня", "Рамка дня на вкладке «Неделя»", st.highlightWeekToday) { scope.launch { prefs.setHighlightWeekToday(it) } }
            Line(
                "Показывать расписание на завтра",
                "После последней пары на вкладке «Сегодня» и в виджете",
                st.showTomorrow,
            ) { scope.launch { prefs.setShowTomorrow(it); ScheduleWidget.refresh(ctx) } }
            Text("Тема", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp, top = 8.dp))
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("auto" to "Авто", "light" to "Светлая", "dark" to "Тёмная").forEach { (id, label) ->
                    FilterChip(selected = st.theme == id, onClick = { scope.launch { prefs.setTheme(id); ScheduleWidget.refresh(ctx) } }, label = { Text(label) })
                }
            }
            Line(
                "Адаптивная тема",
                "Material You: цвета приложения и виджета как в системе",
                st.materialYou,
            ) { scope.launch { prefs.setMaterialYou(it); ScheduleWidget.refresh(ctx) } }
        }

        SettingsBlock("Виджет") {
            SwitchLine("Номер пары", st.widgetNumber) {
                scope.launch { prefs.setWidgetNumber(it); ScheduleWidget.refresh(ctx) }
            }
            SwitchLine("Время пары", st.widgetShowTime) {
                scope.launch { prefs.setWidgetShowTime(it); ScheduleWidget.refresh(ctx) }
            }
            if (st.widgetShowTime) {
                Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = st.widgetTime == "full", onClick = { scope.launch { prefs.setWidgetTime("full"); ScheduleWidget.refresh(ctx) } }, label = { Text("Полное") })
                    FilterChip(selected = st.widgetTime == "start", onClick = { scope.launch { prefs.setWidgetTime("start"); ScheduleWidget.refresh(ctx) } }, label = { Text("Начало") })
                }
            }
            SwitchLine("Аудитория", st.widgetRoom) {
                scope.launch { prefs.setWidgetRoom(it); ScheduleWidget.refresh(ctx) }
            }
            SwitchLine("Доп. Информация", st.widgetHeader) {
                scope.launch { prefs.setWidgetHeader(it); ScheduleWidget.refresh(ctx) }
            }
            Text("Открывать вкладку", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp, top = 8.dp))
            Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Today" to "Сегодня", "Week" to "Неделя", "Bells" to "Звонки", "Cabinet" to "Кабинет", "More" to "Настройки").chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (id, label) ->
                            FilterChip(
                                selected = st.widgetOpen == id,
                                onClick = { scope.launch { prefs.setWidgetOpen(id); ScheduleWidget.refresh(ctx) } },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }
            var alpha by remember(st.widgetAlpha) { mutableFloatStateOf(st.widgetAlpha.toFloat()) }
            var detent by remember { mutableIntStateOf((st.widgetAlpha / 10) * 10) }
            var lastPush by remember { mutableLongStateOf(0L) }
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Прозрачность", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Text(
                        "${alpha.roundToInt()}%",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Slider(
                value = alpha,
                onValueChange = { raw ->
                    val nearest = (raw / 10f).roundToInt() * 10
                    val held = kotlin.math.abs(raw - nearest) < 1.4f
                    alpha = if (held) nearest.toFloat() else raw
                    if (held && nearest != detent) {
                        detent = nearest
                        val vibrator = ctx.getSystemService(Vibrator::class.java)
                        if (Build.VERSION.SDK_INT >= 29) {
                            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                        } else {
                            vibrator?.vibrate(VibrationEffect.createOneShot(12, 40))
                        }
                    }
                    val now = android.os.SystemClock.uptimeMillis()
                    if (now - lastPush > 80) {
                        lastPush = now
                        val value = alpha.roundToInt()
                        scope.launch { prefs.setWidgetAlpha(value); ScheduleWidget.refresh(ctx) }
                    }
                },
                onValueChangeFinished = { scope.launch { prefs.setWidgetAlpha(alpha.roundToInt()); ScheduleWidget.refresh(ctx) } },
                valueRange = 10f..100f,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        LinkRow(
            label = "GitHub",
            url = "https://github.com/currentslot/ntmt-schedule-app",
            mark = "github",
            onQr = { githubQr = true },
        )
        LinkRow(
            label = "@currentslot",
            url = "https://t.me/currentslot",
            mark = "telegram",
        )
        VersionMark()
    }
    if (pickShare) {
        TransferPickDialog("Поделиться расписанием", sending = true, onPick = { pickShare = false; shareCache = it }) { pickShare = false }
    }
    if (pickReceive) {
        TransferPickDialog("Получить расписание", sending = false, onPick = { pickReceive = false; receiveCache = it }) { pickReceive = false }
    }
    shareCache?.let { way -> CacheShareDialog(way) { shareCache = null } }
    receiveCache?.let { way -> CacheReceiveDialog(way, onClose = { receiveCache = null }, onImported = onImported) }
    if (githubQr) LinkQrDialog("https://github.com/currentslot/ntmt-schedule-app") { githubQr = false }
    if (timeEdit != null) {
        TimePickDialog(
            title = "Каждый день в",
            initial = if (timeEdit == "auto") st.autoRefreshTime else st.morningTime,
            onDismiss = { timeEdit = null },
            onConfirm = { v ->
                val kind = timeEdit
                scope.launch {
                    if (kind == "auto") prefs.setAutoRefreshTime(v) else prefs.setMorningTime(v)
                }
                timeEdit = null
            },
        )
    }
}

@Composable
internal fun SettingsBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 12.dp),
    )
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
private fun LinkRow(label: String, url: String, mark: String, onQr: (() -> Unit)? = null) {
    val uri = LocalUriHandler.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .weight(1f)
                .clip(shape)
                .clickable { uri.openUri(url) },
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BrandMark(mark)
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (onQr != null) {
            Surface(
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .size(52.dp)
                    .clip(shape)
                    .clickable(onClick = onQr),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.QrCode, contentDescription = "QR-код", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun BrandMark(kind: String) {
    val tint = MaterialTheme.colorScheme.onSurface
    val data = if (kind == "telegram") TELEGRAM_PATH else GITHUB_PATH
    val path = remember(kind) { androidx.compose.ui.graphics.vector.PathParser().parsePathString(data).toPath() }
    androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
        val s = size.minDimension / 24f
        scale(s, s, androidx.compose.ui.geometry.Offset.Zero) {
            drawPath(path, tint)
        }
    }
}

private const val GITHUB_PATH =
    "M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"

private const val TELEGRAM_PATH =
    "M11.944 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0zm4.962 7.224c.1-.002.321.023.465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.48.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z"

@Composable
private fun VersionMark() {
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Версия 2.1 Stable",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TelegramCalendar(Modifier.padding(start = 6.dp).size(22.dp))
    }
}

@Composable
private fun TelegramCalendar(modifier: Modifier = Modifier) {
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier,
        factory = { context ->
            android.widget.ImageView(context).apply {
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                val drawable = if (android.os.Build.VERSION.SDK_INT >= 28) {
                    val src = android.graphics.ImageDecoder.createSource(context.assets, "emoji/calendar.webp")
                    android.graphics.ImageDecoder.decodeDrawable(src).also { drawn ->
                        if (drawn is android.graphics.drawable.AnimatedImageDrawable) {
                            drawn.repeatCount = android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE
                            drawn.start()
                        }
                    }
                } else {
                    null
                }
                setImageDrawable(drawable)
            }
        },
    )
}

private fun sendTestNotify(ctx: Context, group: String?) {
    Notify.createChannels(ctx)
    val body = if (group.isNullOrBlank()) {
        "Если видите это — уведомления работают."
    } else {
        "Группа $group. Если видите это — уведомления работают."
    }
    Notify.show(ctx, 90, Notify.CH_CHANGE, "Проверка уведомления", body)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val parts = initial.split(":")
    val state = androidx.compose.material3.rememberTimePickerState(
        initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 7,
        initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0,
        is24Hour = true,
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                TimePicker(state = state, modifier = Modifier.padding(top = 12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    TextButton(onClick = {
                        onConfirm("${state.hour.toString().padStart(2, '0')}:${state.minute.toString().padStart(2, '0')}")
                    }) { Text("ОК") }
                }
            }
        }
    }
}

@Composable
private fun SwitchLine(title: String, checked: Boolean, on: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked, on)
    }
}

@Composable
internal fun Line(title: String, hint: String, checked: Boolean, on: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, on)
    }
}

@Composable
internal fun SettingsTap(title: String, hint: String, trailing: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}
