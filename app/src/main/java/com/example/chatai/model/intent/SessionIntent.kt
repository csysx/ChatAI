package com.example.chatai.model.intent

/**
 * 会话相关意图（仅包含会话创建、切换、删除、标题修改等）
 */
sealed class SessionIntent {
    data class CreateNewSession(val userId: String = "default_user") : SessionIntent()
    data class SwitchSession(val sessionId: String) : SessionIntent()
    data class DeleteSession(val sessionId: String) : SessionIntent()
    data class UpdateSessionTitle(val sessionId: String, val title: String) : SessionIntent()
    data class LoadUserSessions(val userId: String = "default_user") : SessionIntent()
}