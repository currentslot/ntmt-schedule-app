package ntmt.schedule.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ntmt.schedule.data.Catalog
import ntmt.schedule.data.FilterCache
import ntmt.schedule.data.GroupWeeks
import ntmt.schedule.data.Lesson
import ntmt.schedule.data.NtmtApi
import ntmt.schedule.data.ru
import ntmt.schedule.widget.ScheduleWidget
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// Вкладки «Сегодня», «Неделя» и «Звонки». Данные и правила берутся из NtmtApi без изменений,
// здесь только то, как они показаны.

private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))
private val SHORT_DAYS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

/** Страница «сегодня» в ленте дней: слева и справа от неё по [DAY_PAGES] / 2 дней */
private const val DAY_PAGES = 20_000
private const val DAY_BASE = DAY_PAGES / 2

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun TodayPane(
    group: String?,
    weeks: GroupWeeks?,
    offset: Int,
    setOffset: (Int) -> Unit,
    showTomorrow: Boolean,
    topTick: Int = 0,
) {
    if (group == null) {
        CenterState { StateBox("Сначала выберите группу", hint = "Настройки → Группа по умолчанию") }
        return
    }
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
    val today = if (tick >= 0) NtmtApi.today() else NtmtApi.today()
    val autoTarget = if (weeks == null) 0 else NtmtApi.shownOffset(group, weeks, showTomorrow)
    // «Завтра» открывается сразу, с первого кадра: пока вкладка не листалась, вместо сегодняшнего дня
    // берётся нужный, а наружу он сообщается следом. Перелистывания с анимацией при этом не видно
    val pendingFlip = offset == 0 && !flipped && autoTarget > 0
    val offset = if (pendingFlip) autoTarget else offset
    LaunchedEffect(showTomorrow, tick, weeks) {
        if (autoTarget <= 0) return@LaunchedEffect
        if (!widgetPushed) {
            widgetPushed = true
            ScheduleWidget.refresh(ctx)
        }
        if (pendingFlip) {
            setOffset(autoTarget)
            flipped = true
        }
    }
    val date = today.plusDays(offset.toLong())
    val auto = offset > 0 && weeks != null && offset == autoTarget
    val shownSlots = weeks?.let { NtmtApi.slotsFor(it, date) }.orEmpty()
    val shownKeys = NtmtApi.ordered(shownSlots).map { it.first }

    // Дни листаются лентой страниц: страница идёт за пальцем и сама доводится до места
    val pager = rememberPagerState(initialPage = DAY_BASE + offset) { DAY_PAGES }
    val wanted by rememberUpdatedState(offset)
    val report by rememberUpdatedState(setOffset)
    // Пока страницу ведёт палец (и пока она после него доезжает), лента страниц сама себе хозяин:
    // программная прокрутка в это время не запускается, иначе она спорила бы с пальцем и день дёргался
    val byFinger = remember { booleanArrayOf(false) }
    LaunchedEffect(offset) {
        if (byFinger[0]) return@LaunchedEffect
        val page = DAY_BASE + offset
        if (pager.currentPage != page || pager.isScrollInProgress) {
            // Далёкий день и автоматический переход на завтра открываются сразу, соседний день — с прокруткой
            if (pendingFlip || kotlin.math.abs(pager.currentPage - page) > 3) pager.scrollToPage(page)
            else pager.animateScrollToPage(page, animationSpec = tween(280, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(pager) {
        pager.interactionSource.interactions.collect { if (it is DragInteraction.Start) byFinger[0] = true }
    }
    // При свайпе день на ленте дат меняется сразу, как только страница пошла к соседней,
    // а когда прокрутка остановилась, наружу уходит день, на котором она встала
    LaunchedEffect(pager) {
        snapshotFlow { Triple(pager.isScrollInProgress, pager.targetPage, pager.currentPage) }.collect { (moving, target, current) ->
            if (!byFinger[0]) return@collect
            val day = (if (moving) target else current) - DAY_BASE
            if (day != wanted) report(day)
            if (!moving) byFinger[0] = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = if (offset == 0) "Сегодня" else date.dayOfWeek.ru(),
            subtitle = if (offset == 0) "${date.dayOfWeek.ru()}, ${date.format(DAY_MONTH)}" else date.format(DAY_MONTH),
            modifier = Modifier.padding(bottom = 0.dp),
        )
        // Стрелки стоят по бокам ленты дат: листают по одному дню, лента выбирает день недели
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StripArrow(Icons.Outlined.ChevronLeft, "Предыдущий день") { setOffset(offset - 1) }
            WeekStrip(
                selected = date,
                today = today,
                busy = { d -> weeks != null && NtmtApi.ordered(NtmtApi.slotsFor(weeks, d)).isNotEmpty() },
                onPick = { d -> setOffset(ChronoUnit.DAYS.between(today, d).toInt()) },
                modifier = Modifier.weight(1f),
            )
            StripArrow(Icons.Outlined.ChevronRight, "Следующий день") { setOffset(offset + 1) }
        }
        // Метки в одну строку постоянной высоты: когда при перелистывании меток становится больше или меньше,
        // список пар под ними не сдвигается. Если метки не помещаются, строка прокручивается вбок
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp).height(28.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (auto) AccentPill(if (NtmtApi.nextWeek(date)) "Следующая неделя" else "Завтра")
            Pill(if (NtmtApi.isNumerator(date)) "Числитель" else "Знаменатель")
            daySummary(group, shownKeys)?.let { Pill(it, container = MaterialTheme.colorScheme.surfaceContainerHigh, content = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (offset != 0) {
                Pill(
                    "К сегодня",
                    modifier = Modifier.clip(CircleShape).clickable { setOffset(0) },
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                    icon = Icons.Outlined.Today,
                )
            }
        }
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            pageSpacing = 16.dp,
            // Соседние дни готовятся заранее: свайп не запинается на первом кадре
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.Top,
            key = { it },
        ) { page ->
            val dayOffset = page - DAY_BASE
            val day = today.plusDays(dayOffset.toLong())
            val slots = weeks?.let { NtmtApi.slotsFor(it, day) }.orEmpty()
            val items = NtmtApi.ordered(slots)
            val marks = if (dayOffset == 0 && tick >= 0) NtmtApi.pairMarks(group, items.map { it.first }) else emptyMap()
            val scroll = rememberScrollState()
            LaunchedEffect(topTick) { if (topTick > 0) scroll.animateScrollTo(0, tween(320, easing = FastOutSlowInEasing)) }
            // Страница всегда во всю высоту и прокручивается: свайп работает и на пустом дне
            Column(Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (items.isEmpty()) {
                    EmptyPairs(today = dayOffset == 0, big = true)
                } else {
                    items.forEach { (key, lessons) -> LessonCard(group, key, lessons, marks[key]) }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Стрелка сбоку от ленты дат: высотой с ленту, чтобы в неё было легко попасть */
@Composable
private fun StripArrow(icon: ImageVector, description: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.width(36.dp).fillMaxHeight().pressable(Ui.Inner, onClick = onClick).background(scheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = scheme.onSurface, modifier = Modifier.size(22.dp))
    }
}

/** «3 пары · 10:30–15:30» для выбранного дня */
private fun daySummary(group: String, keys: List<String>): String? {
    if (keys.isEmpty()) return null
    val n = keys.size
    val word = when {
        n % 100 in 11..14 -> "пар"
        n % 10 == 1 -> "пара"
        n % 10 in 2..4 -> "пары"
        else -> "пар"
    }
    val from = NtmtApi.pairRanges(group, keys.first()).firstOrNull()?.first
    val to = NtmtApi.pairRanges(group, keys.last()).lastOrNull()?.second
    return if (from != null && to != null) "$n $word · $from–$to" else "$n $word"
}

/** Полоса недели: видно, какой день открыт, где сегодня и в какие дни есть пары */
@Composable
private fun WeekStrip(selected: LocalDate, today: LocalDate, busy: (LocalDate) -> Boolean, onPick: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val monday = selected.with(DayOfWeek.MONDAY)
    val onAccent = scheme.primary.readable(scheme.onPrimary)
    Row(
        modifier.clip(Ui.Card).background(scheme.surfaceContainer).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (i in 0..6) {
            val day = monday.plusDays(i.toLong())
            val on = day == selected
            val now = day == today
            val has = busy(day)
            val back by animateColorAsState(if (on) scheme.primary else Color.Transparent, Ui.Tint, label = "day-bg")
            val text by animateColorAsState(
                when {
                    on -> onAccent
                    has -> scheme.onSurface
                    else -> scheme.onSurfaceVariant.copy(alpha = 0.55f)
                },
                Ui.Tint,
                label = "day-fg",
            )
            val dot by animateColorAsState(if (!has) Color.Transparent else if (on) onAccent else scheme.primary, Ui.Tint, label = "day-dot")
            Column(
                Modifier
                    .weight(1f)
                    .clip(Ui.Inner)
                    .background(back)
                    .then(if (now && !on) Modifier.border(1.5.dp, scheme.primary, Ui.Inner) else Modifier)
                    .clickable { onPick(day) }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(SHORT_DAYS[i], color = text, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                Text("${day.dayOfMonth}", color = text, style = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold), maxLines = 1, softWrap = false)
                Box(Modifier.padding(top = 2.dp).size(5.dp).clip(CircleShape).background(dot))
            }
        }
    }
}

internal class WeekFilter {
    var mode by mutableIntStateOf(1)
    var teacher by mutableStateOf<String?>(null)
    var room by mutableStateOf<String?>(null)
    var open by mutableStateOf(false)
    var listStamp by mutableStateOf("")
    var lists by mutableStateOf<Map<Int, Map<String, GroupWeeks>>>(emptyMap())
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WeekPane(
    catalog: Catalog,
    group: String?,
    weeks: GroupWeeks?,
    highlightWeekToday: Boolean,
    filter: WeekFilter,
    topTick: Int = 0,
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
        CenterState { StateBox("Сначала выберите группу", hint = "Настройки → Группа по умолчанию") }
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
    val list = rememberLazyListState()
    LaunchedEffect(topTick) {
        if (topTick > 0) {
            // Издалека список сначала подтягивается к первым карточкам, потом плавно доезжает до верха
            if (list.firstVisibleItemIndex > 3) list.scrollToItem(3)
            list.animateScrollToItem(0)
        }
    }
    LazyColumn(Modifier.fillMaxSize(), state = list, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column {
                ScreenHeader(
                    title = "Неделя",
                    subtitle = when (filter.mode) {
                        2 -> "Преподаватель: ${filter.teacher ?: "не выбран"}"
                        3 -> "Аудитория: ${filter.room ?: "не выбрана"}"
                        else -> "Общее расписание · ${catalog.semesterTitle}"
                    },
                    modifier = Modifier.padding(bottom = 0.dp),
                    onClick = { filter.open = !filter.open },
                ) {
                    TonalIcon(
                        if (filter.open) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.FilterAlt,
                        if (filter.open) "Скрыть фильтр" else "Фильтр",
                        active = filter.mode != 1 && !filter.open,
                    ) { filter.open = !filter.open }
                }
                AnimatedVisibility(
                    visible = filter.open,
                    enter = expandVertically(tween(280, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) + fadeIn(tween(180)),
                    exit = shrinkVertically(tween(240, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(140)),
                ) {
                    BlockCard(Modifier.padding(bottom = 2.dp)) {
                        Text(
                            "Фильтры",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp),
                        )
                        Segmented(
                            options = listOf("Группа", "Преподаватель", "Аудитория"),
                            selected = filter.mode - 1,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        ) { selectMode(it + 1) }
                        AnimatedVisibility(
                            visible = filter.mode != 1,
                            enter = expandVertically(tween(260, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) + fadeIn(tween(180)),
                            exit = shrinkVertically(tween(220, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(140)),
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, top = 8.dp)
                                    .clip(Ui.Inner)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable(enabled = !loading && names.isNotEmpty()) { picking = true }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
        stickyHeader {
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                Segmented(
                    options = listOf("Числитель", "Знаменатель"),
                    selected = if (numerator) 0 else 1,
                    marked = if (catalog.weekTypeNumerator) 0 else 1,
                    modifier = Modifier.padding(vertical = 4.dp),
                ) { numerator = it == 0 }
            }
        }
        if (filter.mode != 1 && (picked == null || shown == null)) {
            item {
                StateBox(
                    title = when {
                        loading -> "Загрузка…"
                        loadErr != null -> loadErr!!
                        picked == null -> "Выберите ${if (filter.mode == 3) "аудиторию" else "преподавателя"}"
                        else -> "Расписание не найдено"
                    },
                    loading = loading,
                    action = if (!loading && loadErr == null && picked == null && names.isNotEmpty()) "Выбрать" else null,
                    onAction = { picking = true },
                )
            }
        } else {
            items(days, key = { it }) { d ->
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
                val scheme = MaterialTheme.colorScheme
                BlockCard(
                    // Карточки дней не анимируют своё место: иначе при раскрытии фильтра и при возврате в начало
                    // списка они отставали бы от соседей и наезжали на них
                    modifier = Modifier,
                    highlighted = isToday,
                    color = if (isToday) scheme.primaryContainer.copy(alpha = 0.35f).compositeOver(scheme.surface) else scheme.surfaceContainer,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(title, style = Ui.CardTitle)
                        if (isToday) AccentPill("сегодня")
                        Spacer(Modifier.weight(1f))
                        if (ordered.isNotEmpty()) {
                            Text(pairCount(ordered.size), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                        }
                    }
                    // Каждая пара анимируется сама по себе, без мерцания всей карточки:
                    // пара, которой не стало, уезжает; новая выезжает; если на том же месте другой предмет,
                    // он проявляется поверх прежнего; пары, которые не изменились, стоят на месте
                    val marks = if (isToday && filter.mode == 1) {
                        tick
                        NtmtApi.pairMarks(group, ordered.map { it.first })
                    } else emptyMap()
                    val now = ordered.toMap()
                    val slotKeys = remember(d) { mutableStateListOf<String>() }
                    val known = (slotKeys + ordered.map { it.first }).distinct()
                        .sortedBy { k -> NtmtApi.PAIRS.indexOf(k).let { if (it < 0) Int.MAX_VALUE else it } }
                    if (known.size != slotKeys.size) SideEffect {
                        slotKeys.clear()
                        slotKeys.addAll(known)
                    }
                    val move = tween<androidx.compose.ui.unit.IntSize>(260, easing = FastOutSlowInEasing)
                    // Когда карточка только появилась на экране, её пары уже на месте; выезжают лишь те, что добавились позже
                    val fresh = remember(d) { booleanArrayOf(true) }
                    AnimatedVisibility(
                        visible = ordered.isEmpty(),
                        enter = expandVertically(move, expandFrom = Alignment.Top) + fadeIn(tween(200)),
                        exit = shrinkVertically(move, shrinkTowards = Alignment.Top) + fadeOut(tween(120)),
                    ) {
                        EmptyPairs(today = d == catalog.todayName && numerator == catalog.weekTypeNumerator, big = false)
                    }
                    known.forEach { slot ->
                        key(slot) {
                            val lessons = now[slot]
                            // Уезжающая пара до конца анимации показывает то, что в ней было
                            val last = remember { mutableStateOf(lessons) }
                            if (lessons != null) last.value = lessons
                            val seen = remember { androidx.compose.animation.core.MutableTransitionState(fresh[0] && lessons != null) }
                            seen.targetState = lessons != null
                            AnimatedVisibility(
                                visibleState = seen,
                                enter = expandVertically(move, expandFrom = Alignment.Top) + fadeIn(tween(200, delayMillis = 60)),
                                exit = shrinkVertically(move, shrinkTowards = Alignment.Top) + fadeOut(tween(140)),
                            ) {
                                Column {
                                    if (ordered.firstOrNull()?.first != slot) RowDivider(start = 58.dp)
                                    AnimatedContent(
                                        targetState = last.value.orEmpty(),
                                        transitionSpec = {
                                            (fadeIn(tween(220)) togetherWith fadeOut(tween(220)))
                                                .using(SizeTransform(clip = false) { _, _ -> tween(240, easing = FastOutSlowInEasing) })
                                        },
                                        label = "pair",
                                    ) { shownLessons ->
                                        LessonRow(group, slot, shownLessons, marks[slot])
                                    }
                                }
                            }
                        }
                    }
                    SideEffect { fresh[0] = false }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
    // Список преподавателей и аудиторий открывается страницей поверх вкладки, как выбор группы из настроек,
    // а не диалогом: диалог затемнял системные панели
    val overlay = LocalOverlay.current
    if (picking) {
        DisposableEffect(Unit) {
            overlay.show(onClose = { picking = false }) {
                GroupPicker(
                    names = names,
                    current = picked,
                    title = pickTitle,
                    allowNone = false,
                    alwaysClose = true,
                    searchHint = if (filter.mode == 3) "Поиск аудитории" else "Поиск преподавателя",
                    mono = false,
                    onClose = { picking = false },
                    onPick = { name ->
                        if (filter.mode == 3) filter.room = name else filter.teacher = name
                        picking = false
                    },
                )
            }
            onDispose { overlay.hide() }
        }
    }
}

private fun pairCount(n: Int): String = when {
    n % 100 in 11..14 -> "$n пар"
    n % 10 == 1 -> "$n пара"
    n % 10 in 2..4 -> "$n пары"
    else -> "$n пар"
}

@Composable
internal fun BellsPane(group: String?, highlightCourse: Boolean, topTick: Int = 0) {
    val odd = group?.let { NtmtApi.courseOdd(it) } ?: true
    val first = Triple("1 и 3 курс", NtmtApi.BELLS_ODD, odd)
    val second = Triple("2 и 4 курс", NtmtApi.BELLS_EVEN, !odd)
    val tables = if (highlightCourse && group != null && !odd) listOf(second, first) else listOf(first, second)
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick++
        }
    }
    val scroll = rememberScrollState()
    LaunchedEffect(topTick) { if (topTick > 0) scroll.animateScrollTo(0, tween(320, easing = FastOutSlowInEasing)) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Обе сетки делят высоту экрана поровну, строки растягиваются: листать не нужно.
        // На совсем низком экране (или при крупном шрифте) строки не сжимаются, а список прокручивается
        val fit = maxHeight >= 560.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        Column(if (fit) Modifier.fillMaxSize() else Modifier.fillMaxSize().verticalScroll(scroll)) {
            ScreenHeader("Звонки", "Время пар по курсам", modifier = if (fit) Modifier.padding(bottom = 0.dp) else Modifier)
            if (fit) Spacer(Modifier.height(8.dp))
            tables.forEach { (title, rows, mine) ->
                val own = group != null && mine
                // Какая пара идёт сейчас, показываем только в сетке своего курса и не в воскресенье
                val marks = if (own && tick >= 0 && NtmtApi.today().dayOfWeek != DayOfWeek.SUNDAY) {
                    NtmtApi.pairMarks(group!!, rows.keys.mapNotNull { NtmtApi.PAIRS.getOrNull(it - 1) })
                } else emptyMap()
                BellCard(title, rows, active = highlightCourse && own, marks = marks, modifier = if (fit) Modifier.weight(1f) else Modifier, fill = fit)
                Spacer(Modifier.height(if (fit) 10.dp else 12.dp))
            }
            if (!fit) Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun BellCard(title: String, rows: Map<Int, String>, active: Boolean, marks: Map<String, String>, modifier: Modifier = Modifier, fill: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    BlockCard(
        modifier = modifier,
        highlighted = active,
        color = if (active) scheme.primaryContainer.copy(alpha = 0.35f).compositeOver(scheme.surface) else scheme.surfaceContainer,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = if (fill) 10.dp else 12.dp, bottom = if (fill) 2.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = Ui.CardTitle)
            if (active) AccentPill("ваш курс")
        }
        rows.entries.forEachIndexed { i, (n, t) ->
            val mark = marks[NtmtApi.PAIRS.getOrNull(n - 1)]
            if (i > 0) RowDivider(start = 58.dp)
            Row(
                (if (fill) Modifier.weight(1f) else Modifier).fillMaxWidth().padding(horizontal = 16.dp, vertical = if (fill) 0.dp else 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NumberDot("$n", size = if (fill) 28.dp else 30.dp, strong = mark == "now")
                Row(Modifier.weight(1f).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$n пара", style = MaterialTheme.typography.bodyLarge)
                    when (mark) {
                        "now" -> AccentPill("сейчас")
                        "next" -> Pill("следующая")
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    t.split("/").map { it.trim() }.filter { it.isNotEmpty() }.forEach { range ->
                        Text(
                            range.replace("–", " – "),
                            style = Ui.Time.copy(fontSize = if (fill) 15.sp else 16.sp, lineHeight = if (fill) 18.sp else 21.sp),
                            color = if (mark == "now") scheme.primary else scheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(if (fill) 4.dp else 6.dp))
    }
}

@Composable
internal fun EmptyPairs(today: Boolean, big: Boolean) {
    if (big) {
        StateBox(
            title = if (today) "Сегодня пар нет" else "Пар нет",
            hint = "Листайте дни свайпом или выберите день в полосе недели",
            emoji = "🎉",
            modifier = Modifier.padding(top = 24.dp),
        )
    } else {
        Text(
            if (today) "Сегодня пар нет 🎉" else "Пар нет",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 14.dp),
        )
    }
}

private fun pairNumber(key: String): String {
    val n = NtmtApi.PAIRS.indexOf(key) + 1
    return if (n > 0) "$n" else key.take(2)
}

private fun timeText(group: String, key: String, lessons: List<Lesson>): String {
    val timeGroup = lessons.firstNotNullOfOrNull { presentField(it.g) } ?: group
    return NtmtApi.pairRanges(timeGroup, key).joinToString("  ·  ") { (from, to) -> "$from – $to" }
}

/** Крупная карточка пары на вкладке «Сегодня» */
@Composable
private fun LessonCard(group: String, key: String, lessons: List<Lesson>, mark: String?, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val now = mark == "now"
    Surface(
        shape = Ui.Card,
        color = if (now) scheme.primaryContainer.copy(alpha = 0.45f).compositeOver(scheme.surface) else scheme.surfaceContainer,
        border = when (mark) {
            "now" -> BorderStroke(2.dp, scheme.primary)
            "next" -> BorderStroke(1.5.dp, scheme.outline)
            else -> null
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            NumberDot(pairNumber(key), size = 42.dp, strong = now)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        timeText(group, key, lessons),
                        style = Ui.Time,
                        color = if (now) scheme.primary else scheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    when (mark) {
                        "now" -> AccentPill("сейчас")
                        "next" -> Pill("следующая")
                    }
                }
                lessons.forEachIndexed { i, l ->
                    Spacer(Modifier.height(if (i == 0) 4.dp else 10.dp))
                    LessonText(l, maxLines = 4)
                }
            }
        }
    }
}

/** Плотная строка пары внутри карточки дня на вкладке «Неделя» */
@Composable
private fun LessonRow(group: String, key: String, lessons: List<Lesson>, mark: String?) {
    val scheme = MaterialTheme.colorScheme
    val now = mark == "now"
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
        NumberDot(pairNumber(key), size = 30.dp, strong = now)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(timeText(group, key, lessons), style = Ui.Time.copy(fontSize = 13.sp), color = if (now) scheme.primary else scheme.onSurfaceVariant)
                when (mark) {
                    "now" -> AccentPill("сейчас")
                    "next" -> Pill("следующая")
                }
            }
            lessons.forEachIndexed { i, l ->
                Spacer(Modifier.height(if (i == 0) 2.dp else 8.dp))
                LessonText(l, maxLines = 3)
            }
        }
    }
}

/** Название, подгруппа, аудитория и преподаватель одной записи */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LessonText(l: Lesson, maxLines: Int) {
    val scheme = MaterialTheme.colorScheme
    val room = presentField(l.a)
    val teacher = presentField(l.p)
    val own = presentField(l.g)
    Text(presentField(l.n) ?: "Занятие", style = Ui.Lesson, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    if (own != null) {
        Text(own, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
    }
    if (room != null || teacher != null) {
        FlowRow(
            Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (room != null) Meta(Icons.Outlined.MeetingRoom, "ауд. $room", scheme.primary, FontWeight.SemiBold)
            if (teacher != null) Meta(Icons.Outlined.Person, teacher, scheme.onSurfaceVariant, FontWeight.Normal)
        }
    }
}

@Composable
private fun Meta(icon: ImageVector, text: String, color: Color, weight: FontWeight) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = weight)
    }
}

internal fun presentField(v: String?): String? {
    val s = v?.trim().orEmpty()
    if (s.isEmpty() || s.equals("null", true) || s.equals("undefined", true) || s == "-" || s == "—") return null
    return s
}
