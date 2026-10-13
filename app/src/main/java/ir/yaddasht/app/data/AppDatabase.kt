package ir.yaddasht.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val MIGRATION_2_4 = object : Migration(2, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `attachments` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`noteId` INTEGER NOT NULL, " +
                "`fileName` TEXT NOT NULL, " +
                "`filePath` TEXT NOT NULL, " +
                "`mimeType` TEXT NOT NULL, " +
                "`isImage` INTEGER NOT NULL)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `task_attachments` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`taskId` INTEGER NOT NULL, " +
                "`fileName` TEXT NOT NULL, " +
                "`filePath` TEXT NOT NULL, " +
                "`mimeType` TEXT NOT NULL, " +
                "`isImage` INTEGER NOT NULL)"
        )
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `task_attachments` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`taskId` INTEGER NOT NULL, " +
                "`fileName` TEXT NOT NULL, " +
                "`filePath` TEXT NOT NULL, " +
                "`mimeType` TEXT NOT NULL, " +
                "`isImage` INTEGER NOT NULL)"
        )
    }
}

private fun createHabitTables(db: SupportSQLiteDatabase) {
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `habits` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`title` TEXT NOT NULL, " +
            "`emoji` TEXT NOT NULL, " +
            "`colorIndex` INTEGER NOT NULL, " +
            "`kind` TEXT NOT NULL, " +
            "`targetPerDay` INTEGER NOT NULL, " +
            "`unit` TEXT NOT NULL, " +
            "`pattern` TEXT NOT NULL, " +
            "`weekdaysMask` INTEGER NOT NULL, " +
            "`everyNDays` INTEGER NOT NULL, " +
            "`cueText` TEXT NOT NULL, " +
            "`rewardText` TEXT NOT NULL, " +
            "`archived` INTEGER NOT NULL, " +
            "`createdAt` INTEGER NOT NULL)"
    )

    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `habit_logs` (" +
            "`habitId` INTEGER NOT NULL, " +
            "`dateKey` TEXT NOT NULL, " +
            "`count` INTEGER NOT NULL, " +
            "`doneAt` INTEGER NOT NULL, " +
            "`rest` INTEGER NOT NULL, " +
            "PRIMARY KEY(`habitId`, `dateKey`), " +
            "FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) " +
            "ON UPDATE NO ACTION ON DELETE CASCADE)"
    )

    db.execSQL(
        "CREATE INDEX IF NOT EXISTS `index_habit_logs_habitId` " +
            "ON `habit_logs` (`habitId`)"
    )
}

private val MIGRATION_4_6 = object : Migration(4, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        createHabitTables(db)
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `habit_logs`")
        db.execSQL("DROP TABLE IF EXISTS `habits`")
        createHabitTables(db)
    }
}

@Database(
    entities = [
        Note::class,
        Task::class,
        Attachment::class,
        TaskAttachment::class,
        Habit::class,
        HabitLog::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yaddasht_database"
                )
                    .addMigrations(
                        MIGRATION_2_4,
                        MIGRATION_3_4,
                        MIGRATION_4_6,
                        MIGRATION_5_6
                    )
                    // عمداً fallbackToDestructiveMigration() اضافه نشده است.
                    // اگر مهاجرتی کم باشد، Room کرش می‌کند ولی داده پاک نمی‌شود.
                    .build()

                INSTANCE = instance
                instance
            }
    }
}
