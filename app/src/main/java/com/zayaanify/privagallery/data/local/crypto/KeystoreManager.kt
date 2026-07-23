package com.zayaanify.privagallery.data.local.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android Keystore-এ AES-256 key রাখা হয়।
 * Key কখনো RAM বা disk-এ plaintext আকারে আসে না —
 * hardware-backed security chip-এ থাকে।
 */
@Singleton
class KeystoreManager @Inject constructor() {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }
    private val KEY_ALIAS = "privagallery_vault_key"

    fun getOrCreateKey(): SecretKey {
        // আগে থেকে key থাকলে সেটাই দাও
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        // না থাকলে New key বানাও
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }

    fun deleteKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }
}