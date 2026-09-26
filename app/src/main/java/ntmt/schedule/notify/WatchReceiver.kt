package ntmt.schedule.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WatchReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (
            action == Intent.ACTION_SCREEN_ON ||
            action == Intent.ACTION_USER_PRESENT ||
            action == Intent.ACTION_BOOT_COMPLETED
        ) {
            ChangeWatch.enqueue(context)
        }
    }
}
