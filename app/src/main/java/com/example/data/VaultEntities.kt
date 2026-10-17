package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "vault_accounts")
data class VaultAccountEntity(
    @PrimaryKey
    val animal: String,
    val saltB64: String,
    val verifierB64: String,
    val wrapIvB64: String,
    val wrappedKeyB64: String,
    val pinSaltB64: String,
    val pinVerifierB64: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "vault_nodes",
    indices = [
        Index(value = ["ownerAnimal", "parentId"]),
        Index(value = ["ownerAnimal", "id"])
    ]
)
data class VaultNodeEntity(
    @PrimaryKey
    val id: String,
    val ownerAnimal: String,
    val parentId: String, // "root" or another folder id
    val type: String,     // "folder", "file", "photo", "recording", "note", "link"
    val name: String,
    val mime: String = "application/octet-stream",
    val size: Long = 0L,
    val url: String? = null,
    val noteContentEncrypted: String? = null,
    val ivB64: String? = null,
    val encryptedFileName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
