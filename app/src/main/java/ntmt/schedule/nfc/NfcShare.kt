package ntmt.schedule.nfc

import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ntmt.schedule.data.FileQr
import ntmt.schedule.data.Gz
import java.util.zip.CRC32

/**
 * Передача расписания и файлов между двумя телефонами по NFC.
 *
 * Android Beam больше нет, поэтому отдающий телефон притворяется картой (HCE, [NfcHostService]),
 * а принимающий читает её в режиме ридера ([Pull]). Данные те же, что в QR-кодах: gzip расписания
 * или gzip блока «имя + файл». Если телефоны разошлись, повторное касание продолжает с того же места.
 */
object NfcShare {
    const val KIND_SCHEDULE: Byte = 'S'.code.toByte()
    const val KIND_FILE: Byte = 'F'.code.toByte()

    /** Больше этого файл по NFC идёт слишком долго (≈10 КБ/с) */
    const val MAX_FILE = 1024 * 1024
    private const val MAX_PAYLOAD = 4 * 1024 * 1024

    internal val AID = byteArrayOf(0xF0.toByte(), 0x4E, 0x54, 0x4D, 0x54, 0x01)
    internal const val INS_READ: Byte = 0xB0.toByte()
    internal const val CLA_PROPRIETARY: Byte = 0x80.toByte()
    /** Короткий APDU: до 256 байт за обмен */
    internal const val CHUNK = 256

    /** Расширенный APDU: если оба телефона его тянут, обмен в ~4 раза крупнее */
    internal const val BIG_CHUNK = 1000

    /** Флаг в ответе на выбор приложения: отдающий телефон понимает расширенные APDU */
    internal const val CAN_BIG: Byte = 0x01
    internal val OK = byteArrayOf(0x90.toByte(), 0x00)
    internal val NOTHING = byteArrayOf(0x6A, 0x82.toByte())
    internal val BAD_OFFSET = byteArrayOf(0x6B, 0x00)
    internal val UNKNOWN = byteArrayOf(0x6D, 0x00)

    internal class Offer(val kind: Byte, val raw: ByteArray, val crc: Int)

    @Volatile
    internal var offer: Offer? = null
        private set

    private val sentFlow = MutableStateFlow(0f)

    /** Доля, которую забрал другой телефон, для экрана отправки */
    val sent: StateFlow<Float> = sentFlow

