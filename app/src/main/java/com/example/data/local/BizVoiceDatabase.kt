package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CallRecordEntity::class, ContactEntity::class],
    version = 2,
    exportSchema = false
)
abstract class BizVoiceDatabase : RoomDatabase() {
    abstract fun callRecordDao(): CallRecordDao
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: BizVoiceDatabase? = null

        fun getDatabase(context: Context): BizVoiceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BizVoiceDatabase::class.java,
                    "bizvoice_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
