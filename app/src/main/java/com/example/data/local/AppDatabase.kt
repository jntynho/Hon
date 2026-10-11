package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.converters.RoomConverters
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        LinkEntity::class,
        ActorEntity::class,
        StudioEntity::class,
        SettingsEntity::class
    ],
    version = 18,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun linkDao(): LinkDao
    abstract fun actorDao(): ActorDao
    abstract fun studioDao(): StudioDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN enableVideoPlayerGestures INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN navBarHeightDp INTEGER NOT NULL DEFAULT 64")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN navBarTransparency REAL NOT NULL DEFAULT 0.65")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN navBarBlurDp INTEGER NOT NULL DEFAULT 24")
            }
        }

        private val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN isPasscodeEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN passcodeHash TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN navBarActiveTabBlurDp INTEGER NOT NULL DEFAULT 16")
                db.execSQL("UPDATE app_settings SET navBarHeightDp = 70 WHERE navBarHeightDp = 64")
            }
        }

        private val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN navBarActiveTabIntensity REAL NOT NULL DEFAULT 0.6")
            }
        }

        private val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN blockMalePerformers INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN enableGlassTophead INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN topheadGlassIntensity REAL NOT NULL DEFAULT 0.65")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "goony_database"
                )
                    .addMigrations(MIGRATION_6_7, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
