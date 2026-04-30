package com.mithra.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val relation: String,
    val age: Int,
    val bloodGroup: String,
    val phone: String,
    val conditions: String = "",          // comma-separated
    val allergies: String = "None known", // comma-separated
    val notes: String = "",
    val lastCheckup: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
