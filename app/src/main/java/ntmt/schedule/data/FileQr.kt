package ntmt.schedule.data

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32

object FileQr {
    /** Не больше стольких кодов на файл */
    const val MAX_FRAMES = 50

    // Приём принимает и коды старых версий (до 80 штук)
    private const val ACCEPT_FRAMES = 80

    /** Сколько байт (после сжатия) помещается в [MAX_FRAMES] кодов */
    const val MAX_RAW = MAX_FRAMES * 1800

    /** Имя и содержимое файла в одном блоке: 2 байта длины имени, имя UTF-8, данные. Тот же формат идёт по NFC */
    internal fun blob(name: String, bytes: ByteArray): ByteArray? {
        val safe = name.replace('\u0000', ' ').trim().take(120).ifBlank { "file" }
        val head = safe.toByteArray(Charsets.UTF_8)
        if (head.size > 240) return null
        val blob = ByteArray(2 + head.size + bytes.size)
        blob[0] = (head.size shr 8).toByte()
        blob[1] = (head.size and 0xff).toByte()
        head.copyInto(blob, 2)
        bytes.copyInto(blob, 2 + head.size)
        return blob
    }

    internal fun unblob(blob: ByteArray): Pair<String, ByteArray>? {
        if (blob.size < 2) return null
        val len = ((blob[0].toInt() and 0xff) shl 8) or (blob[1].toInt() and 0xff)
        if (len < 0 || 2 + len > blob.size) return null
        val name = blob.copyOfRange(2, 2 + len).toString(Charsets.UTF_8).ifBlank { "file" }
        return name to blob.copyOfRange(2 + len, blob.size)
    }

    fun frames(name: String, bytes: ByteArray): List<String>? {
        val blob = blob(name, bytes) ?: return null
        val raw = gzip(blob)
        if (raw.isEmpty()) return null
        val chunk = chunkSize(raw.size)
        val total = (raw.size + chunk - 1) / chunk
        if (total > MAX_FRAMES) return null
        val stamp = stamp(raw)
        return List(total) { i ->
            val from = i * chunk
            val to = minOf(from + chunk, raw.size)
            "F2:$stamp:${i + 1}/$total:" + String(raw.copyOfRange(from, to), Charsets.ISO_8859_1)
        }
    }

    // Небольшие коды считываются быстрее и издалека, поэтому сначала растёт число кодов, потом их размер
    private fun chunkSize(size: Int): Int {
        var chunk = 600
        if ((size + chunk - 1) / chunk > 20) chunk = ((size + 19) / 20).coerceIn(600, 1000)
        if ((size + chunk - 1) / chunk > MAX_FRAMES) chunk = ((size + MAX_FRAMES - 1) / MAX_FRAMES).coerceAtMost(1800)
        return chunk.coerceIn(200, 1800)
    }

    private fun stamp(raw: ByteArray): String {
        val crc = CRC32()
        crc.update(raw)
        return crc.value.toString(16).padStart(6, '0').takeLast(6)
    }

    private fun gzip(bytes: ByteArray): ByteArray = Gz.pack(bytes)

    class Bag {
        var total: Int = 0
            private set
        private var stamp = ""
        private val parts = linkedMapOf<Int, ByteArray>()
        val got: Int get() = synchronized(this) { parts.size }

        fun accept(text: String): Boolean = synchronized(this) {
            if (!text.startsWith("F2:")) return false
            val body = text.substring(3)
            val stampEnd = body.indexOf(':')
            if (stampEnd <= 0) return false
            val mark = body.substring(0, stampEnd)
            val rest = body.substring(stampEnd + 1)
            val slash = rest.indexOf('/')
            val colon = rest.indexOf(':')
            if (slash <= 0 || colon <= slash) return false
            val index = rest.substring(0, slash).toIntOrNull() ?: return false
            val declared = rest.substring(slash + 1, colon).toIntOrNull() ?: return false
            if (index !in 1..declared || declared > ACCEPT_FRAMES) return false
            if (mark != stamp || declared != total) {
                parts.clear()
                stamp = mark
                total = declared
            }
            val bytes = rest.substring(colon + 1).toByteArray(Charsets.ISO_8859_1)
            val prev = parts.put(index, bytes)
            prev == null || !prev.contentEquals(bytes)
        }

        fun complete(): Boolean = synchronized(this) { total > 0 && parts.size == total }

        fun file(): Pair<String, ByteArray>? = synchronized(this) {
            if (!complete()) return null
            val raw = ByteArrayOutputStream()
            for (i in 1..total) raw.write(parts.getValue(i))
            val blob = Gz.unpack(raw.toByteArray(), Gz.FILE_LIMIT) ?: return null
            return unblob(blob)
        }
    }
}
