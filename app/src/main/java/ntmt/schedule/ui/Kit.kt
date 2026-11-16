package ntmt.schedule.ui

import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Общие элементы оформления 3.0: одни и те же отступы, скругления и подписи на всех вкладках.
 * Здесь только внешний вид, без данных и логики.
 */
internal object Ui {
    /** Карточка-блок: день, пара, раздел настроек */
    val Card = RoundedCornerShape(20.dp)

    /** Вложенный элемент внутри карточки и кнопки-плашки */
    val Inner = RoundedCornerShape(14.dp)

    /** Заголовок экрана */
    val Title = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)

    /** Заголовок карточки: день недели, курс */
    val CardTitle = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)

    /** Название пары */
    val Lesson = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium)

    /** Время пары: цифры одной ширины, чтобы столбцы не прыгали */
    val Time = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")

    /** Смена цвета при выборе: коротко, чтобы не отставать от пальца */
    val Tint = tween<Color>(220, easing = FastOutSlowInEasing)
}

/**
 * Появление элемента: проявляется и поднимается на место. [index] сдвигает начало,
 * чтобы строки списка появлялись по очереди, а не все сразу.
 */
internal fun Modifier.appear(index: Int = 0): Modifier = composed {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        shown.animateTo(1f, tween(260, delayMillis = index.coerceIn(0, 8) * 35, easing = FastOutSlowInEasing))
    }
    val lift = with(LocalDensity.current) { 14.dp.toPx() }
    graphicsLayer {
        alpha = shown.value
        translationY = (1f - shown.value) * lift
    }
}

/**
 * Нажатие с лёгким уменьшением. Ставится ПЕРЕД фоном элемента и получает его форму:
 * тогда фон, обрезка и волна нажатия уменьшаются вместе и волна повторяет скругление
 */
internal fun Modifier.pressable(
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.ui.graphics.RectangleShape,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "press")
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clip(shape).clickable(interactionSource = source, indication = ripple(), enabled = enabled, onClick = onClick)
}

/** Заголовок вкладки: крупное название, пояснение под ним и действия справа */
@Composable
internal fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(
                // Нажатие по всему заголовку: волны нет, на нажатие отвечает кнопка справа
                if (onClick != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick) else Modifier,
            )
            .padding(top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Ui.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), content = actions)
    }
}

