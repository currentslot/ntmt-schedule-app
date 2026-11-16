package ntmt.schedule.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.NewReleases
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
import ntmt.schedule.notify.Push
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    updateReady: Boolean = false,
    topTick: Int = 0,
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
    val chevron: @Composable () -> Unit = {
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    val scroll = rememberScrollState()
    LaunchedEffect(topTick) { if (topTick > 0) scroll.animateScrollTo(0, tween(360, easing = FastOutSlowInEasing)) }
    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
        ScreenHeader("Настройки", "Группа, уведомления, оформление и виджет")

        // Доступное обновление: заметная карточка в начале настроек, ведёт на экран обновлений
        AnimatedVisibility(
            visible = updateReady,
            enter = expandVertically(tween(280, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(220, easing = FastOutSlowInEasing)) + fadeOut(tween(140)),
        ) {
            UpdateBanner(Modifier.padding(bottom = 12.dp), onClick = onOpenUpdates)
        }

        // Группы стоят первыми: это то, что меняют чаще всего
        BlockCard {
            GroupRow(
                label = "Группа по умолчанию",
                value = group ?: "Не выбрана",
                hint = null,
                onClick = onChangeGroup,
            ) {
                TextAction("Сменить", onChangeGroup)
            }
            RowDivider(start = 70.dp)
            GroupRow(
                label = "Вторая группа",
                value = st.group2 ?: "Не выбрано",
                hint = if (st.group2 == null) "Для быстрого переключения кнопкой ⇄ вверху экрана" else null,
                onClick = onChangeGroup2,
            ) {
                if (st.group2 != null) {
                    TextAction("Сбросить") { scope.launch { prefs.setGroup2(null); ScheduleWidget.refresh(ctx) } }
                }
                TextAction("Выбрать", onChangeGroup2)
            }
        }

        SettingsBlock("Уведомления") {
            Line(
                "Изменения расписания",
                "Уведомление, когда на сайте поменялись пары вашей группы или дата обновления",
                st.notify,
                Icons.Outlined.Notifications,
            ) { on ->
                scope.launch {
                    prefs.setNotify(on)
                    Push.sync(ctx, on && st.push)
                }
            }
            Reveal(st.notify) {
                Line(
                    "Мгновенно через сервер",
                    "Сервер проверяет сайт каждые 15–35 минут и сразу присылает пуш. Нужны сервисы Google",
                    st.push,
                    Icons.Outlined.Bolt,
                ) { on ->
                    scope.launch {
                        prefs.setPush(on)
                        Push.sync(ctx, on)
                    }
                }
            }
            Line(
                "Следить по активности",
                "Запасная проверка самим телефоном: раз в 30 минут, когда обновляется виджет или включается экран",
                st.liveWatch,
                Icons.Outlined.Visibility,
            ) { scope.launch { prefs.setLiveWatch(it) } }
            Line(
                "Напоминание о парах",
                "Сколько пар сегодня, с какого времени и до какого",
                st.morning,
                Icons.Outlined.Alarm,
            ) { scope.launch { prefs.setMorning(it) } }
            Reveal(st.morning) {
                SettingsTap(
                    title = "Время напоминания",
                    hint = "Каждый день в",
                    trailing = { ValueChip(st.morningTime) },
                    icon = Icons.Outlined.Schedule,
                    onClick = { timeEdit = "morning" },
                )
            }
            SettingsTap(
                title = "Проверить уведомление",
                hint = "Тестовое сообщение в шторке",
                trailing = chevron,
                icon = Icons.Outlined.NotificationsActive,
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

        SettingsBlock("Расписание") {
            Line(
                "Показывать расписание на завтра",
                "После последней пары на вкладке «Сегодня» и в виджете",
                st.showTomorrow,
                Icons.Outlined.Event,
            ) { scope.launch { prefs.setShowTomorrow(it); ScheduleWidget.refresh(ctx) } }
            Line(
                "Выделять сегодня",
                "Рамка дня на вкладке «Неделя»",
                st.highlightWeekToday,
                Icons.Outlined.Today,
            ) { scope.launch { prefs.setHighlightWeekToday(it) } }
            Line(
                "Сетка курса",
                "Подсветить звонки курса по номеру группы",
                st.highlightCourse,
                Icons.Outlined.School,
            ) { scope.launch { prefs.setHighlightCourse(it) } }
        }

        SettingsBlock("Оформление") {
            val themes = listOf("auto" to "Авто", "light" to "Светлая", "dark" to "Тёмная")
            RowLabel(Icons.Outlined.DarkMode, "Тема")
            Segmented(
                options = themes.map { it.second },
                selected = themes.indexOfFirst { it.first == st.theme },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            ) { i -> scope.launch { prefs.setTheme(themes[i].first); ScheduleWidget.refresh(ctx) } }
            Line(
                "Адаптивная тема",
                "Material You: цвета приложения и виджета как в системе",
                st.materialYou,
                Icons.Outlined.Palette,
            ) { scope.launch { prefs.setMaterialYou(it); ScheduleWidget.refresh(ctx) } }
            RowLabel(Icons.Outlined.Apps, "Значок приложения")
            Row(
                Modifier.fillMaxWidth().padding(start = 66.dp, end = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LauncherChoice("Обычный", R.color.ic_launcher_bg, R.drawable.ic_launcher_fg, st.launcher == "calendar", Modifier.weight(1f)) {
                    scope.launch { prefs.setLauncher("calendar") }
                }
                LauncherChoice("Тёмный", R.color.ic_launcher_bg_dark, R.drawable.ic_launcher_fg_dark, st.launcher == "dark", Modifier.weight(1f)) {
                    scope.launch { prefs.setLauncher("dark") }
                }
                LauncherChoice("Тематический", R.color.ic_launcher_bg_tone, R.drawable.ic_launcher_fg_tone, st.launcher == "mono", Modifier.weight(1f)) {
                    scope.launch { prefs.setLauncher("mono") }
                }
            }
            Text(
                "Тематический значок однотонный, с Android 12 берёт цвета обоев",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 66.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
            )
        }

        SettingsBlock("Виджет") {
            SwitchLine("Номер пары", st.widgetNumber, Icons.Outlined.Tag) {
                scope.launch { prefs.setWidgetNumber(it); ScheduleWidget.refresh(ctx) }
            }
            SwitchLine("Время пары", st.widgetShowTime, Icons.Outlined.Schedule) {
                scope.launch { prefs.setWidgetShowTime(it); ScheduleWidget.refresh(ctx) }
            }
            Reveal(st.widgetShowTime) {
                Segmented(
                    options = listOf("Полное", "Начало"),
                    selected = if (st.widgetTime == "start") 1 else if (st.widgetTime == "full") 0 else -1,
                    modifier = Modifier.padding(start = 66.dp, end = 16.dp, bottom = 8.dp),
                ) { i -> scope.launch { prefs.setWidgetTime(if (i == 0) "full" else "start"); ScheduleWidget.refresh(ctx) } }
            }
            SwitchLine("Аудитория", st.widgetRoom, Icons.Outlined.MeetingRoom) {
                scope.launch { prefs.setWidgetRoom(it); ScheduleWidget.refresh(ctx) }
            }
            SwitchLine("Доп. информация", st.widgetHeader, Icons.Outlined.Info) {
                scope.launch { prefs.setWidgetHeader(it); ScheduleWidget.refresh(ctx) }
            }
            RowLabel(Icons.Outlined.OpenInNew, "Открывать вкладку")
            FlowRow(
                Modifier.padding(start = 66.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Today" to "Сегодня", "Week" to "Неделя", "Bells" to "Звонки", "Cabinet" to "Кабинет", "More" to "Настройки").forEach { (id, label) ->
                    Choice(label, selected = st.widgetOpen == id) {
                        scope.launch { prefs.setWidgetOpen(id); ScheduleWidget.refresh(ctx) }
                    }
                }
            }
            var alpha by remember(st.widgetAlpha) { mutableFloatStateOf(st.widgetAlpha.toFloat()) }
            var detent by remember { mutableIntStateOf((st.widgetAlpha / 10) * 10) }
            var lastPush by remember { mutableLongStateOf(0L) }
            RowLabel(Icons.Outlined.Opacity, "Прозрачность") { ValueChip("${alpha.roundToInt()}%") }
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
                modifier = Modifier.padding(start = 62.dp, end = 16.dp),
            )
        }

        SettingsBlock("Данные и файлы") {
            SettingsTap(
                title = "Поделиться расписанием",
                hint = if (nfcSend) "Сохранённое расписание через QR-коды или NFC" else "Показать QR-коды сохранённого расписания",
                trailing = chevron,
                icon = Icons.Outlined.Upload,
                onClick = { if (nfcSend) pickShare = true else shareCache = Transfer.Qr },
            )
            SettingsTap(
                title = "Получить расписание",
                hint = if (nfcReceive) "С другого телефона: QR-коды или NFC" else "Сканировать QR-коды с другого телефона",
                trailing = chevron,
                icon = Icons.Outlined.Download,
                onClick = { if (nfcReceive) pickReceive = true else receiveCache = Transfer.Qr },
            )
            SettingsTap(
                title = "Резервная копия и скачанные",
                hint = "Настройки находятся на вкладке «Кабинет» и доступны без входа",
                trailing = chevron,
                icon = Icons.Outlined.Folder,
                onClick = onOpenCabinet,
            )
        }

        SettingsBlock("О приложении") {
            SettingsTap(
                title = "Обновления",
                hint = buildString {
                    append("GitHub")
                    append(" · ")
                    append(if (st.updateChannel == "stable") "Stable" else "βeta")
                },
                trailing = { if (updateReady) AccentPill("Доступно") else chevron() },
                icon = Icons.Outlined.SystemUpdate,
                onClick = onOpenUpdates,
            )
            Column(Modifier.animateContentSize(tween(280, easing = FastOutSlowInEasing))) {
                val turn by animateFloatAsState(
                    targetValue = if (notes) 180f else 0f,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    label = "notes",
                )
                SettingsTap(
                    title = "Новое в версии 3.0",
                    hint = "Что изменилось в приложении",
                    trailing = {
                        Icon(
                            Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.rotate(turn),
                        )
                    },
                    icon = Icons.Outlined.NewReleases,
                    onClick = { notes = !notes },
                )
                if (notes) {
                    Text(
                        WHATS_NEW,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 66.dp, end = 16.dp, bottom = 12.dp),
                    )
                }
            }
            LinkRow(
                label = "GitHub",
                hint = "Исходный код и выпуски",
                url = "https://github.com/currentslot/ntmt-schedule-app",
                mark = "github",
                onQr = { githubQr = true },
            )
        }
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
            initial = st.morningTime,
            onDismiss = { timeEdit = null },
            onConfirm = { v ->
                val kind = timeEdit
                scope.launch { if (kind == "morning") prefs.setMorningTime(v) }
                timeEdit = null
            },
        )
    }
}

