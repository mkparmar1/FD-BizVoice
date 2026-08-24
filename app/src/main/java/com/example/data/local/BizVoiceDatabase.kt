package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CallRecordEntity::class, ContactEntity::class],
    version = 3,
    exportSchema = false
)
abstract class BizVoiceDatabase : RoomDatabase() {
    abstract fun callRecordDao(): CallRecordDao
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: BizVoiceDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 1 to 2 changes
                try {
                    db.execSQL("ALTER TABLE call_records ADD COLUMN isRecorded INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE call_records ADD COLUMN recordingDurationSeconds INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE call_records ADD COLUMN recordingUrl TEXT DEFAULT NULL")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 2 to 3 changes: added DND, Blacklist, notes to contacts
                try {
                    db.execSQL("ALTER TABLE contacts ADD COLUMN isDnd INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE contacts ADD COLUMN isBlacklisted INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE contacts ADD COLUMN notes TEXT DEFAULT NULL")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
            }
        }

        fun getDatabase(context: Context): BizVoiceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BizVoiceDatabase::class.java,
                    "bizvoice_v3.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

