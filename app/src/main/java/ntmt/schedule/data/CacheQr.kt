package ntmt.schedule.data

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32

object CacheQr {
    private const val EASY = 480
    private const val HARD = 1000
    private const val MAX_CHUNK = 2200
    private const val MAX_FRAMES = 64

    fun frames(json: String): List<String> {
        val raw = gzip(json.toByteArray(Charsets.UTF_8))
        if (raw.isEmpty()) return emptyList()
        val chunk = chunkSize(raw.size)
        val total = (raw.size + chunk - 1) / chunk
        val stamp = stamp(raw)
        return List(total) { i ->
            val from = i * chunk
            val to = minOf(from + chunk, raw.size)
            "N2:$stamp:${i + 1}/$total:" + String(raw.copyOfRange(from, to), Charsets.ISO_8859_1)
        }
    }

    private fun chunkSize(size: Int): Int {
        var chunk = EASY
        if ((size + chunk - 1) / chunk > 12) chunk = ((size + 11) / 12).coerceIn(EASY, HARD)
        if ((size + chunk - 1) / chunk > 28) chunk = ((size + 27) / 28).coerceIn(chunk, 1500)
        if ((size + chunk - 1) / chunk > MAX_FRAMES) chunk = ((size + MAX_FRAMES - 1) / MAX_FRAMES).coerceAtMost(MAX_CHUNK)
        return chunk.coerceIn(200, MAX_CHUNK)
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
        private var stamp: String = ""
        private val parts = linkedMapOf<Int, ByteArray>()

        val got: Int get() = synchronized(this) { parts.size }

        fun accept(text: String): Boolean = synchronized(this) {
            val part = parse(text) ?: return false
            if (part.stamp != stamp || part.total != total) {
                parts.clear()
                stamp = part.stamp
                total = part.total
            }
            val prev = parts.put(part.index, part.bytes)
            prev == null || !prev.contentEquals(part.bytes)
        }

        fun complete(): Boolean = synchronized(this) { total > 0 && parts.size == total }

        fun json(): String? = synchronized(this) {
            if (!complete()) return null
            val raw = ByteArrayOutputStream()
            for (i in 1..total) raw.write(parts.getValue(i))
            return Gz.unpack(raw.toByteArray(), Gz.SCHEDULE_LIMIT)?.toString(Charsets.UTF_8)
        }

        private fun parse(text: String): Part? {
            if (text.startsWith("N2:")) {
                val body = text.substring(3)
                val stampEnd = body.indexOf(':')
                if (stampEnd <= 0) return null
                return split(body.substring(0, stampEnd), body.substring(stampEnd + 1))
            }
            if (text.startsWith("N1:")) return split("", text.substring(3))
            return null
        }

        private fun split(stamp: String, rest: String): Part? {
            val slash = rest.indexOf('/')
            val colon = rest.indexOf(':')
            if (slash <= 0 || colon <= slash) return null
            val index = rest.substring(0, slash).toIntOrNull() ?: return null
            val declared = rest.substring(slash + 1, colon).toIntOrNull() ?: return null
            if (index !in 1..declared || declared > MAX_FRAMES) return null
            return Part(stamp, index, declared, rest.substring(colon + 1).toByteArray(Charsets.ISO_8859_1))
        }
    }

    private class Part(val stamp: String, val index: Int, val total: Int, val bytes: ByteArray)
}
