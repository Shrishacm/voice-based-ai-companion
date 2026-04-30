package com.mithra.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientName: String,
    val medicineName: String,
    val dosage: String,           // e.g. "75mg"
    val frequency: String,        // e.g. "Twice daily"
    val timing: String,           // e.g. "8:00 AM, 10:00 PM"
    val instructions: String = "", // e.g. "Before food"
    val isActive: Boolean = true,
    val reminderHour1: Int = -1,  // -1 = no reminder
    val reminderMinute1: Int = 0,
    val reminderHour2: Int = -1,
    val reminderMinute2: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