/** Круглая кнопка-значок с подложкой: зона нажатия 40 dp вместо голого значка 22 dp */
@Composable
internal fun TonalIcon(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    size: Dp = 40.dp,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val back by animateColorAsState(if (active) scheme.primary else scheme.surfaceContainerHigh, Ui.Tint, label = "tonal-bg")
    val tint by animateColorAsState(if (active) scheme.primary.readable(scheme.onPrimary) else scheme.onSurface, Ui.Tint, label = "tonal-fg")
    Box(
        modifier
            .size(size)
            .pressable(CircleShape, onClick = onClick)
            .background(back),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Небольшая метка-состояние: «Сейчас», «Завтра», «Числитель». Не кнопка, поэтому без рамки и тени */
@Composable
internal fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = Color.Unspecified,
    icon: ImageVector? = null,
) {
    val fg = container.readable(if (content.isSpecified) content else MaterialTheme.colorScheme.onSecondaryContainer)
    Surface(shape = CircleShape, color = container, modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
            Text(text, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
        }
    }
}

/** Метка-акцент на основном цвете: «сегодня», «сейчас», «Ваш курс» */
@Composable
internal fun AccentPill(text: String, modifier: Modifier = Modifier) =
    Pill(text, modifier, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)

/**
 * Переключатель из нескольких положений во всю ширину: видно все варианты и выбранный.
 * Подложка выбранного положения переезжает на новое место, а не перескакивает.
 * [marked] помечает положение подписью [markLabel] (текущая неделя).
 */
@Composable
internal fun Segmented(
    options: List<String>,
    selected: Int,
    modifier: Modifier = Modifier,
    marked: Int? = null,
    markLabel: String = "сейчас",
    onSelect: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val onAccent = scheme.primary.readable(scheme.onPrimary)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(scheme.surfaceContainerHigh)
            .padding(4.dp),
    ) {
        val gap = 4.dp
        val cell = (maxWidth - gap * (options.size - 1)) / options.size.coerceAtLeast(1)
        if (selected in options.indices) {
            val x by animateDpAsState(
                (cell + gap) * selected,
                spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                label = "segment",
            )
            Box(Modifier.offset(x = x).width(cell).height(40.dp).clip(CircleShape).background(scheme.primary))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            options.forEachIndexed { i, label ->
                val on = i == selected
                val fg by animateColorAsState(if (on) onAccent else scheme.onSurfaceVariant, Ui.Tint, label = "segment-fg")
                Row(
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(CircleShape)
                        .clickable { onSelect(i) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                ) {
                    Text(
                        label,
                        color = fg,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (marked == i) {
                        val markBg by animateColorAsState(if (on) onAccent.copy(alpha = 0.22f) else scheme.primaryContainer, Ui.Tint, label = "mark-bg")
                        val markFg by animateColorAsState(if (on) onAccent else scheme.primaryContainer.readable(scheme.onPrimaryContainer), Ui.Tint, label = "mark-fg")
                        Surface(shape = CircleShape, color = markBg) {
                            Text(
                                markLabel,
                                color = markFg,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Кружок с номером пары */
@Composable
internal fun NumberDot(text: String, size: Dp = 40.dp, strong: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val back by animateColorAsState(if (strong) scheme.primary else scheme.primaryContainer, Ui.Tint, label = "dot-bg")
    val fg = if (strong) scheme.primary.readable(scheme.onPrimary) else scheme.primaryContainer.readable(scheme.onPrimaryContainer)
    Box(
        Modifier.size(size).clip(CircleShape).background(back),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = fg,
            style = TextStyle(fontSize = if (size >= 36.dp) 17.sp else 14.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

/** Карточка-блок с необязательной рамкой выделения */
@Composable
internal fun BlockCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = Ui.Card,
        color = color,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = if (highlighted) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

/** Тонкая линия между строками внутри карточки */
@Composable
internal fun RowDivider(start: Dp = 16.dp) {
    HorizontalDivider(
        Modifier.padding(start = start, end = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

/** Состояние на весь экран: загрузка, ошибка, пусто */
@Composable
internal fun StateBox(
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    emoji: String? = null,
    loading: Boolean = false,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier.fillMaxWidth().appear().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
            Spacer(Modifier.height(14.dp))
        } else if (emoji != null) {
            Text(emoji, style = TextStyle(fontSize = 36.sp))
            Spacer(Modifier.height(8.dp))
        }
        Text(title, style = Ui.CardTitle, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (action != null) {
            Button(onClick = onAction, shape = Ui.Inner, modifier = Modifier.padding(top = 14.dp)) { Text(action) }
        }
    }
}

/** Значок строки настроек в круге: тот же мотив, что у номера пары */
@Composable
internal fun IconBadge(icon: ImageVector, size: Dp = 36.dp) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.size(size).clip(CircleShape).background(scheme.primaryContainer), contentAlignment = Alignment.Center) {
        Icon(
            icon,
            contentDescription = null,
            tint = scheme.primaryContainer.readable(scheme.onPrimaryContainer),
            modifier = Modifier.size(size * 0.56f),
        )
    }
}

/** Вариант выбора из нескольких, когда они не помещаются в один ряд */
@Composable
internal fun Choice(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val back by animateColorAsState(if (selected) scheme.primary else scheme.surfaceContainerHigh, Ui.Tint, label = "choice-bg")
    val fg by animateColorAsState(if (selected) scheme.primary.readable(scheme.onPrimary) else scheme.onSurfaceVariant, Ui.Tint, label = "choice-fg")
    Box(
        modifier
            .heightIn(min = 36.dp)
            .pressable(CircleShape, onClick = onClick)
            .background(back)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), maxLines = 1, softWrap = false)
    }
}

/** Небольшая кнопка-действие в строке: «Сменить», «Выбрать» */
@Composable
internal fun TextAction(text: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .heightIn(min = 34.dp)
            .pressable(CircleShape, onClick = onClick)
            .background(scheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = scheme.primary, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), maxLines = 1, softWrap = false)
    }
}

@Composable
internal fun CenterState(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}


/** Страница поверх вкладок: её рисует корень приложения в том же окне, поэтому системные панели не затемняются */
internal class OverlayHost {
    var content by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set
    private var closer: (() -> Unit)? = null

    fun show(onClose: () -> Unit, page: @Composable () -> Unit) {
        closer = onClose
        content = page
    }

    fun hide() {
        content = null
        closer = null
    }

    /** Кнопка «Назад»: сообщить владельцу страницы, он её уберёт */
    fun back() {
        closer?.invoke() ?: hide()
    }
}

internal val LocalOverlay = androidx.compose.runtime.staticCompositionLocalOf { OverlayHost() }