    fun canSend(ctx: Context): Boolean =
        ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION) && NfcAdapter.getDefaultAdapter(ctx) != null

    fun canReceive(ctx: Context): Boolean = NfcAdapter.getDefaultAdapter(ctx) != null

    fun enabled(ctx: Context): Boolean = NfcAdapter.getDefaultAdapter(ctx)?.isEnabled == true

    fun offerSchedule(json: String): Boolean = put(KIND_SCHEDULE, Gz.pack(json.toByteArray(Charsets.UTF_8)))

    fun offerFile(name: String, bytes: ByteArray): Boolean {
        if (bytes.size > MAX_FILE) return false
        val blob = FileQr.blob(name, bytes) ?: return false
        return put(KIND_FILE, Gz.pack(blob))
    }

    fun clear() {
        offer = null
        sentFlow.value = 0f
    }

    private fun put(kind: Byte, raw: ByteArray): Boolean {
        if (raw.isEmpty() || raw.size > MAX_PAYLOAD) return false
        sentFlow.value = 0f
        offer = Offer(kind, raw, crc(raw))
        return true
    }

    internal fun reportSent(end: Int, total: Int) {
        if (total > 0) sentFlow.value = (end.toFloat() / total).coerceIn(0f, 1f)
    }

    internal fun crc(raw: ByteArray): Int {
        val c = CRC32()
        c.update(raw)
        return c.value.toInt()
    }

    internal fun int(v: Int) = byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())

    internal fun readInt(b: ByteArray, at: Int): Int =
        ((b[at].toInt() and 0xff) shl 24) or ((b[at + 1].toInt() and 0xff) shl 16) or
            ((b[at + 2].toInt() and 0xff) shl 8) or (b[at + 3].toInt() and 0xff)

    /**
     * Сборка данных на принимающем телефоне. Живёт, пока открыт экран приёма,
     * поэтому после обрыва связи повторное касание докачивает остаток.
     */
    class Pull(private val kind: Byte) {
        private var crc = 0
        private var buf: ByteArray? = null
        private var got = 0

        // Если крупные блоки не прошли, до конца приёма идут короткие
        private var small = false

        val fraction: Float
            get() = synchronized(this) { buf?.let { if (it.isEmpty()) 0f else got.toFloat() / it.size } ?: 0f }

        /**
         * Читает с поднесённого телефона. Возвращает распакованные данные, когда всё пришло,
         * или null, если связь прервалась (можно приложить ещё раз). Ошибки — [IllegalStateException].
         */
        fun read(dep: IsoDep, onProgress: (Float) -> Unit): ByteArray? = synchronized(this) {
            try {
                dep.connect()
                dep.timeout = 4000
                val head = dep.transceive(byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, AID.size.toByte()) + AID + byteArrayOf(0x00))
                if (head.size < 2 || !ends(head, OK)) {
                    if (ends(head, NOTHING)) error("На другом телефоне откройте «Поделиться» и выберите NFC")
                    error("Это не телефон с Расписанием НТМТ")
                }
                if (head.size < 2 + 11 || head[0] != 'N'.code.toByte() || head[1] != 'T'.code.toByte()) error("Другая версия приложения")
                val theirKind = head[2]
                if (theirKind != kind) {
                    error(if (theirKind == KIND_FILE) "Другой телефон передаёт файл, а не расписание" else "Другой телефон передаёт расписание, а не файл")
                }
                val size = readInt(head, 3)
                val theirCrc = readInt(head, 7)
                if (size <= 0 || size > MAX_PAYLOAD) error("Слишком большие данные")
                val current = buf
                if (current == null || current.size != size || crc != theirCrc) {
                    buf = ByteArray(size)
                    crc = theirCrc
                    got = 0
                }
                val target = buf!!
                val theyBig = head.size >= 2 + 12 && (head[11].toInt() and CAN_BIG.toInt()) != 0
                val big = theyBig && runCatching { dep.isExtendedLengthApduSupported && dep.maxTransceiveLength >= 32 }.getOrDefault(false)
                while (got < size) {
                    val useBig = big && !small
                    val want = minOf(if (useBig) BIG_CHUNK else CHUNK, size - got)
                    val cmd = if (useBig) {
                        byteArrayOf(CLA_PROPRIETARY, INS_READ, 0x00, 0x00, 0x00, 0x00, 0x04) + int(got) +
                            byteArrayOf((want ushr 8).toByte(), want.toByte())
                    } else {
                        byteArrayOf(CLA_PROPRIETARY, INS_READ, 0x00, 0x00, 0x04) + int(got) + byteArrayOf(want.toByte())
                    }
                    val res = try {
                        dep.transceive(cmd)
                    } catch (e: Exception) {
                        if (useBig) small = true
                        throw e
                    }
                    if (useBig && (!ends(res, OK) || res.size < 3) && !ends(res, NOTHING)) {
                        small = true
                        continue
                    }
                    if (!ends(res, OK) || res.size < 3) {
                        if (ends(res, NOTHING)) error("На другом телефоне закрыли экран передачи")
                        error("Передача прервалась, приложите ещё раз")
                    }
                    val n = res.size - 2
                    if (got + n > size) error("Данные повреждены")
                    System.arraycopy(res, 0, target, got, n)
                    got += n
                    onProgress(got.toFloat() / size)
                }
                if (crc(target) != crc) {
                    buf = null
                    got = 0
                    error("Данные повреждены, приложите ещё раз")
                }
                val limit = if (kind == KIND_FILE) Gz.FILE_LIMIT else Gz.SCHEDULE_LIMIT
                return Gz.unpack(target, limit) ?: error("Не удалось распаковать данные")
            } catch (e: IllegalStateException) {
                throw e
            } catch (_: Exception) {
                // TagLostException, IOException: телефоны разошлись, данные сохраняются до следующего касания
                return null
            } finally {
                runCatching { dep.close() }
            }
        }

        private fun ends(res: ByteArray, sw: ByteArray) = res.size >= 2 && res[res.size - 2] == sw[0] && res[res.size - 1] == sw[1]
    }
}
