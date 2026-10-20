package ntmt.schedule.data

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Логин и пароль кабинета. Ключ AES-256-GCM живёт в Android Keystore и работает
 * только [AUTH_SECONDS] секунд после подтверждения кодом, отпечатком или лицом.
 * Логин хранится отдельно открытым текстом, чтобы показать его до подтверждения.
 */
object CabinetVault {
    private const val ALIAS = "ntmt-cabinet-auth"
    private const val FILE = "cabinet2.vault"
    private const val LOGIN_FILE = "cabinet.login"
    private const val AUTH_SECONDS = 60

    // Версии до 2.1 Beta (115): ключ без привязки к подтверждению
    private const val LEGACY_ALIAS = "ntmt-cabinet"
    private const val LEGACY_FILE = "cabinet.vault"

    fun has(context: Context): Boolean =
        context.getFileStreamPath(FILE).exists() || context.getFileStreamPath(LEGACY_FILE).exists()

    fun peekLogin(context: Context): String? {
        val file = context.getFileStreamPath(LOGIN_FILE)
        if (file.exists()) {
            return runCatching { context.openFileInput(LOGIN_FILE).use { it.readBytes() }.toString(Charsets.UTF_8) }
                .getOrNull()?.takeIf { it.isNotBlank() }
        }
        if (context.getFileStreamPath(LEGACY_FILE).exists()) {
            return runCatching { decrypt(context, LEGACY_FILE, legacyKey() ?: return null) }.getOrNull()?.first
        }
        return null
    }

    /** Вызывать сразу после подтверждения входа. false — ключ не открыт или недоступен. */
    fun save(context: Context, login: String, password: String): Boolean {
        return try {
            val loginBytes = login.toByteArray(Charsets.UTF_8)
            val passBytes = password.toByteArray(Charsets.UTF_8)
            val payload = ByteBuffer.allocate(4 + loginBytes.size + passBytes.size)
            payload.putInt(loginBytes.size)
            payload.put(loginBytes)
            payload.put(passBytes)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val encrypted = cipher.doFinal(payload.array())
            val out = ByteBuffer.allocate(12 + encrypted.size)
            out.put(cipher.iv.copyOf(12))
            out.put(encrypted)
            context.openFileOutput(FILE, Context.MODE_PRIVATE).use { it.write(out.array()) }
            context.openFileOutput(LOGIN_FILE, Context.MODE_PRIVATE).use { it.write(loginBytes) }
            dropLegacy(context)
            true
        } catch (e: KeyPermanentlyInvalidatedException) {
            clear(context)
            false
        } catch (_: Exception) {
            false
        }
    }

    /** Вызывать сразу после подтверждения входа. */
    fun read(context: Context): Pair<String, String>? {
        if (context.getFileStreamPath(FILE).exists()) {
            return try {
                decrypt(context, FILE, key())
            } catch (e: KeyPermanentlyInvalidatedException) {
                // Сменили блокировку экрана или добавили отпечаток
                clear(context)
                null
            } catch (_: Exception) {
                null
            }
        }
        if (context.getFileStreamPath(LEGACY_FILE).exists()) {
            val old = runCatching { decrypt(context, LEGACY_FILE, legacyKey() ?: return null) }.getOrNull()
            if (old == null) {
                dropLegacy(context)
                return null
            }
            // Перешифровать новым ключом, пока подтверждение ещё действует
            save(context, old.first, old.second)
            return old
        }
        return null
    }

    fun clear(context: Context) {
        // Без быстрого входа фоновая копия кабинета тоже не должна работать
        ntmt.schedule.backup.BackupVault.clear(context)
        context.deleteFile(FILE)
        context.deleteFile(LOGIN_FILE)
        dropLegacy(context)
        runCatching { keyStore().deleteEntry(ALIAS) }
    }

    private fun dropLegacy(context: Context) {
        context.deleteFile(LEGACY_FILE)
        runCatching { keyStore().deleteEntry(LEGACY_ALIAS) }
    }

    private fun decrypt(context: Context, name: String, key: SecretKey): Pair<String, String>? {
        val raw = context.openFileInput(name).use { it.readBytes() }
        if (raw.size < 13) return null
        val iv = raw.copyOfRange(0, 12)
        val data = raw.copyOfRange(12, raw.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        val plain = cipher.doFinal(data)
        val buf = ByteBuffer.wrap(plain)
        val len = buf.int
        if (len < 0 || len > buf.remaining()) return null
        val login = ByteArray(len)
        buf.get(login)
        val pass = ByteArray(buf.remaining())
        buf.get(pass)
        return String(login, Charsets.UTF_8) to String(pass, Charsets.UTF_8)
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun legacyKey(): SecretKey? = keyStore().getKey(LEGACY_ALIAS, null) as? SecretKey

    private fun key(): SecretKey {
        (keyStore().getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
        if (Build.VERSION.SDK_INT >= 30) {
            builder.setUserAuthenticationParameters(
                AUTH_SECONDS,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(AUTH_SECONDS)
        }
        gen.init(builder.build())
        return gen.generateKey()
    }
}
