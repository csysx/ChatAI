package com.example.chatai.repository

import com.example.chatai.model.data.Session
import kotlinx.coroutines.flow.Flow

/**
 * 会话管理接口（独立于ChatRepository，职责单一）
 */
interface SessionRepository {
    // 创建新会话
    suspend fun createNewSession(userId: String = "default_user"): Session

    // 获取指定用户的所有会话（流式响应，实时更新）
    fun getUserSessions(userId: String = "default_user"): Flow<List<Session>>

    // 获取单个会话详情
    suspend fun getSessionById(sessionId: String): Session?

    // 更新会话信息（标题、最后消息等）
    suspend fun updateSession(session: Session)

    // 删除会话（含会话下的所有消息）
    suspend fun deleteSession(sessionId: String)

    // 为会话设置标题（从首条消息提取）
    suspend fun setSessionTitleByFirstMessage(sessionId: String, firstMessage: String)
}