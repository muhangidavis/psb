package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.PsbCrypto
import com.example.repository.VaultRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches PSB`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PSB", appName)
    }

    @Test
    fun `crypto encrypt and decrypt produces original bytes`() {
        val keyBytes = PsbCrypto.generateRandomBytes(32)
        val key = PsbCrypto.createSecretKey(keyBytes)
        val originalText = "TopSecretConfidentialInformation_12345"
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)

        val encrypted = PsbCrypto.encryptAesGcm(key, originalBytes)
        val decrypted = PsbCrypto.decryptAesGcm(key, encrypted.iv, encrypted.ciphertext)

        assertEquals(originalText, String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun `vault creation and login workflow succeeds`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = VaultRepository(context)

        val animal = "🐯"
        val password = "StrongPassword123"
        val pin = "7890"

        // 1. Create vault
        val masterKey = repository.createVault(animal, password, pin)
        assertNotNull(masterKey)

        // 2. Verify password
        val kekResult = repository.verifyPasswordAndDeriveKek(animal, password)
        assertTrue(kekResult.isSuccess)
        val kek = kekResult.getOrThrow()

        // 3. Verify PIN and unwrap master key
        val unwrapResult = repository.verifyPinAndUnwrapKey(animal, kek, pin)
        assertTrue(unwrapResult.isSuccess)
        val unwrappedKey = unwrapResult.getOrThrow()

        // 4. Save and decrypt note
        val note = repository.saveEncryptedNote(
            owner = animal,
            parentId = "root",
            title = "My Secret Passwords",
            content = "Bank: 998811, Safe: 4422",
            dataKey = unwrappedKey
        )
        assertNotNull(note)

        val decryptedNote = repository.decryptNode(note, unwrappedKey)
        assertEquals("Bank: 998811, Safe: 4422", String(decryptedNote, Charsets.UTF_8))
    }
}
