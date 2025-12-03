package com.example.chatai.model.data

import com.example.chatai.model.data.Session

/**
 * 会话UI状态（仅包含会话列表、选中状态等）
 */
data class SessionUiState(
    val sessions: List<Session> = emptyList(),
    val selectedSessionId: String = "", // 当前选中的会话ID
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)