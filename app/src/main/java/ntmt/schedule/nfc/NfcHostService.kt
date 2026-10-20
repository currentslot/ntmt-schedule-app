package ntmt.schedule.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/**
 * Отдающая сторона NFC: телефон отвечает как карта, пока открыт экран «Поделиться → NFC».
 * Без открытого экрана на любые запросы отвечает «нет данных».
 */
class NfcHostService : HostApduService() {
    override fun processCommandApdu(apdu: ByteArray, extras: Bundle?): ByteArray {
        if (apdu.size < 4) return NfcShare.UNKNOWN
        val cla = apdu[0]
        val ins = apdu[1]
        if (cla == 0x00.toByte() && ins == 0xA4.toByte()) return select(apdu)
        if (cla == NfcShare.CLA_PROPRIETARY && ins == NfcShare.INS_READ) return read(apdu)
        return NfcShare.UNKNOWN
    }

    private fun select(apdu: ByteArray): ByteArray {
        val offer = NfcShare.offer ?: return NfcShare.NOTHING
        if (apdu.size < 5 + NfcShare.AID.size) return NfcShare.NOTHING
        val aid = apdu.copyOfRange(5, 5 + NfcShare.AID.size)
        if (!aid.contentEquals(NfcShare.AID)) return NfcShare.NOTHING
        val head = byteArrayOf('N'.code.toByte(), 'T'.code.toByte(), offer.kind) +
            NfcShare.int(offer.raw.size) + NfcShare.int(offer.crc) + byteArrayOf(NfcShare.CAN_BIG)
        return head + NfcShare.OK
    }

    private fun read(apdu: ByteArray): ByteArray {
        val offer = NfcShare.offer ?: return NfcShare.NOTHING
        if (apdu.size < 9) return NfcShare.BAD_OFFSET
        // Расширенный APDU: Lc из трёх байт (00 00 04) и Le из двух
        val extended = apdu[4] == 0x00.toByte() && apdu.size >= 11
        val from = NfcShare.readInt(apdu, if (extended) 7 else 5)
        val want = if (extended) {
            if (apdu.size >= 13) (((apdu[11].toInt() and 0xff) shl 8) or (apdu[12].toInt() and 0xff)).let { if (it == 0) 65536 else it } else NfcShare.BIG_CHUNK
        } else {
            if (apdu.size >= 10) (apdu[9].toInt() and 0xff).let { if (it == 0) 256 else it } else NfcShare.CHUNK
        }
        if (from < 0 || from >= offer.raw.size) return NfcShare.BAD_OFFSET
        val end = minOf(offer.raw.size, from + minOf(want, if (extended) NfcShare.BIG_CHUNK else NfcShare.CHUNK))
        NfcShare.reportSent(end, offer.raw.size)
        return offer.raw.copyOfRange(from, end) + NfcShare.OK
    }

    override fun onDeactivated(reason: Int) = Unit
}
