package ntmt.schedule.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PublicOff
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
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

internal enum class Tab { Today, Week, Bells, Cabinet, More }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NtmtAppUi() {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    val st by prefs.state.collectAsState(initial = ntmt.schedule.data.PrefState())
    var tab by remember { mutableStateOf(Tab.Today) }
    var restored by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    var pickFor by remember { mutableStateOf("group") }
    var updates by remember { mutableStateOf(false) }
    var scanSchedule by remember { mutableStateOf<Transfer?>(null) }
    var pickScan by remember { mutableStateOf(false) }
    var offset by remember { mutableIntStateOf(0) }
    var boot by remember { mutableStateOf(false) }
    var catalog by remember { mutableStateOf<Catalog?>(null) }
    var all by remember { mutableStateOf<Map<String, GroupWeeks>>(emptyMap()) }
    var err by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var stale by remember { mutableStateOf(false) }
    var fetchFailed by remember { mutableStateOf(false) }
    var online by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun pull() {
        try {
            val loaded = withContext(Dispatchers.IO) { NtmtApi.load() }
            catalog = loaded.first
            all = loaded.second
            err = null
            stale = false
            fetchFailed = false
            ScheduleCache.save(ctx, loaded.first, loaded.second)
            ScheduleWidget.refresh(ctx)
            // Изменения, которые уже видны в приложении, не повторяются уведомлением
            prefs.state.first().group?.let { g -> loaded.second[g]?.let { prefs.setSnapshot(NtmtApi.snapshot(it)) } }
        } catch (e: Exception) {
            val cached = withContext(Dispatchers.IO) { ScheduleCache.load(ctx) }
            if (cached != null) {
                catalog = cached.first
                all = cached.second
                err = null
                stale = true
                fetchFailed = true
            } else if (catalog == null) {
                err = if (e is java.io.IOException) "Нет сети" else (e.message ?: "Нет сети")
                fetchFailed = true
            } else {
                fetchFailed = true
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) { prefs.ensureWidgetOpen() }
            val first = prefs.state.first()
            val opened = WidgetOpen.tabs.replayCache.lastOrNull()
            tab = Tab.entries.find { it.name.equals(opened ?: first.tab, true) } ?: Tab.Today
            picker = first.group == null
            restored = true
            val cached = withContext(Dispatchers.IO) { ScheduleCache.load(ctx) }
            if (cached != null) {
                catalog = cached.first
                all = cached.second
                val g = first.group
                val weeks = g?.let { all[it] }
                if (g != null && weeks != null) offset = NtmtApi.shownOffset(g, weeks, first.showTomorrow)
                boot = true
            }
            launch { try { prefs.setLauncher(first.launcher) } catch (_: Exception) {} }
            launch {
                withContext(Dispatchers.IO) {
                    try {
                        UpdateAuto.run(ctx, force = true)
                    } catch (_: Exception) {
                    }
                }
            }
            refreshing = true
            pull()
            refreshing = false
            val g = first.group
            val weeks = g?.let { all[it] }
            if (g != null && weeks != null && offset == 0) {
                offset = NtmtApi.shownOffset(g, weeks, first.showTomorrow)
            }
        } finally {
            boot = true
        }
    }
    LaunchedEffect(Unit) {
        ntmt.schedule.UpdateOpen.events.collect { updates = true }
    }
    // Каждый вход в приложение: проверка новой версии на GitHub по выбранному каналу
    val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        var first = true
        val watch = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event != androidx.lifecycle.Lifecycle.Event.ON_START) return@LifecycleEventObserver
            // Первый вход уже проверен при запуске выше
            if (first) { first = false; return@LifecycleEventObserver }
            scope.launch(Dispatchers.IO) { try { UpdateAuto.run(ctx, force = false) } catch (_: Exception) {} }
        }
        owner.lifecycle.addObserver(watch)
        onDispose { owner.lifecycle.removeObserver(watch) }
    }
    LaunchedEffect(Unit) {
        WidgetOpen.tabs.collect { name ->
            val next = Tab.entries.find { it.name.equals(name, true) } ?: Tab.Today
            tab = next
            prefs.setTab(next.name)
        }
    }

    LaunchedEffect(fetchFailed, refreshing) {
        online = withContext(Dispatchers.IO) { NtmtApi.online(ctx) }
    }

    fun go(next: Tab) {
        tab = next
        if (restored) scope.launch { prefs.setTab(next.name) }
    }

    fun reload() {
        if (refreshing) return
        scope.launch {
            refreshing = true
            pull()
            refreshing = false
        }
    }

    val systemDark = isSystemInDarkTheme()
    val dark = when (st.theme) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    val overlay = remember { OverlayHost() }
    NtmtTheme(dark = dark, materialYou = st.materialYou) {
      androidx.compose.runtime.CompositionLocalProvider(LocalOverlay provides overlay) {
        if (!restored) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            return@CompositionLocalProvider
        }
        BackHandler(enabled = updates) { updates = false }
        BackHandler(enabled = picker && st.group != null && !updates) { picker = false }
        val screen = when {
            updates -> "updates"
            picker -> "picker"
            else -> "home"
        }
        AnimatedContent(
            targetState = screen,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            transitionSpec = {
                val open = targetState != "home"
                val move = tween<IntOffset>(280, easing = FastOutSlowInEasing)
                if (open) {
                    ContentTransform(
                        targetContentEnter = slideInHorizontally(move) { it },
                        initialContentExit = slideOutHorizontally(move) { -it / 5 },
                        targetContentZIndex = 1f,
                    )
                } else {
                    ContentTransform(
                        targetContentEnter = slideInHorizontally(move) { -it / 4 },
                        initialContentExit = slideOutHorizontally(move) { it },
                        targetContentZIndex = -1f,
                    )
                }
            },
            label = "page",
        ) { dest ->
            when (dest) {
                "updates" -> UpdatesPane(st, prefs) { updates = false }
                "picker" -> GroupPicker(
                    names = catalog?.groups.orEmpty(),
                    current = if (pickFor == "group2") st.group2 else st.group,
                    title = if (pickFor == "group2") "Вторая группа" else "Группа по умолчанию",
                    allowNone = pickFor == "group2",
                    onFetch = { if (nfcPossible(ctx, sending = false)) pickScan = true else scanSchedule = Transfer.Qr },
                    onClose = { if (st.group != null) picker = false },
                    onPick = { name ->
                        scope.launch {
                            if (pickFor == "group2") prefs.setGroup2(name) else if (name != null) prefs.setGroup(name)
                            ScheduleWidget.refresh(ctx)
                        }
                        picker = false
                        if (st.group == null && pickFor != "group2") go(Tab.Today)
                        offset = 0
                    },
                )
                else -> HomeScaffold(
                    st, prefs, catalog, all, err, refreshing, fetchFailed, tab, offset, boot,
                    updateReady = st.availableUpdate > AppUpdate.installedCode(ctx),
                    siteDown = fetchFailed && online,
                    onTab = { go(it) },
                    onOffset = { offset = it },
                    onReload = { reload() },
                    onPickGroup = { pickFor = "group"; picker = true },
                    onPickGroup2 = { pickFor = "group2"; picker = true },
                    onUpdates = { updates = true },
                    onImported = { c, m ->
                        catalog = c
                        all = m
                        err = null
                        stale = false
                        fetchFailed = false
                        ScheduleWidget.refresh(ctx)
                    },
                    onSwap = {
                        scope.launch {
                            prefs.swapGroups()
                            ScheduleWidget.refresh(ctx)
                        }
                    },
                )
            }
        }
        // Страница поверх вкладок (выбор преподавателя или аудитории): выезжает так же, как выбор группы
        val shownOverlay = overlay.content
        val lastOverlay = remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
        if (shownOverlay != null) lastOverlay.value = shownOverlay
        BackHandler(enabled = shownOverlay != null) { overlay.back() }
        AnimatedVisibility(
            visible = shownOverlay != null,
            enter = slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { it },
            exit = slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { it },
        ) {
            lastOverlay.value?.invoke()
        }
        if (pickScan) {
            TransferPickDialog("Получить расписание", sending = false, onPick = { pickScan = false; scanSchedule = it }) { pickScan = false }
        }
        scanSchedule?.let { way ->
            CacheReceiveDialog(
                method = way,
                onClose = { scanSchedule = null },
                onImported = { c, m ->
                    catalog = c
                    all = m
                    err = null
                    stale = false
                    fetchFailed = false
                    ScheduleWidget.refresh(ctx)
                },
            )
        }
      }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HomeScaffold(
    st: PrefState,
    prefs: Prefs,
    catalog: Catalog?,
    all: Map<String, GroupWeeks>,
    err: String?,
    refreshing: Boolean,
    fetchFailed: Boolean,
    tab: Tab,
    offset: Int,
    boot: Boolean,
    updateReady: Boolean,
    onTab: (Tab) -> Unit,
    onOffset: (Int) -> Unit,
    onReload: () -> Unit,
    onPickGroup: () -> Unit,
    onPickGroup2: () -> Unit,
    onUpdates: () -> Unit,
    onImported: (Catalog, Map<String, GroupWeeks>) -> Unit,
    onSwap: () -> Unit,
    weekFilter: WeekFilter = remember { WeekFilter() },
    siteDown: Boolean = false,
) {
    // Повторное нажатие на «Кабинет» возвращает на первую страницу вкладки
    var cabinetHome by remember { mutableIntStateOf(0) }
    var cabinetSettings by remember { mutableIntStateOf(0) }
    // Повторное нажатие на открытую вкладку возвращает её список в начало
    var todayTop by remember { mutableIntStateOf(0) }
    var weekTop by remember { mutableIntStateOf(0) }
    var bellsTop by remember { mutableIntStateOf(0) }
    var moreTop by remember { mutableIntStateOf(0) }
    var groupMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val groups = catalog?.groups.orEmpty()
    BackHandler(enabled = groupMenu) { groupMenu = false }
    LaunchedEffect(tab) { groupMenu = false }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HomeTopBar(
                st = st,
                catalog = catalog,
                fetchFailed = fetchFailed,
                siteDown = siteDown,
                refreshing = refreshing,
                updateReady = updateReady,
                menuOpen = groupMenu,
                onGroup = { if (groupMenu) groupMenu = false else if (groups.isNotEmpty()) groupMenu = true },
                onSwap = { groupMenu = false; onSwap() },
                onReload = onReload,
                onUpdates = onUpdates,
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                NavTab("Сегодня", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth, tab == Tab.Today) { if (tab == Tab.Today) todayTop++ else onTab(Tab.Today) }
                NavTab("Неделя", Icons.Outlined.ViewWeek, Icons.Filled.ViewWeek, tab == Tab.Week) { if (tab == Tab.Week) weekTop++ else onTab(Tab.Week) }
                NavTab("Звонки", Icons.Outlined.Notifications, Icons.Filled.Notifications, tab == Tab.Bells) { if (tab == Tab.Bells) bellsTop++ else onTab(Tab.Bells) }
                NavTab("Кабинет", Icons.Outlined.Person, Icons.Filled.Person, tab == Tab.Cabinet) { if (tab == Tab.Cabinet) cabinetHome++ else onTab(Tab.Cabinet) }
                NavTab("Настройки", Icons.Outlined.Settings, Icons.Filled.Settings, tab == Tab.More) { if (tab == Tab.More) moreTop++ else onTab(Tab.More) }
            }
        },
    ) { pad ->
        val g = st.group
        val weeks = g?.let { all[it] }
        Box(Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp)) {
            var keepCabinet by remember { mutableStateOf(false) }
            if (tab == Tab.Cabinet) keepCabinet = true
            if (tab != Tab.Cabinet) {
                when {
                    err != null -> CenterState {
                        if (siteDown) {
                            StateBox(
                                "Сайт расписания недоступен",
                                hint = "nti.urfu.ru не отвечает, хотя интернет на телефоне есть. Попробуйте позже",
                                emoji = "🛠️", action = "Повторить", onAction = onReload,
                            )
                        } else {
                            StateBox("Не удалось загрузить расписание", hint = err, emoji = "📡", action = "Повторить", onAction = onReload)
                        }
                    }
                    catalog == null || !boot || refreshing && all.isEmpty() -> CenterState {
                        StateBox("Загрузка расписания…", loading = true)
                    }
                    tab == Tab.Today -> TodayPane(g, weeks, offset, onOffset, st.showTomorrow, todayTop)
                    tab == Tab.Week -> WeekPane(catalog!!, g, weeks, st.highlightWeekToday, weekFilter, weekTop)
                    tab == Tab.Bells -> BellsPane(g, st.highlightCourse, bellsTop)
                    else -> MorePane(st, catalog, prefs, g, onPickGroup, onPickGroup2, onUpdates, onImported, onOpenCabinet = { cabinetSettings++; onTab(Tab.Cabinet) }, updateReady = updateReady, topTick = moreTop)
                }
            }
            if (keepCabinet) {
                CabinetPane(
                    active = tab == Tab.Cabinet,
                    home = cabinetHome,
                    toSettings = cabinetSettings,
                    modifier = if (tab == Tab.Cabinet) Modifier.fillMaxSize() else Modifier.size(0.dp),
                )
            }
            val transfer = CabinetTransfer.name
            if (tab != Tab.Cabinet && transfer != null) {
                DownloadNotice(
                    name = transfer,
                    fraction = CabinetTransfer.fraction,
                    onCancel = { CabinetTransfer.cancel?.invoke() },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                )
            }
            if (groupMenu) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                        ) { groupMenu = false },
                )
            }
            AnimatedVisibility(
                visible = groupMenu,
                enter = expandVertically(tween(220, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) + fadeIn(tween(140)),
                exit = shrinkVertically(tween(180, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(120)),
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Surface(
                    shape = Ui.Card,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 6.dp,
                    modifier = Modifier.widthIn(min = 200.dp, max = 260.dp).heightIn(max = 380.dp),
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(6.dp)) {
                        Text(
                            "Показать группу",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
                        )
                        groups.forEach { name ->
                            val on = name == st.group
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(Ui.Inner)
                                    .background(if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable {
                                        groupMenu = false
                                        onOffset(0)
                                        scope.launch {
                                            prefs.setGroup(name)
                                            ScheduleWidget.refresh(ctx)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    name,
                                    color = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (on) androidx.compose.ui.text.font.FontWeight.SemiBold else null,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (on) Icon(Icons.Outlined.Check, contentDescription = "Выбрана", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconOn: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(if (selected) iconOn else icon, contentDescription = null) },
        label = { Text(label, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall) },
    )
}

/**
 * Верхняя панель: группа (нажатие открывает список), вторая группа, строка состояния и кнопки справа.
 * Строка состояния всегда одной высоты: «Нет сети» и «Сайт недоступен» заменяют в ней текст,
 * а не добавляют плашку, поэтому остальной экран не сдвигается.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeTopBar(
    st: PrefState,
    catalog: Catalog?,
    fetchFailed: Boolean,
    siteDown: Boolean,
    refreshing: Boolean,
    updateReady: Boolean,
    menuOpen: Boolean,
    onGroup: () -> Unit,
    onSwap: () -> Unit,
    onReload: () -> Unit,
    onUpdates: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .background(scheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier
                        .weight(1f, fill = false)
                        .clip(Ui.Inner)
                        .background(scheme.surfaceContainerHigh)
                        .combinedClickable(onClick = onGroup, onLongClick = onGroup)
                        .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        st.group ?: "Группа",
                        style = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    val turn by animateFloatAsState(if (menuOpen) 180f else 0f, tween(200), label = "group-arrow")
                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Выбрать группу", tint = scheme.onSurface, modifier = Modifier.rotate(turn))
                }
                val second = st.group2
                if (second != null) {
                    Row(
                        Modifier
                            .weight(1f, fill = false)
                            .pressable(Ui.Inner, onClick = onSwap)
                            .border(1.dp, scheme.outlineVariant, Ui.Inner)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Outlined.SwapHoriz, contentDescription = "Переключить на группу", modifier = Modifier.size(18.dp), tint = scheme.onSurfaceVariant)
                        Text(second, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            val stamp = catalog?.let { NtmtApi.formatUpdate(it.updateDt) }
            val status = when {
                siteDown -> "site"
                fetchFailed -> "net"
                else -> "ok"
            }
            AnimatedContent(
                targetState = status,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(140)) },
                modifier = Modifier.padding(top = 6.dp, start = 2.dp).height(18.dp),
                contentAlignment = Alignment.CenterStart,
                label = "status",
            ) { kind ->
                Row(Modifier.height(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    val bad = kind != "ok"
                    if (bad) {
                        Icon(
                            if (kind == "site") Icons.Outlined.PublicOff else Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = scheme.error,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        when (kind) {
                            "site" -> "Сайт недоступен" + (stamp?.let { " · данные от $it" } ?: "")
                            "net" -> "Нет сети" + (stamp?.let { " · данные от $it" } ?: "")
                            else -> if (stamp == null) "НТМТ" else "Обновлено $stamp"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (bad) scheme.error else scheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // Доступное обновление: заметная кнопка рядом с «Обновить», место под неё не меняет высоту панели
        AnimatedVisibility(
            visible = updateReady,
            enter = scaleIn(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(160)),
            exit = scaleOut(tween(160)) + fadeOut(tween(140)),
        ) {
            Box {
                Box(
                    Modifier.size(40.dp).pressable(CircleShape, onClick = onUpdates).background(scheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_system_update),
                        contentDescription = "Доступно обновление",
                        modifier = Modifier.size(22.dp),
                        tint = scheme.primary.readable(scheme.onPrimary),
                    )
                }
                Box(Modifier.align(Alignment.TopEnd).size(11.dp).clip(CircleShape).background(scheme.background).padding(2.dp).clip(CircleShape).background(scheme.tertiary))
            }
        }
        Box(
            Modifier.size(40.dp).pressable(CircleShape, onClick = onReload).background(scheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (refreshing) {
                // Анимация создаётся только на время обновления, а не крутится вхолостую всё время
                val spin = rememberInfiniteTransition(label = "refresh")
                val angle by spin.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearEasing)),
                    label = "refresh-angle",
                )
                Icon(Icons.Outlined.Refresh, contentDescription = "Обновление…", tint = scheme.onSurface, modifier = Modifier.size(22.dp).rotate(angle))
            } else {
                Icon(Icons.Outlined.Refresh, contentDescription = "Обновить", tint = scheme.onSurface, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/**
 * Выбор из списка: группа, преподаватель или аудитория.
 * Группы ([mono]) показаны плитками по специальностям, длинные имена — списком.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GroupPicker(
    names: List<String>,
    current: String?,
    title: String,
    allowNone: Boolean = false,
    alwaysClose: Boolean = false,
    searchHint: String = "Поиск группы",
    mono: Boolean = true,
    onFetch: (() -> Unit)? = null,
    onClose: () -> Unit,
    onPick: (String?) -> Unit,
) {
    var q by remember { mutableStateOf("") }
    val filtered = names.filter { it.contains(q, true) || it.replace("-", "").contains(q.replace("-", ""), true) }
    val showNone = allowNone && (q.isBlank() || "не выбрано".contains(q.trim(), true))
    val scheme = MaterialTheme.colorScheme
    val canClose = current != null || allowNone || alwaysClose
    Surface(color = scheme.background, contentColor = scheme.onSurface, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (canClose) TonalIcon(Icons.Outlined.ChevronLeft, "Назад", onClick = onClose)
                Column(Modifier.weight(1f)) {
                    Text(title, style = Ui.Title.copy(fontSize = 22.sp, lineHeight = 28.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        when {
                            names.isEmpty() -> "Список ещё не загружен"
                            q.isNotBlank() -> "Найдено: ${filtered.size}"
                            current != null -> "Сейчас: $current"
                            else -> "Всего: ${names.size}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            androidx.compose.material3.TextField(
                q, { q = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(searchHint, color = scheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = scheme.onSurfaceVariant) },
                trailingIcon = {
                    if (q.isNotEmpty()) IconButton(onClick = { q = "" }) { Icon(Icons.Outlined.Close, "Очистить", tint = scheme.onSurfaceVariant) }
                },
                singleLine = true,
                shape = CircleShape,
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = scheme.surfaceContainerHigh,
                    unfocusedContainerColor = scheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = scheme.onSurface,
                    unfocusedTextColor = scheme.onSurface,
                ),
            )
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showNone) {
                    item(key = "\u0000none") { PickTile("Не выбрано", selected = current == null, modifier = Modifier.fillMaxWidth()) { onPick(null) } }
                }
                if (mono) {
                    // Группы одной специальности стоят вместе: «ТО», «ИС» и так далее
                    val sections = filtered.groupBy { it.substringBefore('-').trim().ifEmpty { "Другие" } }
                    sections.forEach { (head, list) ->
                        item(key = "sec:$head") {
                            Column(Modifier.animateItem()) {
                                Row(Modifier.padding(start = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(head, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), color = scheme.primary)
                                    Text("${list.size}", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                                }
                                list.chunked(3).forEach { row ->
                                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        row.forEach { name -> PickTile(name, selected = name == current, modifier = Modifier.weight(1f)) { onPick(name) } }
                                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                } else if (filtered.isNotEmpty()) {
                    item(key = "list") {
                        BlockCard {
                            filtered.forEachIndexed { i, name ->
                                if (i > 0) RowDivider()
                                PickRow(name, selected = name == current) { onPick(name) }
                            }
                        }
                    }
                }
                if (filtered.isEmpty() && !showNone) {
                    item(key = "empty") {
                        if (names.isEmpty()) StateBox("Список пуст", hint = "Расписание ещё не загружено. Проверьте интернет или получите расписание с другого телефона", emoji = "📡")
                        else StateBox("Ничего не найдено", hint = "Проверьте запрос", emoji = "🔎")
                    }
                }
            }
            if (onFetch != null) {
                Button(
                    onClick = onFetch,
                    modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 16.dp, vertical = 12.dp).height(52.dp),
                    shape = Ui.Inner,
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Получить расписание с другого телефона", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
            }
        }
    }
}

/** Плитка группы: короткое имя, три в ряд */
@Composable
private fun PickTile(name: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val back by animateColorAsState(if (selected) scheme.primary else scheme.surfaceContainer, Ui.Tint, label = "tile-bg")
    val fg = if (selected) scheme.primary.readable(scheme.onPrimary) else scheme.onSurface
    Row(
        modifier.height(48.dp).pressable(Ui.Inner, onClick = onClick).background(back).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (selected) {
            Icon(Icons.Outlined.Check, contentDescription = "Выбрано", tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            name,
            style = Ui.Time.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium),
            color = fg,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Строка длинного имени: преподаватель, аудитория */
@Composable
private fun PickRow(name: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) scheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (selected) scheme.primaryContainer.readable(scheme.onPrimaryContainer) else scheme.onSurface
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else null,
            color = fg,
            modifier = Modifier.weight(1f),
        )
        if (selected) Icon(Icons.Outlined.Check, contentDescription = "Выбрано", tint = fg, modifier = Modifier.size(20.dp))
    }
}
