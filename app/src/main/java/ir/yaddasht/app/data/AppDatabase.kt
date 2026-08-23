package ir.yaddasht.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val MIGRATION_2_4 = object : Migration(2, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `attachments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `noteId` NOT NULL, `fileName` TEXT NOT NULL, `filePath` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `isImage` NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `task_attachments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskId` NOT NULL, `fileName` TEXT NOT NULL, `filePath` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `isImage` NOT NULL)")
    }
}
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `task_attachments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskId` NOT NULL, `fileName` TEXT NOT NULL, `filePath` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `isImage` NOT NULL)")
    }
}

@Database(
    entities = [Note::class, Task::class, Attachment::class, TaskAttachment::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): NoteDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "yaddasht_database")
                    .addMigrations(MIGRATION_2_4, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
