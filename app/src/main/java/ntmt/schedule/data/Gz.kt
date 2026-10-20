package ntmt.schedule.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Сжатие для QR и NFC. Распаковка с потолком, чтобы подсунутый код не забил память */
internal object Gz {
    const val SCHEDULE_LIMIT = 8 * 1024 * 1024
    const val FILE_LIMIT = 32 * 1024 * 1024

    // Максимальное сжатие: меньше QR-кодов и короче передача по NFC
    fun pack(bytes: ByteArray): ByteArray = ByteArrayOutputStream().use { out ->
        object : GZIPOutputStream(out, 64 * 1024) {
            init {
                def.setLevel(java.util.zip.Deflater.BEST_COMPRESSION)
            }
        }.use { it.write(bytes) }
        out.toByteArray()
    }

    /** null, если данные повреждены или после распаковки больше [limit] байт */
    fun unpack(raw: ByteArray, limit: Int): ByteArray? = try {
        GZIPInputStream(ByteArrayInputStream(raw)).use { input ->
            val out = ByteArrayOutputStream(minOf(limit, maxOf(raw.size * 4, 8192)))
            val buf = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                total += n
                if (total > limit) return null
                out.write(buf, 0, n)
            }
            out.toByteArray()
        }
    } catch (_: Exception) {
        null
    }
}
