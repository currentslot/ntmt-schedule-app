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

private enum class Tab { Today, Week, Bells, Cabinet, More }

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

    NtmtTheme(dark = dark, materialYou = st.materialYou) {
        if (!restored) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            return@NtmtTheme
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
                    updateReady = online && st.availableUpdate > AppUpdate.installedCode(ctx),
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun HomeScaffold(
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
) {
    // Повторное нажатие на «Кабинет» возвращает на первую страницу вкладки
    var cabinetHome by remember { mutableIntStateOf(0) }
    var cabinetSettings by remember { mutableIntStateOf(0) }
    val weekFilter = remember { WeekFilter() }
    var groupMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val groups = catalog?.groups.orEmpty()
    BackHandler(enabled = groupMenu) { groupMenu = false }
    LaunchedEffect(tab) { groupMenu = false }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (catalog == null) "НТМТ" else "Обновлено: ${NtmtApi.formatUpdate(catalog.updateDt)}",
                                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (fetchFailed) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.padding(start = 6.dp).height(16.dp),
                                ) {
                                    Text(
                                        "Нет сети",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                    )
                                }
                            }
                            if (updateReady) {
                                var showLabel by remember { mutableStateOf(!st.updateExpandedOnce) }
                                LaunchedEffect(updateReady) {
                                    if (st.updateExpandedOnce) {
                                        showLabel = false
                                        return@LaunchedEffect
                                    }
                                    showLabel = true
                                    kotlinx.coroutines.delay(7_000)
                                    showLabel = false
                                    prefs.setUpdateExpandedOnce()
                                }
                                val pulse = rememberInfiniteTransition(label = "update-pulse")
                                val glow by pulse.animateFloat(
                                    initialValue = 0.55f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1600, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse,
                                    ),
                                    label = "update-glow",
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .height(30.dp)
                                        .animateContentSize(tween(320, easing = FastOutSlowInEasing))
                                        .alpha(glow)
                                        .clip(CircleShape)
                                        .clickable(onClick = onUpdates),
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.ic_system_update),
                                            contentDescription = "Доступно обновление",
                                            modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                        AnimatedVisibility(
                                            visible = showLabel,
                                            enter = fadeIn(tween(160)) + expandHorizontally(tween(280, easing = FastOutSlowInEasing)),
                                            exit = fadeOut(tween(180)) + shrinkHorizontally(tween(320, easing = FastOutSlowInEasing)),
                                        ) {
                                            Text(
                                                "Доступно обновление",
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(start = 4.dp, end = 2.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Row(
                            Modifier
                                .combinedClickable(
                                    onClick = {
                                        if (groupMenu) groupMenu = false
                                        else if (st.group2 != null) onSwap()
                                    },
                                    onLongClick = { if (groups.isNotEmpty()) groupMenu = !groupMenu },
                                )
                                .padding(end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(st.group ?: "Группа", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (st.group2 != null) {
                                Icon(
                                    Icons.Outlined.SwapHoriz,
                                    contentDescription = "Переключить группу",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                },
                actions = {
                    val spin = rememberInfiniteTransition(label = "refresh")
                    val angle by spin.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearEasing)),
                        label = "refresh-angle",
                    )
                    IconButton(onClick = onReload) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "Обновить",
                            modifier = if (refreshing) Modifier.rotate(angle) else Modifier,
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == Tab.Today, { onTab(Tab.Today) }, icon = { Icon(Icons.Outlined.CalendarMonth, null) }, label = { Text("Сегодня", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall) })
                NavigationBarItem(tab == Tab.Week, { onTab(Tab.Week) }, icon = { Icon(Icons.Outlined.ViewWeek, null) }, label = { Text("Неделя", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall) })
                NavigationBarItem(tab == Tab.Bells, { onTab(Tab.Bells) }, icon = { Icon(Icons.Outlined.Notifications, null) }, label = { Text("Звонки", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall) })
                NavigationBarItem(tab == Tab.Cabinet, { if (tab == Tab.Cabinet) cabinetHome++ else onTab(Tab.Cabinet) }, icon = { Icon(Icons.Outlined.Person, null) }, label = { Text("Кабинет", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall) })
                NavigationBarItem(tab == Tab.More, { onTab(Tab.More) }, icon = { Icon(Icons.Outlined.Settings, null) }, label = { Text("Настройки", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall) })
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
                    err != null -> Text(err, color = MaterialTheme.colorScheme.primary)
                    catalog == null || !boot || refreshing && all.isEmpty() -> Text("Загрузка…", modifier = Modifier.padding(24.dp))
                    tab == Tab.Today -> TodayPane(g, weeks, offset, onOffset, st.showTomorrow)
                    tab == Tab.Week -> WeekPane(catalog!!, g, weeks, st.highlightWeekToday, weekFilter)
                    tab == Tab.Bells -> BellsPane(g, st.highlightCourse)
                    else -> MorePane(st, catalog, prefs, g, onPickGroup, onPickGroup2, onUpdates, onImported, onOpenCabinet = { cabinetSettings++; onTab(Tab.Cabinet) })
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
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 6.dp,
                    modifier = Modifier.widthIn(max = 260.dp).heightIn(max = 360.dp),
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        groups.forEach { name ->
                            Text(
                                name,
                                color = if (name == st.group) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        groupMenu = false
                                        onOffset(0)
                                        scope.launch {
                                            prefs.setGroup(name)
                                            ScheduleWidget.refresh(ctx)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayPane(
    group: String?,
    weeks: GroupWeeks?,
    offset: Int,
    setOffset: (Int) -> Unit,
    showTomorrow: Boolean,
) {
    if (group == null) {
        Text("Сначала выберите группу", modifier = Modifier.padding(24.dp))
        return
    }
    var drag by remember { mutableFloatStateOf(0f) }
    var tick by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick++
        }
    }
    var flipped by remember { mutableStateOf(offset != 0) }
    var widgetPushed by remember { mutableStateOf(false) }
    LaunchedEffect(showTomorrow, tick, weeks) {
        val target = if (weeks == null) 0 else NtmtApi.shownOffset(group, weeks, showTomorrow)
        if (target <= 0) return@LaunchedEffect
        if (!widgetPushed) {
            widgetPushed = true
            ScheduleWidget.refresh(ctx)
        }
        if (offset == 0 && !flipped) {
            flipped = true
            setOffset(target)
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(offset) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (drag > 64f) setOffset(offset - 1)
                        else if (drag < -64f) setOffset(offset + 1)
                        drag = 0f
                    },
                    onHorizontalDrag = { _, amount -> drag += amount },
                )
            },
    ) {
        AnimatedContent(
            targetState = offset,
            transitionSpec = {
                val forward = targetState > initialState
                val dir = if (forward) 1 else -1
                val move = tween<IntOffset>(280, easing = FastOutSlowInEasing)
                (slideInHorizontally(move) { full -> dir * full } togetherWith
                    slideOutHorizontally(move) { full -> -dir * full })
                    .using(SizeTransform(clip = true) { _, _ -> snap() })
            },
            label = "day",
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        ) { dayOffset ->
            val date = NtmtApi.today().plusDays(dayOffset.toLong())
            val slots = weeks?.let { NtmtApi.slotsFor(it, date) }.orEmpty()
            val keys = NtmtApi.ordered(slots).map { it.first }
            val marks = if (dayOffset == 0 && tick >= 0) NtmtApi.pairMarks(group, keys) else emptyMap()
            val auto = dayOffset > 0 && weeks != null && dayOffset == NtmtApi.shownOffset(group, weeks, showTomorrow)
            val tomorrow = auto
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxWidth().padding(bottom = 6.dp, end = 60.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (dayOffset == 0) "Сегодня" else date.dayOfWeek.ru(),
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (tomorrow) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(start = 8.dp),
                            ) {
                                Text(
                                    if (NtmtApi.nextWeek(date)) "Следующая неделя" else "Завтра",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Text(
                        if (dayOffset == 0) {
                            "${date.dayOfWeek.ru()}, ${date.format(DateTimeFormatter.ofPattern("d MMMM", Locale("ru")))}"
                        } else {
                            date.format(DateTimeFormatter.ofPattern("d MMMM", Locale("ru")))
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(selected = true, onClick = {}, label = { Text(if (NtmtApi.isNumerator(date)) "Числитель" else "Знаменатель") })
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (keys.isEmpty()) EmptyPairs(today = dayOffset == 0)
                    else PairColumn(group, slots, scroll = true, marks = marks)
                }
            }
        }
        Row(Modifier.align(Alignment.TopEnd).padding(top = 4.dp)) {
            Icon(
                Icons.Outlined.ChevronLeft,
                contentDescription = "Назад",
                modifier = Modifier.size(22.dp).clickable { setOffset(offset - 1) },
            )
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "Вперёд",
                modifier = Modifier.padding(start = 8.dp).size(22.dp).clickable { setOffset(offset + 1) },
            )
        }
    }
}

private class WeekFilter {
    var mode by mutableIntStateOf(1)
    var teacher by mutableStateOf<String?>(null)
    var room by mutableStateOf<String?>(null)
    var open by mutableStateOf(false)
    var listStamp by mutableStateOf("")
    var lists by mutableStateOf<Map<Int, Map<String, GroupWeeks>>>(emptyMap())
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeekPane(
    catalog: Catalog,
    group: String?,
    weeks: GroupWeeks?,
    highlightWeekToday: Boolean,
    filter: WeekFilter,
) {
    DisposableEffect(Unit) {
        onDispose {
            filter.mode = 1
            filter.teacher = null
            filter.room = null
            filter.open = false
        }
    }
    if (group == null) {
        Text("Сначала выберите группу", modifier = Modifier.padding(24.dp))
        return
    }
    var numerator by remember { mutableStateOf(catalog.weekTypeNumerator) }
    var tick by remember { mutableIntStateOf(0) }
    var picking by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var loadErr by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick++
        }
    }
    LaunchedEffect(catalog.semesterId, catalog.stamp, filter.mode) {
        val stamp = catalog.stamp
        if (filter.listStamp != stamp) {
            filter.lists = emptyMap()
            filter.listStamp = stamp
        }
        val order = if (filter.mode == 1) listOf(2, 3) else listOf(filter.mode, if (filter.mode == 2) 3 else 2)
        for (mode in order) {
            if (filter.lists[mode] != null) continue
            if (filter.mode == mode) {
                loading = true
                loadErr = null
            }
            val map = try {
                val cached = withContext(Dispatchers.IO) {
                    FilterCache.load(ctx, mode, catalog.semesterId, stamp)
                        ?.let { got -> if (mode == 3) got.filterKeys(NtmtApi::realRoom) else got }
                }
                cached ?: withContext(Dispatchers.IO) {
                    NtmtApi.loadNamed(mode, catalog.semesterId).also {
                        FilterCache.save(ctx, mode, catalog.semesterId, stamp, it)
                    }
                }
            } catch (e: Exception) {
                if (filter.mode == mode) loadErr = e.message ?: "Не удалось загрузить"
                null
            }
            if (map != null) filter.lists = filter.lists + (mode to map)
            if (filter.mode == mode) loading = false
        }
    }
    val picked = when (filter.mode) {
        2 -> filter.teacher
        3 -> filter.room
        else -> group
    }
    val shown = when (filter.mode) {
        1 -> weeks
        else -> picked?.let { filter.lists[filter.mode]?.get(it) }
    }
    val week = shown?.getOrNull(if (numerator) 0 else 1).orEmpty()
    val days = NtmtApi.DAYS.take(6)
    val pickTitle = if (filter.mode == 3) "Выбор аудитории" else "Выбор преподавателя"
    val names = filter.lists[filter.mode]?.keys?.toList().orEmpty().filter { name ->
        val s = name.trim()
        s.isNotEmpty() && !s.equals("None", true) && !s.equals("null", true)
    }
    fun selectMode(mode: Int) {
        if (filter.mode == mode) return
        filter.teacher = null
        filter.room = null
        filter.mode = mode
    }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Column(Modifier.padding(bottom = 6.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable { filter.open = !filter.open }
                        .padding(bottom = 6.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(end = 40.dp)) {
                        Text("Неделя", style = MaterialTheme.typography.headlineSmall, maxLines = 1)
                        Text("Общее расписание · ${catalog.semesterTitle}", style = MaterialTheme.typography.bodySmall)
                    }
                    Box(Modifier.align(Alignment.TopEnd).padding(top = 4.dp)) {
                        AnimatedContent(
                            targetState = filter.open,
                            transitionSpec = {
                                (fadeIn(tween(180)) + scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.82f))
                                    .togetherWith(fadeOut(tween(140)) + scaleOut(tween(160), targetScale = 0.82f))
                            },
                            label = "filter-icon",
                        ) { opened ->
                            Icon(
                                if (opened) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.FilterAlt,
                                contentDescription = if (opened) "Скрыть фильтр" else "Фильтр",
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = filter.open,
                    enter = expandVertically(tween(280, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) + fadeIn(tween(180)),
                    exit = shrinkVertically(tween(240, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(140)),
                ) {
                    Column(Modifier.padding(top = 8.dp)) {
                        Text(
                            "Режим отображения",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            WeekChip("По группе", filter.mode == 1, false) { selectMode(1) }
                            WeekChip("По преподавателю", filter.mode == 2, false) { selectMode(2) }
                            WeekChip("По аудитории", filter.mode == 3, false) { selectMode(3) }
                        }
                        AnimatedVisibility(
                            visible = filter.mode != 1,
                            enter = expandVertically(tween(260, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) + fadeIn(tween(180)),
                            exit = shrinkVertically(tween(220, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(140)),
                        ) {
                            val shape = RoundedCornerShape(16.dp)
                            Surface(
                                shape = shape,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .clip(shape)
                                    .clickable(enabled = !loading && names.isNotEmpty()) { picking = true },
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(pickTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            when {
                                                loading -> "Загрузка…"
                                                loadErr != null -> loadErr!!
                                                picked != null -> picked
                                                names.isEmpty() -> "Нет данных"
                                                else -> "Не выбрано"
                                            },
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
        stickyHeader {
            Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WeekChip("Числитель", numerator, catalog.weekTypeNumerator) { numerator = true }
                    WeekChip("Знаменатель", !numerator, !catalog.weekTypeNumerator) { numerator = false }
                }
            }
        }
        if (filter.mode != 1 && (picked == null || shown == null)) {
            item {
                Text(
                    when {
                        loading -> "Загрузка…"
                        loadErr != null -> loadErr!!
                        picked == null -> "Выберите ${if (filter.mode == 3) "аудиторию" else "преподавателя"}"
                        else -> "Расписание не найдено"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        } else {
            items(days) { d ->
                val slots = week[d].orEmpty()
                val ordered = NtmtApi.ordered(slots)
                val isToday = highlightWeekToday && d == catalog.todayName && numerator == catalog.weekTypeNumerator
                val title = when (d) {
                    "ПОНЕДЕЛЬНИК" -> "Понедельник"
                    "ВТОРНИК" -> "Вторник"
                    "СРЕДА" -> "Среда"
                    "ЧЕТВЕРГ" -> "Четверг"
                    "ПЯТНИЦА" -> "Пятница"
                    "СУББОТА" -> "Суббота"
                    else -> d
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                    border = if (isToday) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(title, style = MaterialTheme.typography.titleMedium)
                            if (isToday) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                                    Text(
                                        "сегодня",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (ordered.isEmpty()) {
                            EmptyPairs(today = d == catalog.todayName && numerator == catalog.weekTypeNumerator)
                        } else {
                            val marks = if (isToday && filter.mode == 1) {
                                tick
                                NtmtApi.pairMarks(group, ordered.map { it.first })
                            } else emptyMap()
                            PairColumn(group, slots, scroll = false, marks = marks)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    if (picking) {
        Dialog(
            onDismissRequest = { picking = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            GroupPicker(
                names = names,
                current = picked,
                title = pickTitle,
                allowNone = false,
                alwaysClose = true,
                searchHint = if (filter.mode == 3) "Поиск аудитории" else "Поиск преподавателя",
                onClose = { picking = false },
                onPick = { name ->
                    if (filter.mode == 3) filter.room = name else filter.teacher = name
                    picking = false
                },
            )
        }
    }
}

@Composable
private fun WeekChip(label: String, selected: Boolean, now: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, maxLines = 1, softWrap = false)
                if (now) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Text(
                            "сейчас",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun BellsPane(group: String?, highlightCourse: Boolean) {
    val odd = group?.let { NtmtApi.courseOdd(it) } ?: true
    val first = Triple("1/3 курс", NtmtApi.BELLS_ODD, odd)
    val second = Triple("2/4 курс", NtmtApi.BELLS_EVEN, !odd)
    val tables = if (highlightCourse && group != null && !odd) listOf(second, first) else listOf(first, second)
    Column(Modifier.fillMaxSize()) {
        Text("Звонки", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        tables.forEach { (title, rows, mine) ->
            BellCard(
                title = title,
                rows = rows,
                active = highlightCourse && group != null && mine,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun BellCard(title: String, rows: Map<Int, String>, active: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
        border = if (active) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            rows.forEach { (n, t) ->
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(28.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("$n", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Text(
                        "$n пара",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(52.dp),
                    )
                    Text(
                        t.replace("–", "–").replace(" / ", "  /  "),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPairs(today: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🎉", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (today) "Сегодня пар нет!" else "Пар нет",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun PairColumn(
    group: String,
    slots: Map<String, List<Lesson>>,
    scroll: Boolean,
    marks: Map<String, String> = emptyMap(),
) {
    val items = NtmtApi.ordered(slots)
    if (items.isEmpty()) {
        EmptyPairs(today = false)
        return
    }
    val body: @Composable () -> Unit = {
        items.forEach { (key, lessons) ->
            val n = NtmtApi.PAIRS.indexOf(key) + 1
            val mark = marks[key]
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = when (mark) {
                    "now" -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    "next" -> BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
                    else -> null
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("$n", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("$n пара", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val timeGroup = lessons.firstNotNullOfOrNull { presentField(it.g) } ?: group
                        NtmtApi.pairRanges(timeGroup, key).forEach { (from, to) ->
                            Text("с $from до $to", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        lessons.forEach { l ->
                            val room = presentField(l.a)
                            val teacher = presentField(l.p)
                            val own = presentField(l.g)
                            Text(presentField(l.n) ?: "Занятие", style = MaterialTheme.typography.bodyLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            if (own != null) {
                                Text(own, style = MaterialTheme.typography.bodySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                            }
                            if (room != null || teacher != null) {
                                Row {
                                    if (room != null) {
                                        Text("ауд. $room", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                                    }
                                    if (room != null && teacher != null) Text(" · ", style = MaterialTheme.typography.bodySmall)
                                    if (teacher != null) Text(teacher, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (scroll) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            body()
            Spacer(Modifier.height(24.dp))
        }
    } else {
        Column { body() }
    }
}

private fun presentField(v: String?): String? {
    val s = v?.trim().orEmpty()
    if (s.isEmpty() || s.equals("null", true) || s.equals("undefined", true) || s == "-" || s == "—") return null
    return s
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupPicker(
    names: List<String>,
    current: String?,
    title: String,
    allowNone: Boolean = false,
    alwaysClose: Boolean = false,
    searchHint: String = "Поиск группы",
    onFetch: (() -> Unit)? = null,
    onClose: () -> Unit,
    onPick: (String?) -> Unit,
) {
    var q by remember { mutableStateOf("") }
    val filtered = names.filter { it.contains(q, true) || it.replace("-", "").contains(q.replace("-", ""), true) }
    val showNone = allowNone && (q.isBlank() || "не выбрано".contains(q.trim(), true))
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(title) }, navigationIcon = {
                if (current != null || allowNone || alwaysClose) IconButton(onClick = onClose) { Icon(Icons.Outlined.ChevronLeft, "Назад") }
            })
        },
        bottomBar = {
            if (onFetch != null) {
                Button(
                    onClick = onFetch,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Получить расписание")
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            OutlinedTextField(
                q, { q = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = {
                    Text(
                        searchHint,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                    )
                },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                if (showNone) {
                    item {
                        ListItem(
                            headlineContent = { Text("Не выбрано") },
                            supportingContent = { if (current == null) Text("сейчас") },
                            modifier = Modifier.clickable { onPick(null) },
                        )
                    }
                }
                items(filtered) { name ->
                    ListItem(
                        headlineContent = { Text(name, fontFamily = FontFamily.Monospace) },
                        supportingContent = { if (name == current) Text("выбрано") },
                        modifier = Modifier.clickable { onPick(name) },
                    )
                }
            }
        }
    }
}
