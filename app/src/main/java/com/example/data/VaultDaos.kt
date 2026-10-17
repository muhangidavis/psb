package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultAccountDao {
    @Query("SELECT * FROM vault_accounts")
    fun getAllAccounts(): Flow<List<VaultAccountEntity>>

    @Query("SELECT * FROM vault_accounts WHERE animal = :animal LIMIT 1")
    suspend fun getAccount(animal: String): VaultAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: VaultAccountEntity)

    @Query("DELETE FROM vault_accounts WHERE animal = :animal")
    suspend fun deleteAccount(animal: String)
}

@Dao
interface VaultNodeDao {
    @Query("SELECT * FROM vault_nodes WHERE ownerAnimal = :owner AND parentId = :parentId ORDER BY CASE WHEN type = 'folder' THEN 0 ELSE 1 END, name ASC")
    fun getNodesInFolder(owner: String, parentId: String): Flow<List<VaultNodeEntity>>

    @Query("SELECT * FROM vault_nodes WHERE ownerAnimal = :owner")
    suspend fun getAllNodesForOwner(owner: String): List<VaultNodeEntity>

    @Query("SELECT * FROM vault_nodes WHERE id = :id LIMIT 1")
    suspend fun getNodeById(id: String): VaultNodeEntity?

    @Query("SELECT * FROM vault_nodes WHERE ownerAnimal = :owner AND parentId = :parentId")
    suspend fun getDirectChildren(owner: String, parentId: String): List<VaultNodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: VaultNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<VaultNodeEntity>)

    @Update
    suspend fun updateNode(node: VaultNodeEntity)

    @Delete
    suspend fun deleteNode(node: VaultNodeEntity)

    @Query("DELETE FROM vault_nodes WHERE id IN (:ids)")
    suspend fun deleteNodesById(ids: List<String>)

    @Query("DELETE FROM vault_nodes WHERE ownerAnimal = :owner")
    suspend fun deleteAllForOwner(owner: String)

    @Query("SELECT SUM(size) FROM vault_nodes WHERE ownerAnimal = :owner")
    suspend fun getTotalStorageForOwner(owner: String): Long?
}
