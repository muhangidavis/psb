package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VaultAccountEntity::class,
        VaultNodeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PsbDatabase : RoomDatabase() {
    abstract fun accountDao(): VaultAccountDao
    abstract fun nodeDao(): VaultNodeDao

    companion object {
        @Volatile
        private var instance: PsbDatabase? = null

        fun getInstance(context: Context): PsbDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PsbDatabase::class.java,
                    "psb_vault.db"
                ).build().also { instance = it }
            }
        }
    }
}
