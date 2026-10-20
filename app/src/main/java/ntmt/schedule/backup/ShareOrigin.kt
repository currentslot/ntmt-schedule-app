package ntmt.schedule.backup

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Откуда на сервере скачан файл из «Скачанных»: цепочка папок кабинета.
 * По ней архив раскладывает скачанное так же, как на сервере.
 */
object ShareOrigin {
    private const val FILE = "share-origin.json"
    private val lock = Any()

    fun remember(ctx: Context, fileName: String, folders: List<String>) {
        // Список файлов берётся до блокировки: Downloads сам вызывает ShareOrigin под своей
        val alive = ntmt.schedule.data.Downloads.allNames(ctx)
        synchronized(lock) { write(ctx, fileName, folders, alive) }
    }

    private fun write(ctx: Context, fileName: String, folders: List<String>, alive: Set<String>) {
        val root = read(ctx)
        // Записи об удалённых файлах больше не нужны
        root.keys().asSequence().toList().filter { it !in alive && it != fileName }.forEach { root.remove(it) }
        root.put(fileName, JSONArray(folders))
        runCatching { File(ctx.filesDir, FILE).writeText(root.toString()) }
    }

    fun all(ctx: Context): Map<String, List<String>> = synchronized(lock) {
        val root = read(ctx)
        root.keys().asSequence().associateWith { key ->
            val arr = root.optJSONArray(key) ?: JSONArray()
            List(arr.length()) { arr.optString(it) }
        }
    }

    private fun read(ctx: Context): JSONObject = runCatching {
        JSONObject(File(ctx.filesDir, FILE).readText())
    }.getOrElse { JSONObject() }
}
