package ntmt.schedule.ui

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Палитра 3.0: один красный тон на всех ролях. Раньше вторичный цвет был синим, а незаданные роли
// (третичный, оттенок поверхности, обводки) брались из стандартной фиолетовой схемы и спорили с красным.
private val Light = lightColorScheme(
    primary = Color(0xFFB3261E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF410001),
    inversePrimary = Color(0xFFFFB4A9),
    secondary = Color(0xFF775651),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF7DDD9),
    onSecondaryContainer = Color(0xFF2C1512),
    tertiary = Color(0xFF705C2E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFBDFA6),
    onTertiaryContainer = Color(0xFF251A00),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFA),
    onBackground = Color(0xFF201A19),
    surface = Color(0xFFFFFBFA),
    onSurface = Color(0xFF201A19),
    surfaceVariant = Color(0xFFF1E4E2),
    onSurfaceVariant = Color(0xFF524442),
    surfaceTint = Color(0xFFB3261E),
    inverseSurface = Color(0xFF362F2E),
    inverseOnSurface = Color(0xFFFBEEEC),
    outline = Color(0xFF857371),
    outlineVariant = Color(0xFFD8C8C5),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFBFA),
    surfaceDim = Color(0xFFE4DAD8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF3F2),
    surfaceContainer = Color(0xFFF5EEEC),
    surfaceContainerHigh = Color(0xFFEEE7E5),
    surfaceContainerHighest = Color(0xFFE8E1DF),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB4A9),
    onPrimary = Color(0xFF5F0A08),
    primaryContainer = Color(0xFF7E1F1A),
    onPrimaryContainer = Color(0xFFFFDAD5),
    inversePrimary = Color(0xFFB3261E),
    secondary = Color(0xFFE7BDB7),
    onSecondary = Color(0xFF442926),
    secondaryContainer = Color(0xFF4F3532),
    onSecondaryContainer = Color(0xFFFFDAD5),
    tertiary = Color(0xFFDEC48C),
    onTertiary = Color(0xFF3E2E04),
    tertiaryContainer = Color(0xFF564419),
    onTertiaryContainer = Color(0xFFFBDFA6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF161312),
    onBackground = Color(0xFFECE0DE),
    surface = Color(0xFF161312),
    onSurface = Color(0xFFECE0DE),
    surfaceVariant = Color(0xFF524442),
    onSurfaceVariant = Color(0xFFD8C2BE),
    surfaceTint = Color(0xFFFFB4A9),
    inverseSurface = Color(0xFFECE0DE),
    inverseOnSurface = Color(0xFF362F2E),
    outline = Color(0xFFA08C89),
    outlineVariant = Color(0xFF524442),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3D3837),
    surfaceDim = Color(0xFF161312),
    surfaceContainerLowest = Color(0xFF100E0D),
    surfaceContainerLow = Color(0xFF1E1B1A),
    surfaceContainer = Color(0xFF231F1E),
    surfaceContainerHigh = Color(0xFF2D2928),
    surfaceContainerHighest = Color(0xFF383433),
)

/**
 * Цвет текста и значков поверх произвольного фона: белый на тёмном, тёмный на светлом.
 * Нужен там, где фон приходит из системной палитры (Material You) и заранее неизвестен:
 * пара «цвет / цвет поверх него» из схемы не всегда даёт читаемый контраст.
 */
internal fun Color.readable(prefer: Color): Color {
    fun contrast(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return if (la > lb) la / lb else lb / la
    }
    if (contrast(this, prefer) >= 4.5f) return prefer
    val light = Color(0xFFFFFFFF)
    val dark = Color(0xFF201A19)
    return if (contrast(this, light) >= contrast(this, dark)) light else dark
}

private val Type = Typography(
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Normal, lineHeight = 28.sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Normal, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp),
    titleSmall = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
)

@Composable
fun NtmtTheme(
    dark: Boolean = isSystemInDarkTheme(),
    materialYou: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Схема Material You собирается из полусотни системных цветов. Раньше она пересобиралась при каждой
    // перерисовке корня приложения (смена дня, вкладки, тик времени), и каждый раз это была новая схема:
    // заново перерисовывался весь экран. Отсюда подтормаживания, особенно на Pixel с адаптивной темой.
    // Теперь схема создаётся один раз и меняется только вместе с темой или обоями
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val scheme = androidx.compose.runtime.remember(dark, materialYou, config) {
        if (materialYou && Build.VERSION.SDK_INT >= 31) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else if (dark) {
            Dark
        } else {
            Light
        }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        // Фон окна и цвет системных панелей выставляются только при смене цвета, а не на каждую перерисовку
        val back = scheme.background
        androidx.compose.runtime.LaunchedEffect(back, view) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            window.setBackgroundDrawable(ColorDrawable(back.toArgb()))
            window.statusBarColor = back.toArgb()
            window.navigationBarColor = back.toArgb()
            // Значки строки состояния и панели навигации следуют теме приложения, а не системы:
            // иначе при тёмной теме в светлой системе часы и значки остаются тёмными на тёмном
            val light = back.luminance() > 0.5f
            androidx.core.view.WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightStatusBars = light
                isAppearanceLightNavigationBars = light
            }
        }
    }
    MaterialTheme(colorScheme = scheme, typography = Type, content = content)
}
