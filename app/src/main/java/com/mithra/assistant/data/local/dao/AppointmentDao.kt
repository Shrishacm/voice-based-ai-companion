package com.mithra.assistant.data.local.dao

import androidx.room.*
import com.mithra.assistant.data.local.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appointment: AppointmentEntity): Long

    @Update
    suspend fun update(appointment: AppointmentEntity)

    @Delete
    suspend fun delete(appointment: AppointmentEntity)

    @Query("SELECT * FROM appointments ORDER BY triggerTimeMillis ASC")
    fun getAllAppointments(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments ORDER BY triggerTimeMillis ASC")
    suspend fun getAllAppointmentsOnce(): List<AppointmentEntity>

    @Query("SELECT * FROM appointments WHERE triggerTimeMillis >= :now ORDER BY triggerTimeMillis ASC")
    suspend fun getUpcomingAppointments(now: Long = System.currentTimeMillis()): List<AppointmentEntity>
}
