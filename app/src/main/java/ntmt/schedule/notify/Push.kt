package ntmt.schedule.notify

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Пуши об изменениях расписания. Сервер (Yandex Cloud Functions) проверяет сайт раз в 15–35 минут
 * и при изменении шлёт data-сообщение на топик [TOPIC]. Подписка включена, только когда
 * включены «Изменения расписания» и «Мгновенно через сервер».
 * Без сервисов Google подписка просто не получится, и остаётся проверка самим телефоном.
 */
object Push {
    const val TOPIC = "schedule"

    /** Топики сервера о новых версиях приложения: свой у каждого канала обновлений */
    fun updateTopic(channel: String) = if (channel == "stable") "update-stable" else "update-beta"

    /** Подписка на пуш о новой версии: только на выбранный канал и только при включённом уведомлении */
    fun syncUpdates(ctx: Context, channel: String, enabled: Boolean) {
        try {
            val fm = FirebaseMessaging.getInstance()
            val mine = updateTopic(channel)
            listOf("update-stable", "update-beta").forEach { topic ->
                if (enabled && topic == mine) fm.subscribeToTopic(topic) else fm.unsubscribeFromTopic(topic)
            }
        } catch (_: Throwable) {
        }
    }

    fun sync(ctx: Context, enabled: Boolean) {
        try {
            val fm = FirebaseMessaging.getInstance()
            if (enabled) fm.subscribeToTopic(TOPIC) else fm.unsubscribeFromTopic(TOPIC)
        } catch (_: Throwable) {
            // Нет Google Play Services или Firebase не поднялся: работаем без пушей
        }
    }
}
