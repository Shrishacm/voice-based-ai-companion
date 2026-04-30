package com.mithra.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val embedding: String,
    val category: String = "general",
    val timestamp: Long = System.currentTimeMillis()
)
