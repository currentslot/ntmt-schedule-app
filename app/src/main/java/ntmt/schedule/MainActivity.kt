package ntmt.schedule

import android.content.Intent
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.flow.MutableSharedFlow
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
        setContent { NtmtAppUi() }
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                ask.launch(Manifest.permission.POST_NOTIFICATIONS)
            } catch (_: Exception) {
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        offerTab(intent)
    }

    private fun offerTab(intent: Intent?) {
        intent?.getStringExtra(ScheduleWidget.EXTRA_TAB)?.let { WidgetOpen.tabs.tryEmit(it) }
        if (intent?.getBooleanExtra(EXTRA_UPDATES, false) == true) UpdateOpen.events.tryEmit(Unit)
    }

    companion object {
        const val EXTRA_UPDATES = "open_updates"
    }

    override fun onResume() {
        super.onResume()
        ChangeWatch.enqueue(this)
    }
}
