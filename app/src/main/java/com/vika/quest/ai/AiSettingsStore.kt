package com.vika.quest.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class StoredAiSettings(val baseUrl: String, val model: String, val hasApiKey: Boolean)

class AiSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun readPublic() = StoredAiSettings(
        baseUrl = prefs.getString(BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL,
        model = prefs.getString(MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL,
        hasApiKey = prefs.contains(KEY_CIPHERTEXT) && prefs.contains(KEY_IV),
    )

    fun readConnectionSettings(): AiConnectionSettings? {
        val public = readPublic()
        val key = decryptKey() ?: return null
        return AiConnectionSettings(public.baseUrl, public.model, key)
    }

    fun save(baseUrl: String, model: String, apiKey: String?) {
        val normalized = baseUrl.trim().removeSuffix("/")
        require(normalized.startsWith("https://")) { "API 地址必须使用 HTTPS" }
        require(model.isNotBlank()) { "模型名称不能为空" }
        val editor = prefs.edit().putString(BASE_URL, normalized).putString(MODEL, model.trim())
        apiKey?.trim()?.takeIf(String::isNotEmpty)?.let { key ->
            val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, getOrCreateKey()) }
            editor.putString(KEY_CIPHERTEXT, Base64.encodeToString(cipher.doFinal(key.toByteArray()), Base64.NO_WRAP))
            editor.putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
        }
        editor.apply()
    }

    private fun decryptKey(): String? = runCatching {
        val encrypted = Base64.decode(prefs.getString(KEY_CIPHERTEXT, null), Base64.NO_WRAP)
        val iv = Base64.decode(prefs.getString(KEY_IV, null), Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv)) }
        String(cipher.doFinal(encrypted))
    }.getOrNull()

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.deepseek.com"
        const val DEFAULT_MODEL = "deepseek-flash"
        private const val PREFS = "quest_ai_secure"
        private const val BASE_URL = "base_url"
        private const val MODEL = "model"
        private const val KEY_CIPHERTEXT = "api_key_ciphertext"
        private const val KEY_IV = "api_key_iv"
        private const val KEY_ALIAS = "quest_deepseek_api_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
