package com.mithra.assistant.data.local.dao

import androidx.room.*
import com.mithra.assistant.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE isFired = 0 AND triggerTime <= :currentTime ORDER BY triggerTime ASC")
    suspend fun getPendingReminders(currentTime: Long = System.currentTimeMillis()): List<ReminderEntity>

    @Query("SELECT * FROM reminders ORDER BY triggerTime ASC")
    suspend fun getAllReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders ORDER BY triggerTime ASC")
    fun getAllRemindersFlow(): Flow<List<ReminderEntity>>

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
