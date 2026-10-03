package ntmt.schedule.widget

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Build
import android.widget.RemoteViews
import ntmt.schedule.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class WidgetPalette(
    val night: Boolean,
    val bg: Int,
    val fg: Int,
    val muted: Int,
    val pill: Int,
    val dynamic: Boolean = false,
) {
    fun textTone(base: Int, percent: Int): Int {
        val t = (100 - percent.coerceIn(10, 100)) / 90f
        val toward = if (night) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        return opaque(mix(base, toward, t * 0.55f))
    }

    fun pillTone(percent: Int): Int {
        val t = (100 - percent.coerceIn(10, 100)) / 90f
        val toward = if (night) 0xFF6A6570.toInt() else 0xFFB7AEB4.toInt()
        return opaque(mix(pill, toward, t))
    }

    companion object {
        fun of(night: Boolean) = if (night) {
            WidgetPalette(true, 0xFF1C1B1F.toInt(), 0xFFE6E1E5.toInt(), 0xFFCAC4D0.toInt(), 0xFF36343B.toInt())
        } else {
            WidgetPalette(false, 0xFFF7F2F4.toInt(), 0xFF1C1B1F.toInt(), 0xFF79747E.toInt(), 0xFFE7E0E4.toInt())
        }

        fun resolve(context: Context, night: Boolean, materialYou: Boolean): WidgetPalette {
            val classic = of(night)
            if (!materialYou || Build.VERSION.SDK_INT < 31) return classic
            return try {
                val bg = context.getColor(if (night) android.R.color.system_neutral1_800 else android.R.color.system_accent2_100)
                val fg = context.getColor(if (night) android.R.color.system_neutral1_50 else android.R.color.system_neutral1_900)
                val muted = context.getColor(if (night) android.R.color.system_neutral2_200 else android.R.color.system_accent2_700)
                val pill = context.getColor(if (night) android.R.color.system_accent1_200 else android.R.color.system_accent1_600)
                var safeBg = opaque(bg)
                var safeFg = opaque(fg)
                if (contrast(safeBg, safeFg) < 4.0) {
                    safeFg = if (night) 0xFFF4F1F4.toInt() else 0xFF1C1B1F.toInt()
                }
                if (contrast(safeBg, safeFg) < 3.2) {
                    safeBg = opaque(mix(safeBg, if (night) 0xFF000000.toInt() else 0xFFFFFFFF.toInt(), 0.5f))
                }
                val safeMuted = if (contrast(safeBg, muted) < 2.2) safeFg else opaque(muted)
                val rawPill = opaque(pill)
                val safePill = if (contrast(safeBg, rawPill) < 1.25) {
                    opaque(mix(safeBg, safeFg, if (night) 0.28f else 0.18f))
                } else {
                    rawPill
                }
                WidgetPalette(night, safeBg, safeFg, safeMuted, safePill, dynamic = true)
            } catch (_: Exception) {
                classic
            }
        }

        fun night(context: Context, theme: String, config: Configuration? = null): Boolean = when (theme) {
            "dark" -> true
            "light" -> false
            else -> {
                val ui = (config ?: context.resources.configuration).uiMode and Configuration.UI_MODE_NIGHT_MASK
                ui == Configuration.UI_MODE_NIGHT_YES
            }
        }

        fun tint(rgb: Int, percent: Int): Int {
            val a = percent.coerceIn(10, 100) * 255 / 100
            return (a shl 24) or (rgb and 0x00FFFFFF)
        }

        fun opaque(rgb: Int): Int = (0xFF shl 24) or (rgb and 0x00FFFFFF)

        fun mix(from: Int, to: Int, t: Float): Int {
            fun ch(src: Int, dst: Int) = (src + (dst - src) * t).toInt().coerceIn(0, 255)
            val fr = (from shr 16) and 0xFF
            val fg = (from shr 8) and 0xFF
            val fb = from and 0xFF
            val tr = (to shr 16) and 0xFF
            val tg = (to shr 8) and 0xFF
            val tb = to and 0xFF
            return (ch(fr, tr) shl 16) or (ch(fg, tg) shl 8) or ch(fb, tb)
        }

        fun contrast(a: Int, b: Int): Double {
            val hi = max(luminance(a), luminance(b))
            val lo = min(luminance(a), luminance(b))
            return (hi + 0.05) / (lo + 0.05)
        }

        private fun luminance(color: Int): Double {
            fun ch(v: Int): Double {
                val s = v / 255.0
                return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * ch((color shr 16) and 0xFF) +
                0.7152 * ch((color shr 8) and 0xFF) +
                0.0722 * ch(color and 0xFF)
        }
    }
}

internal fun RemoteViews.applyTone(
    viewId: Int,
    method: String,
    day: Int,
    night: Int,
    followSystem: Boolean,
    nightNow: Boolean,
) {
    if (followSystem && Build.VERSION.SDK_INT >= 31) {
        setColorInt(viewId, method, day, night)
    } else {
        val color = if (nightNow) night else day
        if (method == "setTextColor") setTextColor(viewId, color) else setInt(viewId, method, color)
    }
}

internal fun RemoteViews.applyChip(
    viewId: Int,
    day: Int,
    night: Int,
    followSystem: Boolean,
    nightNow: Boolean,
    dynamic: Boolean,
) {
    if ((dynamic || followSystem) && Build.VERSION.SDK_INT >= 31) {
        setInt(viewId, "setBackgroundResource", R.drawable.widget_pill_mask)
        val dayColor = if (dynamic) day else 0xFFD5CED3.toInt()
        val nightColor = if (dynamic) night else 0xFF36343B.toInt()
        if (followSystem) {
            setColorStateList(
                viewId,
                "setBackgroundTintList",
                ColorStateList.valueOf(dayColor),
                ColorStateList.valueOf(nightColor),
            )
        } else {
            setColorStateList(
                viewId,
                "setBackgroundTintList",
                ColorStateList.valueOf(if (nightNow) nightColor else dayColor),
            )
        }
    } else {
        setInt(
            viewId,
            "setBackgroundResource",
            if (nightNow) R.drawable.widget_pill_dark else R.drawable.widget_pill_light,
        )
    }
}