private const val WHATS_NEW =
    "• Новый интерфейс всех вкладок: полоса недели и перелистывание дней пальцем на вкладке «Сегодня», дни карточками на вкладке «Неделя», настройки по разделам\n" +
        "• Мгновенные уведомления об изменении расписания через сервер\n" +
        "• Уведомление о новой версии приложения от сервера, проверка обновлений при каждом входе\n" +
        "• В строке вверху видно, что нет сети или недоступен сам сайт расписания\n" +
        "• Новый значок приложения и выбор: обычный, тёмный, тематический\n" +
        "• Выбор группы плитками по специальностям\n" +
        "• Повторное нажатие на вкладку возвращает в начало списка\n" +
        "• В виджете название предмета переносится по правилам языка"

/** Карточка «Доступно обновление» */
@Composable
internal fun UpdateBanner(modifier: Modifier = Modifier, title: String = "Доступно обновление", hint: String = "Нажмите, чтобы посмотреть и установить", onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val fg = scheme.primary.readable(scheme.onPrimary)
    Row(
        modifier.fillMaxWidth().pressable(Ui.Card, onClick = onClick).background(scheme.primary).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(fg.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_system_update), contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Ui.CardTitle, color = fg)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.85f))
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = fg)
    }
}

/** Строка, которая появляется и исчезает вместе с включением настройки над ней */
@Composable
private fun ColumnScope.Reveal(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(240, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) + fadeIn(tween(180)),
        exit = shrinkVertically(tween(200, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(120)),
    ) { content() }
}

