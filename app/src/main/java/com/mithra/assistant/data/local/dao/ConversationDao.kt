package com.mithra.assistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mithra.assistant.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getConversationsBySession(sessionId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentConversations(limit: Int = 20): List<ConversationEntity>

    @Query("SELECT DISTINCT sessionId FROM conversations ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSessionIds(limit: Int = 10): List<String>

    @Query("DELETE FROM conversations WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
}
