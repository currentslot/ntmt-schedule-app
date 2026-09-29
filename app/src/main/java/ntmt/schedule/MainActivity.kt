package ntmt.schedule

import android.Manifest
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ntmt.schedule.data.Prefs
import ntmt.schedule.notify.ChangeWatch
import ntmt.schedule.ui.NtmtAppUi
import ntmt.schedule.widget.ScheduleWidget

object WidgetOpen {
    val tabs = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
}

object UpdateOpen {
    val events = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
}

class MainActivity : ComponentActivity() {
    private val ask = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        offerTab(intent)
        try {
            enableEdgeToEdge()
        } catch (_: Exception) {
            // older OEM skins
        }
        try {
            val st = runBlocking { Prefs(this@MainActivity).state.first() }
            paintWindow(st.theme, st.materialYou)
        } catch (_: Exception) {
            paintWindow("auto", true)
        }
        setContent { NtmtAppUi() }
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                ask.launch(Manifest.permission.POST_NOTIFICATIONS)
            } catch (_: Exception) {
            }
        }
    }

    private fun paintWindow(theme: String, materialYou: Boolean) {
        val night = when (theme) {
            "dark" -> true
            "light" -> false
            else -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        val color = if (materialYou && Build.VERSION.SDK_INT >= 31) {
            getColor(if (night) android.R.color.system_neutral1_900 else android.R.color.system_neutral1_10)
        } else if (night) {
            0xFF141218.toInt()
        } else {
            0xFFFFFBFF.toInt()
        }
        window.setBackgroundDrawable(ColorDrawable(color))
        window.statusBarColor = color
        window.navigationBarColor = color
        window.decorView.setBackgroundColor(color)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        offerTab(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        ChangeWatch.onWidgetUpdated(this)
    }

    private fun offerTab(intent: Intent?) {
        intent?.getStringExtra(ScheduleWidget.EXTRA_TAB)?.let { WidgetOpen.tabs.tryEmit(it) }
        if (intent?.getBooleanExtra(EXTRA_UPDATES, false) == true) UpdateOpen.events.tryEmit(Unit)
    }

    companion object {
        const val EXTRA_UPDATES = "open_updates"
    }
}
