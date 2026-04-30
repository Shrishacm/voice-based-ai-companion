package com.mithra.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val type: String = "medicine",      // "medicine" | "appointment"
    val triggerTime: Long,
    val isRecurring: Boolean = false,
    val recurrencePattern: String? = null,
    val isFired: Boolean = false
)
