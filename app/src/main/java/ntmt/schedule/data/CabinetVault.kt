package ntmt.schedule.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CabinetVault {
    private const val ALIAS = "ntmt-cabinet"
    private const val FILE = "cabinet.vault"

    fun has(context: Context): Boolean = context.getFileStreamPath(FILE).exists()

    fun peekLogin(context: Context): String? = read(context)?.first

    fun save(context: Context, login: String, password: String) {
        val payload = ByteBuffer.allocate(4 + login.toByteArray(Charsets.UTF_8).size + password.toByteArray(Charsets.UTF_8).size)
        val loginBytes = login.toByteArray(Charsets.UTF_8)
        val passBytes = password.toByteArray(Charsets.UTF_8)
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
    }

    fun read(context: Context): Pair<String, String>? {
        val file = context.getFileStreamPath(FILE)
        if (!file.exists()) return null
        return try {
            val raw = context.openFileInput(FILE).use { it.readBytes() }
            if (raw.size < 13) return null
            val iv = raw.copyOfRange(0, 12)
            val data = raw.copyOfRange(12, raw.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            val plain = cipher.doFinal(data)
            val buf = ByteBuffer.wrap(plain)
            val len = buf.int
            if (len < 0 || len > buf.remaining()) return null
            val login = ByteArray(len)
            buf.get(login)
            val pass = ByteArray(buf.remaining())
            buf.get(pass)
            String(login, Charsets.UTF_8) to String(pass, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun clear(context: Context) {
        context.deleteFile(FILE)
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        gen.init(spec)
        return gen.generateKey()
    }
}
