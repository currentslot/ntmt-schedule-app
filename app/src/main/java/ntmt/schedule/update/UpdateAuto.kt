package ntmt.schedule.update

import android.content.Context
import kotlinx.coroutines.flow.first
import ntmt.schedule.data.NtmtApi
import ntmt.schedule.data.Prefs
import ntmt.schedule.notify.Notify

object UpdateAuto {
    const val WIDGET_GAP_MS = 6L * 60L * 60L * 1000L

    suspend fun run(ctx: Context, force: Boolean) {
        val app = ctx.applicationContext
        val prefs = Prefs(app)
        val st = prefs.state.first()
        if (!st.updateAuto) return
        val now = System.currentTimeMillis()
        if (!force && now - st.lastUpdateCheckAt < WIDGET_GAP_MS) return
        if (!NtmtApi.online(app)) return
        try {
            val code = AppUpdate.installedCode(app)
            val found = AppUpdate.check(AppUpdate.SOURCE_SERVER, st.updateChannel, code)
            prefs.setLastUpdateCheckAt(System.currentTimeMillis())
            val release = found.release
            if (found.newer && release != null) {
                prefs.setAvailableUpdate(release.versionCode)
                if (st.updateNotify && st.lastNotifiedUpdate != release.versionCode) {
                    Notify.showUpdate(
                        app,
                        "Доступно обновление",
                        "${release.versionName}. Откройте, чтобы скачать.",
                    )
                    prefs.setLastNotifiedUpdate(release.versionCode)
                }
            } else if (found.error == null) {
                prefs.setAvailableUpdate(0)
            }
        } catch (_: Exception) {
        }
    }
}
