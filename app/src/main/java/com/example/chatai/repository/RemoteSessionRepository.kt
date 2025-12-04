package com.example.chatai.repository

import com.example.chatai.model.data.Session
import com.example.chatai.repository.local.ChatDao
import com.example.chatai.repository.local.SessionDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 会话管理实现（独立文件，仅处理会话逻辑）
 */
class RemoteSessionRepository (
    private val sessionDao: SessionDao,
) : SessionRepository {

    override suspend fun createNewSession(userId: String): Session {
        val newSession = Session(
            title = "新会话",
            userId = userId,
            createTime = System.currentTimeMillis(),
            lastMessageTime = System.currentTimeMillis()
        )
        sessionDao.insertSession(newSession)
        return newSession
    }

    override fun getUserSessions(userId: String): Flow<List<Session>> {
        return sessionDao.getSessionsByUserId(userId)
    }

    override suspend fun getSessionById(sessionId: String): Session? {
        return sessionDao.getSessionById(sessionId)
    }

    override suspend fun updateSession(session: Session) {
        sessionDao.updateSession(session)
    }

    override suspend fun deleteSession(sessionId: String) {
        // 级联删除：先删会话下的消息，再删会话
        sessionDao.deleteMessagesBySessionId(sessionId)
        sessionDao.deleteSession(sessionId)
    }

    override suspend fun setSessionTitleByFirstMessage(sessionId: String, firstMessage: String) {
        val session = sessionDao.getSessionById(sessionId) ?: return
        // 截取前20字作为标题，避免过长
        val title = if (firstMessage.length > 20) {
            firstMessage.substring(0, 20) + "..."
        } else {
            firstMessage
        }
        sessionDao.updateSession(session.copy(title = title))
    }

}