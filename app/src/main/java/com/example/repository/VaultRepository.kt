package com.example.repository

import android.content.Context
import com.example.crypto.PsbCrypto
import com.example.data.PsbDatabase
import com.example.data.VaultAccountEntity
import com.example.data.VaultNodeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.crypto.SecretKey

class VaultRepository(private val context: Context) {

    private val db = PsbDatabase.getInstance(context)
    private val accountDao = db.accountDao()
    private val nodeDao = db.nodeDao()

    val accountsFlow: Flow<List<VaultAccountEntity>> = accountDao.getAllAccounts()

    suspend fun getAccount(animal: String): VaultAccountEntity? = withContext(Dispatchers.IO) {
        accountDao.getAccount(animal)
    }

    suspend fun createVault(
        animal: String,
        password: String,
        pin: String
    ): SecretKey = withContext(Dispatchers.IO) {
        val salt = PsbCrypto.generateRandomBytes(16)
        val kekBits = PsbCrypto.deriveKekBits(password, salt)
        val kek = PsbCrypto.createSecretKey(kekBits)
        val verifier = PsbCrypto.sha256(kekBits)

        val masterKeyBytes = PsbCrypto.generateRandomBytes(32)
        val wrapped = PsbCrypto.encryptAesGcm(kek, masterKeyBytes)

        val pinSalt = PsbCrypto.generateRandomBytes(16)
        val pinVerifier = PsbCrypto.derivePinVerifier(pin, pinSalt)

        val account = VaultAccountEntity(
            animal = animal,
            saltB64 = PsbCrypto.toBase64(salt),
            verifierB64 = PsbCrypto.toBase64(verifier),
            wrapIvB64 = PsbCrypto.toBase64(wrapped.iv),
            wrappedKeyB64 = PsbCrypto.toBase64(wrapped.ciphertext),
            pinSaltB64 = PsbCrypto.toBase64(pinSalt),
            pinVerifierB64 = pinVerifier
        )
        accountDao.insertAccount(account)

        // Seed default compartments/folders
        val defaultFolders = listOf("Information", "Movies", "Music", "Records", "Photos")
        val now = System.currentTimeMillis()
        val defaultNodes = defaultFolders.map { folderName ->
            VaultNodeEntity(
                id = UUID.randomUUID().toString(),
                ownerAnimal = animal,
                parentId = "root",
                type = "folder",
                name = folderName,
                createdAt = now,
                updatedAt = now
            )
        }
        nodeDao.insertNodes(defaultNodes)

        PsbCrypto.createSecretKey(masterKeyBytes)
    }

    suspend fun verifyPasswordAndDeriveKek(
        animal: String,
        password: String
    ): Result<SecretKey> = withContext(Dispatchers.IO) {
        val account = accountDao.getAccount(animal)
            ?: return@withContext Result.failure(Exception("Vault not found for this guardian."))

        val salt = PsbCrypto.fromBase64(account.saltB64)
        val kekBits = PsbCrypto.deriveKekBits(password, salt)
        val verifier = PsbCrypto.sha256(kekBits)

        if (PsbCrypto.toBase64(verifier) != account.verifierB64) {
            return@withContext Result.failure(Exception("Incorrect password. The box stays chained."))
        }

        Result.success(PsbCrypto.createSecretKey(kekBits))
    }

    suspend fun verifyPinAndUnwrapKey(
        animal: String,
        kek: SecretKey,
        pin: String
    ): Result<SecretKey> = withContext(Dispatchers.IO) {
        val account = accountDao.getAccount(animal)
            ?: return@withContext Result.failure(Exception("Vault not found."))

        val pinSalt = PsbCrypto.fromBase64(account.pinSaltB64)
        val computedPinVerifier = PsbCrypto.derivePinVerifier(pin, pinSalt)

        if (computedPinVerifier != account.pinVerifierB64) {
            return@withContext Result.failure(Exception("Incorrect PIN. The padlock stays shut."))
        }

        try {
            val wrapIv = PsbCrypto.fromBase64(account.wrapIvB64)
            val wrappedData = PsbCrypto.fromBase64(account.wrappedKeyB64)
            val masterKeyBytes = PsbCrypto.decryptAesGcm(kek, wrapIv, wrappedData)
            Result.success(PsbCrypto.createSecretKey(masterKeyBytes))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to unwrap master vault key: ${e.message}"))
        }
    }

