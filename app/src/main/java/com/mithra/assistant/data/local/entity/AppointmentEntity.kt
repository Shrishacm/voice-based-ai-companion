package com.mithra.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientName: String,
    val doctorName: String,
    val specialty: String,
    val hospital: String,
    val date: String,             // "YYYY-MM-DD"
    val time: String,             // "HH:MM AM/PM"
    val triggerTimeMillis: Long,  // epoch ms for alarm
    val notes: String = "",
    val reminderScheduled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
