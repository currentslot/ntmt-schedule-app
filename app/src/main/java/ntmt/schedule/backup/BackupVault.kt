package ntmt.schedule.backup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Вход кабинета только для фоновой резервной копии.
 *
 * Основной быстрый вход ([ntmt.schedule.data.CabinetVault]) открывается только отпечатком или кодом,
 * поэтому фоном им не воспользоваться. Сюда пароль копируется один раз, когда пользователь
 * подтверждает выбор папок кабинета отпечатком. Ключ AES-256-GCM в Android Keystore без
 * подтверждения, но читает его только [BackupWorker]: экран «Кабинет» по-прежнему спрашивает отпечаток.
 * Стирается при выключении копий, отключении быстрого входа или смене блокировки.
 */
object BackupVault {
    private const val ALIAS = "ntmt-backup-login"
    private const val FILE = "backup.vault"

    fun has(context: Context): Boolean = context.getFileStreamPath(FILE).exists()

    fun save(context: Context, login: String, password: String): Boolean = try {
        val loginBytes = login.toByteArray(Charsets.UTF_8)
        val passBytes = password.toByteArray(Charsets.UTF_8)
        val payload = ByteBuffer.allocate(4 + loginBytes.size + passBytes.size)
            .putInt(loginBytes.size).put(loginBytes).put(passBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(payload.array())
        context.openFileOutput(FILE, Context.MODE_PRIVATE).use { it.write(cipher.iv.copyOf(12) + encrypted) }
        true
    } catch (_: Exception) {
        false
    }

    fun read(context: Context): Pair<String, String>? = try {
        val raw = context.openFileInput(FILE).use { it.readBytes() }
        if (raw.size < 13) {
            null
        } else {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw.copyOfRange(0, 12)))
            val buf = ByteBuffer.wrap(cipher.doFinal(raw.copyOfRange(12, raw.size)))
            val len = buf.int
            if (len < 0 || len > buf.remaining()) {
                null
            } else {
                val login = ByteArray(len).also { buf.get(it) }
                val pass = ByteArray(buf.remaining()).also { buf.get(it) }
                String(login, Charsets.UTF_8) to String(pass, Charsets.UTF_8)
            }
        }
    } catch (_: Exception) {
        null
    }

    fun clear(context: Context) {
        context.deleteFile(FILE)
        runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(ALIAS) }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }
}
