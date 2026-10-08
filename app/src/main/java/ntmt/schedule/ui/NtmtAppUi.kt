package ntmt.schedule.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.AlertDialog
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
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ntmt.schedule.R
import ntmt.schedule.data.Catalog
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
    var offset by remember { mutableIntStateOf(0) }
    var catalog by remember { mutableStateOf<Catalog?>(null) }
    var all by remember { mutableStateOf<Map<String, GroupWeeks>>(emptyMap()) }
    var err by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var stale by remember { mutableStateOf(false) }
    var fetchFailed by remember { mutableStateOf(false) }
    var online by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun pull() {
        if (!withContext(Dispatchers.IO) { NtmtApi.online(ctx) }) {
            val cached = withContext(Dispatchers.IO) { ScheduleCache.load(ctx) }
            if (cached != null) {
                catalog = cached.first
                all = cached.second
                err = null
                stale = true
                fetchFailed = true
            } else if (catalog == null) {
                err = "Нет сети"
                fetchFailed = true
            } else {
                fetchFailed = true
            }
            return
        }
        try {
            val loaded = withContext(Dispatchers.IO) { NtmtApi.load() }
            catalog = loaded.first
            all = loaded.second
            err = null
            stale = false
            fetchFailed = false
            ScheduleCache.save(ctx, loaded.first, loaded.second)
            ScheduleWidget.refresh(ctx)
        } catch (e: Exception) {
            val cached = withContext(Dispatchers.IO) { ScheduleCache.load(ctx) }
            if (cached != null) {
                catalog = cached.first
                all = cached.second
                err = null
                stale = true
                fetchFailed = true
            } else {
                err = e.message
                fetchFailed = true
            }
        }
    }

    LaunchedEffect(Unit) {
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
                    st, prefs, catalog, all, err, refreshing, fetchFailed, tab, offset,
                    updateReady = online && st.availableUpdate > AppUpdate.installedCode(ctx),
                    onTab = { go(it) },
                    onOffset = { offset = it },
                    onReload = { reload() },
                    onPickGroup = { pickFor = "group"; picker = true },
                    onPickGroup2 = { pickFor = "group2"; picker = true },
                    onUpdates = { updates = true },
                    onSwap = {
                        scope.launch {
                            prefs.swapGroups()
                            ScheduleWidget.refresh(ctx)
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    updateReady: Boolean,
    onTab: (Tab) -> Unit,
    onOffset: (Int) -> Unit,
    onReload: () -> Unit,
    onPickGroup: () -> Unit,
    onPickGroup2: () -> Unit,
    onUpdates: () -> Unit,
    onSwap: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Box(Modifier.fillMaxWidth().height(16.dp)) {
                            Text(
                                if (catalog == null) "НТМТ" else "Обновлено: ${NtmtApi.formatUpdate(catalog.updateDt)}",
                                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.align(Alignment.CenterStart).padding(end = 76.dp),
                            )
                            if (fetchFailed) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.align(Alignment.CenterEnd).height(16.dp),
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
                        }
                        if (st.group2 != null) {
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(onClick = onSwap)
                                    .padding(end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(st.group ?: "Группа", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Outlined.SwapHoriz, contentDescription = "Переключить группу", modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Text(st.group ?: "Группа", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                actions = {
                    if (updateReady) {
                        var showLabel by remember { mutableStateOf(true) }
                        LaunchedEffect(updateReady) {
                            showLabel = true
                            kotlinx.coroutines.delay(10_000)
                            showLabel = false
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
                                .padding(end = 2.dp)
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
                NavigationBarItem(tab == Tab.Cabinet, { onTab(Tab.Cabinet) }, icon = { Icon(Icons.Outlined.Person, null) }, label = { Text("Кабинет", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall) })
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
                    catalog == null || refreshing && all.isEmpty() -> Text("Загрузка…", modifier = Modifier.padding(24.dp))
                    tab == Tab.Today -> TodayPane(g, weeks, offset, onOffset)
                    tab == Tab.Week -> WeekPane(catalog!!, g, weeks, st.highlightWeekToday)
                    tab == Tab.Bells -> BellsPane(g, st.highlightCourse)
                    else -> MorePane(st, catalog, prefs, g, onPickGroup, onPickGroup2, onUpdates)
                }
            }
            if (keepCabinet) {
                CabinetPane(
                    active = tab == Tab.Cabinet,
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
        }
    }
}

@Composable
private fun TodayPane(
    group: String?,
    weeks: GroupWeeks?,
    offset: Int,
    setOffset: (Int) -> Unit,
) {
    if (group == null) {
        Text("Сначала выберите группу", modifier = Modifier.padding(24.dp))
        return
    }
    var drag by remember { mutableFloatStateOf(0f) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick++
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
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxWidth().padding(bottom = 6.dp, end = 60.dp)) {
                    Text(
                        if (dayOffset == 0) "Сегодня" else date.dayOfWeek.ru(),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                    )
                    Text(
                        "${date.dayOfWeek.ru()}, ${date.format(DateTimeFormatter.ofPattern("d MMMM", Locale("ru")))}",
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeekPane(catalog: Catalog, group: String?, weeks: GroupWeeks?, highlightWeekToday: Boolean) {
    if (group == null) {
        Text("Сначала выберите группу", modifier = Modifier.padding(24.dp))
        return
    }
    var numerator by remember { mutableStateOf(catalog.weekTypeNumerator) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick++
        }
    }
    val week = weeks?.getOrNull(if (numerator) 0 else 1).orEmpty()
    val days = NtmtApi.DAYS.take(6)
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Column(Modifier.padding(bottom = 6.dp)) {
                Text("Неделя", style = MaterialTheme.typography.headlineSmall)
                Text("Общее расписание · ${catalog.semesterTitle}", style = MaterialTheme.typography.bodySmall)
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
                            val marks = if (isToday) {
                                tick
                                NtmtApi.pairMarks(group, ordered.map { it.first })
                            } else emptyMap()
                            PairColumn(group, slots, scroll = false, marks = marks)
                        }
                    }
                }
            }
        item { Spacer(Modifier.height(24.dp)) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MorePane(
    st: PrefState,
    catalog: Catalog?,
    prefs: Prefs,
    group: String?,
    onChangeGroup: () -> Unit,
    onChangeGroup2: () -> Unit,
    onOpenUpdates: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var timeEdit by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf(false) }
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
                    Text("Изменения в этой версии", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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
                        "• Старый кэш расписания больше не копится\n• Исправлен фон виджета",
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

        SettingsBlock("Уведомления") {
            Line("Изменения расписания", "Только уведомление, если слежение нашло правку", st.notify) { scope.launch { prefs.setNotify(it) } }
            Line("Напоминание о парах", "В выбранное время и только в дни, когда есть пары", st.morning) { scope.launch { prefs.setMorning(it) } }
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
            label = "Страница проекта",
            url = "https://github.com/currentslot/ntmt-schedule-app",
            mark = "github",
        )
        LinkRow(
            label = "@currentslot",
            url = "https://t.me/currentslot",
            mark = "telegram",
        )
        VersionMark()
    }
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
private fun SettingsBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
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
private fun LinkRow(label: String, url: String, mark: String) {
    val uri = LocalUriHandler.current
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
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
            "Версия 2.0.1 Stable (93)",
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

@Composable
private fun IconPick(selected: Boolean, res: Int, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(res), label, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
            Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
        }
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
                        NtmtApi.pairRanges(group, key).forEach { (from, to) ->
                            Text("с $from до $to", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        lessons.forEach { l ->
                            val room = presentField(l.a)
                            val teacher = presentField(l.p)
                            Text(presentField(l.n) ?: "Занятие", style = MaterialTheme.typography.bodyLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
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
                if (current != null || allowNone) IconButton(onClick = onClose) { Icon(Icons.Outlined.ChevronLeft, "Назад") }
            })
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            OutlinedTextField(
                q, { q = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = {
                    Text(
                        "Поиск группы",
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
