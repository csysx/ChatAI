package com.example.chatai.repository.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chatai.model.data.Session
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    // 插入新会话
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session)

    // 更新会话（如标题、最后一条消息）
    @Update
    suspend fun updateSession(session: Session)

    // 查询指定用户的所有会话（按最后消息时间倒序）
    @Query("SELECT * FROM sessions WHERE userId = :userId ORDER BY lastMessageTime DESC")
    fun getSessionsByUserId(userId: String): Flow<List<Session>>

    // 查询单个会话
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): Session?

    // 删除会话（同时删除该会话下的所有消息）
    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesBySessionId(sessionId: String)
}