    suspend fun changePasswordAndPin(
        animal: String,
        oldKek: SecretKey,
        dataKey: SecretKey,
        newPassword: String,
        newPin: String
    ): Result<SecretKey> = withContext(Dispatchers.IO) {
        try {
            val salt = PsbCrypto.generateRandomBytes(16)
            val newKekBits = PsbCrypto.deriveKekBits(newPassword, salt)
            val newKek = PsbCrypto.createSecretKey(newKekBits)
            val verifier = PsbCrypto.sha256(newKekBits)

            val wrapped = PsbCrypto.encryptAesGcm(newKek, dataKey.encoded)

            val pinSalt = PsbCrypto.generateRandomBytes(16)
            val pinVerifier = PsbCrypto.derivePinVerifier(newPin, pinSalt)

            val account = VaultAccountEntity(
                animal = animal,
                saltB64 = PsbCrypto.toBase64(salt),
                verifierB64 = PsbCrypto.toBase64(verifier),
                wrapIvB64 = PsbCrypto.toBase64(wrapped.iv),
                wrappedKeyB64 = PsbCrypto.toBase64(wrapped.ciphertext),
                pinSaltB64 = PsbCrypto.toBase64(pinSalt),
                pinVerifierB64 = pinVerifier
            )
            accountDao.insertAccount(account)
            Result.success(newKek)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getNodesInFolder(owner: String, parentId: String): Flow<List<VaultNodeEntity>> {
        return nodeDao.getNodesInFolder(owner, parentId)
    }

    suspend fun getNode(id: String): VaultNodeEntity? = withContext(Dispatchers.IO) {
        nodeDao.getNodeById(id)
    }

    private fun getVaultDirectory(owner: String): File {
        val dir = File(context.filesDir, "vaults/$owner")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun createFolder(
        owner: String,
        parentId: String,
        name: String
    ): VaultNodeEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val node = VaultNodeEntity(
            id = UUID.randomUUID().toString(),
            ownerAnimal = owner,
            parentId = parentId,
            type = "folder",
            name = name,
            createdAt = now,
            updatedAt = now
        )
        nodeDao.insertNode(node)
        node
    }

    suspend fun saveEncryptedFile(
        owner: String,
        parentId: String,
        name: String,
        mime: String,
        bytes: ByteArray,
        dataKey: SecretKey,
        type: String = "file"
    ): VaultNodeEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val encrypted = PsbCrypto.encryptAesGcm(dataKey, bytes)

        val fileName = "enc_${id}.bin"
        val vaultDir = getVaultDirectory(owner)
        val targetFile = File(vaultDir, fileName)
        targetFile.writeBytes(encrypted.ciphertext)

        val now = System.currentTimeMillis()
        val node = VaultNodeEntity(
            id = id,
            ownerAnimal = owner,
            parentId = parentId,
            type = type,
            name = name,
            mime = mime,
            size = bytes.size.toLong(),
            ivB64 = PsbCrypto.toBase64(encrypted.iv),
            encryptedFileName = fileName,
            createdAt = now,
            updatedAt = now
        )
        nodeDao.insertNode(node)
        node
    }

    suspend fun saveEncryptedNote(
        owner: String,
        parentId: String,
        title: String,
        content: String,
        dataKey: SecretKey
    ): VaultNodeEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val contentBytes = content.toByteArray(Charsets.UTF_8)
        val encrypted = PsbCrypto.encryptAesGcm(dataKey, contentBytes)

        val now = System.currentTimeMillis()
        val node = VaultNodeEntity(
            id = id,
            ownerAnimal = owner,
            parentId = parentId,
            type = "note",
            name = title,
            mime = "text/plain",
            size = contentBytes.size.toLong(),
            ivB64 = PsbCrypto.toBase64(encrypted.iv),
            noteContentEncrypted = PsbCrypto.toBase64(encrypted.ciphertext),
            createdAt = now,
            updatedAt = now
        )
        nodeDao.insertNode(node)
        node
    }

    suspend fun updateEncryptedNote(
        node: VaultNodeEntity,
        newTitle: String,
        newContent: String,
        dataKey: SecretKey
    ): VaultNodeEntity = withContext(Dispatchers.IO) {
        val contentBytes = newContent.toByteArray(Charsets.UTF_8)
        val encrypted = PsbCrypto.encryptAesGcm(dataKey, contentBytes)

        val updated = node.copy(
            name = newTitle,
            size = contentBytes.size.toLong(),
            ivB64 = PsbCrypto.toBase64(encrypted.iv),
            noteContentEncrypted = PsbCrypto.toBase64(encrypted.ciphertext),
            updatedAt = System.currentTimeMillis()
        )
        nodeDao.updateNode(updated)
        updated
    }

