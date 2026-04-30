package com.mithra.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userText: String,
    val assistantText: String,
    val language: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String
)
