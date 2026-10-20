package ntmt.schedule.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ntmt.schedule.data.Prefs
import ntmt.schedule.notify.Notify

class BackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        val st = prefs.state.first().backup
        val manual = inputData.getBoolean(Backup.KEY_MANUAL, false)
        val cabinetOnly = inputData.getBoolean(Backup.KEY_CABINET, false)
        if (!manual && !st.on) return Result.success()
        if (st.dest == null) return Result.success()
        // Копии работают только с настроенным быстрым входом
        if (!ntmt.schedule.data.CabinetVault.has(applicationContext)) {
            withContext(NonCancellable) { prefs.setBackupResult(false, st.lastAt, "Нужен быстрый вход в кабинет", null) }
            return Result.success()
        }
        val problems = mutableListOf<String>()
        if (!cabinetOnly) {
            val local = runCatching { Backup.runLocal(applicationContext, st, prefs.backupPrint(false), force = manual) { isStopped } }
                .getOrElse { Backup.Outcome("Ошибка: ${it.message ?: it.javaClass.simpleName}", null, true) }
            withContext(NonCancellable) {
                prefs.setBackupResult(false, if (local.cancelled) st.lastAt else System.currentTimeMillis(), local.summary, local.print)
            }
            if (local.failed) problems += local.summary
            if (local.cancelled || isStopped) return Result.success()
        }
        // Папки кабинета: фоном, со своим входом из BackupVault, без участия пользователя
        if (st.cabinet && st.cabinetFolders.isNotEmpty()) {
            val cab = runCatching { Backup.runCabinet(applicationContext, st, prefs.backupPrint(true), force = manual && !cabinetOnly) { isStopped } }
                .getOrElse { Backup.Outcome("Ошибка: ${it.message ?: it.javaClass.simpleName}", null, true) }
            withContext(NonCancellable) {
                if (cab.gone.isNotEmpty()) prefs.dropCabinetFolders(cab.gone)
                prefs.setBackupResult(true, if (cab.failed || cab.cancelled) st.cabinetAt else System.currentTimeMillis(), cab.summary, cab.print)
            }
            if (cab.cancelled) return Result.success()
            if (cab.failed && (manual || cabinetOnly)) problems += cab.summary
        }
        if (problems.isNotEmpty() && !manual) {
            Notify.show(applicationContext, 51, Notify.CH_BACKUP, "Резервная копия не создана", problems.joinToString("\n"))
        }
        return Result.success()
    }
}