    suspend fun saveLink(
        owner: String,
        parentId: String,
        name: String,
        url: String
    ): VaultNodeEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val node = VaultNodeEntity(
            id = UUID.randomUUID().toString(),
            ownerAnimal = owner,
            parentId = parentId,
            type = "link",
            name = name,
            mime = "text/x-uri",
            size = 0L,
            url = url,
            createdAt = now,
            updatedAt = now
        )
        nodeDao.insertNode(node)
        node
    }

    suspend fun decryptNode(node: VaultNodeEntity, dataKey: SecretKey): ByteArray = withContext(Dispatchers.IO) {
        val ivB64 = node.ivB64 ?: throw IllegalStateException("Node missing IV")
        val iv = PsbCrypto.fromBase64(ivB64)

        if (node.type == "note") {
            val contentB64 = node.noteContentEncrypted
                ?: throw IllegalStateException("Note has no encrypted text")
            val ciphertext = PsbCrypto.fromBase64(contentB64)
            return@withContext PsbCrypto.decryptAesGcm(dataKey, iv, ciphertext)
        }

        val fileName = node.encryptedFileName
            ?: throw IllegalStateException("File node missing encrypted file name")
        val vaultDir = getVaultDirectory(node.ownerAnimal)
        val file = File(vaultDir, fileName)
        if (!file.exists()) {
            throw IllegalStateException("Encrypted file not found on disk")
        }
        val ciphertext = file.readBytes()
        PsbCrypto.decryptAesGcm(dataKey, iv, ciphertext)
    }

    suspend fun renameNode(id: String, newName: String) = withContext(Dispatchers.IO) {
        val node = nodeDao.getNodeById(id) ?: return@withContext
        nodeDao.updateNode(node.copy(name = newName, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNodeAndChildren(nodeId: String) = withContext(Dispatchers.IO) {
        val node = nodeDao.getNodeById(nodeId) ?: return@withContext
        deleteRecursive(node)
    }

    private suspend fun deleteRecursive(node: VaultNodeEntity) {
        if (node.type == "folder") {
            val children = nodeDao.getDirectChildren(node.ownerAnimal, node.id)
            for (child in children) {
                deleteRecursive(child)
            }
        } else if (node.encryptedFileName != null) {
            val vaultDir = getVaultDirectory(node.ownerAnimal)
            val file = File(vaultDir, node.encryptedFileName)
            if (file.exists()) file.delete()
        }
        nodeDao.deleteNode(node)
    }

    suspend fun copyNodeTree(nodeId: String, newParentId: String): VaultNodeEntity? = withContext(Dispatchers.IO) {
        val node = nodeDao.getNodeById(nodeId) ?: return@withContext null
        copyRecursive(node, newParentId)
    }

    private suspend fun copyRecursive(original: VaultNodeEntity, newParentId: String): VaultNodeEntity {
        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        var newFileName: String? = null
        if (original.encryptedFileName != null) {
            val vaultDir = getVaultDirectory(original.ownerAnimal)
            val srcFile = File(vaultDir, original.encryptedFileName)
            if (srcFile.exists()) {
                newFileName = "enc_${newId}.bin"
                val destFile = File(vaultDir, newFileName)
                srcFile.copyTo(destFile, overwrite = true)
            }
        }

        val clone = original.copy(
            id = newId,
            parentId = newParentId,
            encryptedFileName = newFileName,
            createdAt = now,
            updatedAt = now
        )
        nodeDao.insertNode(clone)

        if (original.type == "folder") {
            val children = nodeDao.getDirectChildren(original.ownerAnimal, original.id)
            for (child in children) {
                copyRecursive(child, newId)
            }
        }
        return clone
    }

    suspend fun moveNode(nodeId: String, newParentId: String) = withContext(Dispatchers.IO) {
        val node = nodeDao.getNodeById(nodeId) ?: return@withContext
        if (node.id == newParentId) return@withContext
        if (node.type == "folder" && isDescendant(newParentId, node.id)) {
            throw IllegalArgumentException("Cannot move a folder inside itself.")
        }
        nodeDao.updateNode(node.copy(parentId = newParentId, updatedAt = System.currentTimeMillis()))
    }

    private suspend fun isDescendant(candidateId: String, ancestorId: String): Boolean {
        var current: String? = candidateId
        while (current != null && current != "root") {
            if (current == ancestorId) return true
            val parent = nodeDao.getNodeById(current)
            current = parent?.parentId
        }
        return false
    }

    suspend fun getTotalStorageBytes(owner: String): Long = withContext(Dispatchers.IO) {
        nodeDao.getTotalStorageForOwner(owner) ?: 0L
    }
}