/** Вариант значка приложения: как он будет выглядеть на рабочем столе */
@Composable
private fun LauncherChoice(label: String, bg: Int, fg: Int, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val ring by androidx.compose.animation.animateColorAsState(if (selected) scheme.primary else Color.Transparent, Ui.Tint, label = "icon-ring")
    Column(
        modifier.pressable(Ui.Inner, onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(64.dp).border(2.dp, ring, RoundedCornerShape(20.dp)).padding(5.dp)) {
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(15.dp)).background(androidx.compose.ui.res.colorResource(bg)),
                contentAlignment = Alignment.Center,
            ) {
                // У адаптивного значка видна середина 72 из 108, поэтому рисунок в полтора раза больше окна
                Image(painterResource(fg), contentDescription = null, modifier = Modifier.requiredSize(81.dp))
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Строка группы в верхней карточке настроек */
@Composable
private fun GroupRow(label: String, value: String, hint: String?, onClick: () -> Unit, actions: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(Icons.Outlined.Group, size = 40.dp)
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = Ui.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { actions() }
    }
}

/** Значение справа в строке настройки: время, проценты */
@Composable
private fun ValueChip(text: String) {
    Surface(shape = Ui.Inner, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(
            text,
            style = Ui.Time.copy(fontSize = 16.sp, lineHeight = 21.sp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** Подпись к элементу выбора внутри блока: значок, название и необязательное значение справа */
@Composable
private fun RowLabel(icon: ImageVector, title: String, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconBadge(icon)
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Название раздела и карточка с его строками */
@Composable
internal fun SettingsBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp, start = 16.dp),
    )
    Surface(shape = Ui.Card, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        // Без отступов сверху и снизу: волна нажатия у первой и последней строки доходит до края карточки
        Column(content = content)
    }
}

@Composable
private fun LinkRow(label: String, hint: String, url: String, mark: String, onQr: (() -> Unit)? = null) {
    val uri = LocalUriHandler.current
    Row(
        Modifier.fillMaxWidth().clickable { uri.openUri(url) }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) { BrandMark(mark) }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onQr != null) TonalIcon(Icons.Outlined.QrCode, "QR-код, скопировать, поделиться", size = 36.dp, onClick = onQr)
        Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun BrandMark(kind: String) {
    val tint = MaterialTheme.colorScheme.onSurface
    val data = GITHUB_PATH
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

@Composable
private fun VersionMark() {
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Версия 3.0 Stable (136)",
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

/** Общая строка настройки: значок, название с пояснением и элемент справа */
@Composable
private fun SettingRow(
    icon: ImageVector?,
    title: String,
    hint: String?,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) IconBadge(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun SwitchLine(title: String, checked: Boolean, icon: ImageVector? = null, on: (Boolean) -> Unit) {
    SettingRow(icon, title, null, Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = on)) {
        Switch(checked, onCheckedChange = null)
    }
}

/** Переключатель с пояснением. Нажимается вся строка, а не только сам переключатель */
@Composable
internal fun Line(title: String, hint: String, checked: Boolean, icon: ImageVector? = null, on: (Boolean) -> Unit) {
    SettingRow(icon, title, hint, Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = on)) {
        Switch(checked, onCheckedChange = null)
    }
}

@Composable
internal fun SettingsTap(
    title: String,
    hint: String,
    trailing: @Composable () -> Unit,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    SettingRow(icon, title, hint, Modifier.clickable(onClick = onClick), trailing)
}
