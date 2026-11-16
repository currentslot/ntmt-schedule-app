package ntmt.schedule.notify

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import ntmt.schedule.data.Prefs

/** Принимает пуш сервера о том, что расписание на сайте изменилось */
class PushService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"]
        if (type == "test") {
            // Тест сервера тем же путём, что и настоящий пуш (только data, показывает приложение)
            val title = message.data["title"] ?: return
            Notify.show(applicationContext, 91, Notify.CH_CHANGE, title, message.data["text"].orEmpty())
            return
        }
        if (type == "update") {
            onUpdatePush(message.data)
            return
        }
        if (type != "schedule") return
        val prefs = Prefs(applicationContext)
        val done = runBlocking {
            val st = prefs.state.first()
            if (!st.notify || !st.push) {
                Push.sync(applicationContext, false)
                return@runBlocking true
            }
            // Сервер говорит только, что что-то поменялось. Что именно у своей группы,
            // приложение проверяет само: так уведомление совпадает с тем, что видно в расписании
            withTimeoutOrNull(15_000) { ScheduleCheck.pull(applicationContext, prefs, st, allowNotify = true) } == true
        }
        if (!done) {
            val req = OneTimeWorkRequestBuilder<ScheduleWorker>()
                .setInputData(workDataOf("reason" to "push"))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(applicationContext).enqueueUniqueWork("ntmt-push", ExistingWorkPolicy.REPLACE, req)
        }
    }

    /** Сервер увидел на GitHub новый выпуск канала. Показываем, только если он новее установленного */
    private fun onUpdatePush(data: Map<String, String>) {
        val app = applicationContext
        if (data["test"] == "1") {
            Notify.showUpdate(app, "Проверка уведомления об обновлении", "Пуши о новых версиях с сервера доходят")
            return
        }
        val code = data["code"]?.toIntOrNull() ?: return
        val prefs = Prefs(app)
        runBlocking {
            val st = prefs.state.first()
            val channel = if (data["channel"] == "stable") "stable" else "beta"
            if (!st.updateNotify || channel != st.updateChannel) {
                Push.syncUpdates(app, st.updateChannel, st.updateNotify)
                return@runBlocking
            }
            if (code <= ntmt.schedule.update.AppUpdate.installedCode(app)) return@runBlocking
            prefs.setAvailableUpdate(code)
            // remind=1: ручное напоминание с сервера, показывается, даже если об этой версии уже сообщали
            if (st.lastNotifiedUpdate != code || data["remind"] == "1") {
                Notify.showUpdate(app, "Доступно обновление", "${data["name"].orEmpty().ifBlank { "Новая версия" }}. Откройте, чтобы скачать.")
                prefs.setLastNotifiedUpdate(code)
            }
        }
    }

    override fun onNewToken(token: String) {
        // Новый токен: подписку на топики нужно повторить
        val st = runBlocking { Prefs(applicationContext).state.first() }
        Push.sync(applicationContext, st.notify && st.push)
        Push.syncUpdates(applicationContext, st.updateChannel, st.updateNotify)
    }
}
