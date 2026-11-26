package com.example.medora.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// The user's own AI keys. Saved encrypted (AES-GCM, key held in Android Keystore)
// in this app's private storage on the phone only. Never put keys in the repository.
// Old plain-text keys from earlier builds are encrypted on first start.
class AiSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)

    init {
        migrate("gemini_key")
        migrate("openrouter_key")
    }

    var geminiKey: String
        get() = readSecret("gemini_key")
        set(value) = writeSecret("gemini_key", value.trim())

    var geminiModel: String
        get() = prefs.getString("gemini_model", null)?.ifBlank { null } ?: DEFAULT_GEMINI_MODEL
        set(value) = prefs.edit().putString("gemini_model", value.trim()).apply()

    var openRouterKey: String
        get() = readSecret("openrouter_key")
        set(value) = writeSecret("openrouter_key", value.trim())

    var openRouterModel: String
        get() = prefs.getString("openrouter_model", null)?.ifBlank { null } ?: DEFAULT_OPENROUTER_MODEL
        set(value) = prefs.edit().putString("openrouter_model", value.trim()).apply()

    val hasAnyKey: Boolean get() = geminiKey.isNotBlank() || openRouterKey.isNotBlank()

    private fun readSecret(name: String): String {
        val stored = prefs.getString("${name}_enc", null) ?: return ""
        return decrypt(stored) ?: ""
    }

    private fun writeSecret(name: String, value: String) {
        if (value.isBlank()) {
            prefs.edit().remove("${name}_enc").remove(name).apply()
            return
        }
        // If encryption fails nothing is saved: a key is never stored in plain text.
        val enc = encrypt(value) ?: return
        prefs.edit().putString("${name}_enc", enc).remove(name).apply()
    }

    private fun migrate(name: String) {
        val old = prefs.getString(name, null) ?: return
        if (old.isBlank()) {
            prefs.edit().remove(name).apply()
            return
        }
        val enc = encrypt(old) ?: return
        prefs.edit().putString("${name}_enc", enc).remove(name).apply()
    }

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun encrypt(plain: String): String? = try {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val data = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        Base64.encodeToString(iv + data, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }

    private fun decrypt(stored: String): String? = try {
        val all = Base64.decode(stored, Base64.NO_WRAP)
        val iv = all.copyOfRange(0, IV_SIZE)
        val data = all.copyOfRange(IV_SIZE, all.size)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
        String(cipher.doFinal(data), Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }

    companion object {
        const val DEFAULT_GEMINI_MODEL = "gemini-flash-latest"
        const val DEFAULT_OPENROUTER_MODEL = "openrouter/free"
        private const val KEY_ALIAS = "medora_ai_keys"
        private const val TRANSFORM = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128
    }
}
