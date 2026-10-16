package ir.yaddasht.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class HabitDao {

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY createdAt ASC")
    abstract fun observeHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    abstract suspend fun getHabitByIdSync(habitId: Long): Habit?

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY createdAt ASC")
    abstract suspend fun getActiveHabitsSync(): List<Habit>

    @Query("SELECT * FROM habit_logs")
    abstract fun observeAllLogs(): Flow<List<HabitLog>>

    @Query(
        "SELECT * FROM habit_logs " +
            "WHERE habitId = :habitId AND dateKey = :dateKey " +
            "LIMIT 1"
    )
    abstract suspend fun getLog(habitId: Long, dateKey: String): HabitLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertLog(log: HabitLog)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND dateKey = :dateKey")
    abstract suspend fun deleteLog(habitId: Long, dateKey: String)

    @Insert
    abstract suspend fun insertHabit(habit: Habit): Long

    @Update
    abstract suspend fun updateHabit(habit: Habit)

    @Query("UPDATE habits SET archived = 1 WHERE id = :id")
    abstract suspend fun archiveHabit(id: Long)

    @Transaction
    open suspend fun toggleBinary(habitId: Long, dateKey: String) {
        val current = getLog(habitId, dateKey)
        if (current != null && current.count >= 1 && !current.rest) {
            deleteLog(habitId, dateKey)
        } else {
            upsertLog(
                HabitLog(
                    habitId = habitId,
                    dateKey = dateKey,
                    count = 1,
                    doneAt = System.currentTimeMillis(),
                    rest = false
                )
            )
        }
    }

    @Transaction
    open suspend fun addCount(habitId: Long, dateKey: String, delta: Int) {
        val current = getLog(habitId, dateKey)
        val nextCount = ((current?.count ?: 0) + delta).coerceAtLeast(0)
        if (nextCount == 0) {
            deleteLog(habitId, dateKey)
        } else {
            upsertLog(
                HabitLog(
                    habitId = habitId,
                    dateKey = dateKey,
                    count = nextCount,
                    doneAt = System.currentTimeMillis(),
                    rest = false
                )
            )
        }
    }

    @Transaction
    open suspend fun markDone(habitId: Long, dateKey: String, target: Int, isCount: Boolean) {
        val count = if (isCount) target.coerceAtLeast(1) else 1

        upsertLog(
            HabitLog(
                habitId = habitId,
                dateKey = dateKey,
                count = count,
                doneAt = System.currentTimeMillis(),
                rest = false
            )
        )
    }

    @Transaction
    open suspend fun toggleRest(habitId: Long, dateKey: String) {
        val current = getLog(habitId, dateKey)
        val nextRest = !(current?.rest ?: false)
        val nextCount = current?.count ?: 0
        if (!nextRest && nextCount == 0) {
            deleteLog(habitId, dateKey)
        } else {
            upsertLog(
                HabitLog(
                    habitId = habitId,
                    dateKey = dateKey,
                    count = nextCount,
                    doneAt = System.currentTimeMillis(),
                    rest = nextRest
                )
            )
        }
    }
}
