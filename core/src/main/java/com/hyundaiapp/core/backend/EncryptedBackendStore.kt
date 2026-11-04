package com.hyundaiapp.core.backend

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Log
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Tokens and outbox stay encrypted in noBackupFilesDir; never copied to Android backups. */
class EncryptedBackendStore(context: Context, namespace: String = "supabase") : BackendStore {
    init { require(namespace.matches(Regex("[a-z0-9-]+"))) }
    private val file = AtomicFile(File(context.noBackupFilesDir, "$namespace-state.bin"))
    private val alias = "pleos-$namespace-state-v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }

    /**
     * 읽지 못하면(형식 오류, Keystore 키 유실·파일 손상으로 복호화 실패) null을 돌려준다 — 복구할 길이 없는 상태라
     * 예외를 올려 앱이 매번 켜지자마자 죽게 두는 것보다 빈 상태로 시작하는 편이 낫다. 다음 [write]가 새 키로 덮어쓴다.
     */
    override fun read(): String? {
        if (!file.baseFile.exists()) return null
        return try {
            val bytes = file.openRead().use { it.readBytes() }
            require(bytes.size > 13 && bytes[0] == 1.toByte()) { "Invalid backend state format" }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
            String(cipher.doFinal(bytes.copyOfRange(13, bytes.size)), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "unreadable state file ($alias), starting empty: ${e.javaClass.simpleName}")
            null
        }
    }

    override fun write(value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val bytes = byteArrayOf(1) + cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val output = file.startWrite()
        try { output.write(bytes); file.finishWrite(output) }
        catch (e: Exception) { file.failWrite(output); throw e }
    }

    private companion object { const val TAG = "EncryptedBackendStore" }
}
