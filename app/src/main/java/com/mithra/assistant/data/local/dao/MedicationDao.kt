package com.mithra.assistant.data.local.dao

import androidx.room.*
import com.mithra.assistant.data.local.entity.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: MedicationEntity): Long

    @Update
    suspend fun update(medication: MedicationEntity)

    @Delete
    suspend fun delete(medication: MedicationEntity)

    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY medicineName ASC")
    fun getActiveMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications ORDER BY medicineName ASC")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications ORDER BY medicineName ASC")
    suspend fun getAllMedicationsOnce(): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE patientName = :patientName ORDER BY medicineName ASC")
    suspend fun getMedicationsForPatient(patientName: String): List<MedicationEntity>
}
