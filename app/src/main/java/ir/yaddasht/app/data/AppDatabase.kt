package ir.yaddasht.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Note::class, Task::class, TaskAttachment::class, Attachment::class],
    version = 5, // اگر قبلاً version دیگری داشتید، همین عدد را نگه دارید
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): NoteDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yaddasht_database"
                )
                .fallbackToDestructiveMigration() // در صورت نیاز به Migrationهای دستی، این خط را حذف و Migration اضافه کنید
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
