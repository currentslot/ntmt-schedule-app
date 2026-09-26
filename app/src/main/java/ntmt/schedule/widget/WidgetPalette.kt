package ntmt.schedule.widget

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration

data class WidgetPalette(
    val night: Boolean,
    val bg: Int,
    val fg: Int,
    val muted: Int,
    val pill: Int,
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

        fun night(context: Context, theme: String): Boolean = when (theme) {
            "dark" -> true
            "light" -> false
            else -> systemNight(context)
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

        private fun systemNight(context: Context): Boolean {
            val ui = context.getSystemService(UiModeManager::class.java)
            return when (ui?.nightMode) {
                UiModeManager.MODE_NIGHT_YES -> true
                UiModeManager.MODE_NIGHT_NO -> false
                else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
}
