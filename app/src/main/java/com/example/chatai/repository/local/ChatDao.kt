package com.example.chatai.repository.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chatai.model.data.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * 聊天消息 DAO（数据访问对象）：支持多模态消息的存储操作
 */
@Dao
interface ChatDao {

    // 插入单条消息（主键冲突时替换，比如重新发送失败的消息）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    // 插入多条消息（批量保存历史记录）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessage>)

    // 查询指定会话的所有消息（按时间戳升序，即聊天记录从上到下）
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySessionId(sessionId: String): Flow<List<ChatMessage>>

    // 删除指定会话的所有消息（清空当前聊天记录）
    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteAllMessagesForSession(sessionId: String)

    // 删除单条消息（比如删除某张生成的图片）
    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    // 删除所有会话的消息（清空全部历史）
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()
}