package com.example.chatai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatai.model.data.SessionUiState
import com.example.chatai.model.intent.SessionIntent
import com.example.chatai.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 会话ViewModel（仅处理会话相关逻辑，不涉及任何消息逻辑）
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    // 初始化：加载默认用户的会话列表
    init {
        handleIntent(SessionIntent.LoadUserSessions("default_user"))
    }

    // 仅处理会话相关意图
    fun handleIntent(intent: SessionIntent) {
        when (intent) {
            is SessionIntent.CreateNewSession -> createNewSession(intent.userId)
            is SessionIntent.SwitchSession -> switchSession(intent.sessionId)
            is SessionIntent.DeleteSession -> deleteSession(intent.sessionId)
            is SessionIntent.UpdateSessionTitle -> updateSessionTitle(intent.sessionId, intent.title)
            is SessionIntent.LoadUserSessions -> loadUserSessions(intent.userId)
        }
    }

    // -------------------------- 仅会话相关方法 --------------------------
    private fun loadUserSessions(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            sessionRepository.getUserSessions(userId).collectLatest { sessions ->
                _uiState.update {
                    it.copy(
                        sessions = sessions,
                        isLoading = false,
                        // 默认选中第一个会话（如果有）
                        selectedSessionId = sessions.firstOrNull()?.id ?: ""
                    )
                }
            }
        }
    }

    private fun createNewSession(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newSession = sessionRepository.createNewSession(userId)
                // 创建后自动选中新会话
                _uiState.update {
                    it.copy(
                        selectedSessionId = newSession.id,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "创建会话失败：${e.message}")
                }
            }
        }
    }

    private fun switchSession(sessionId: String) {
        _uiState.update {
            it.copy(
                selectedSessionId = sessionId,
                errorMessage = null
            )
        }
    }

    private fun deleteSession(sessionId: String) {
        if (sessionId == _uiState.value.selectedSessionId) {
            _uiState.update { it.copy(errorMessage = "不能删除当前选中的会话") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                sessionRepository.deleteSession(sessionId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "删除会话失败：${e.message}")
                }
            }
        }
    }

    private fun updateSessionTitle(sessionId: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val session = sessionRepository.getSessionById(sessionId) ?: return@launch
                sessionRepository.updateSession(session.copy(title = title))
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "修改标题失败：${e.message}")
                }
            }
        }
    }

    // 对外提供：通过消息更新会话最后一条消息（仅在消息发送后调用）
    fun updateSessionLastMessage(sessionId: String, lastMessage: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionRepository.getSessionById(sessionId) ?: return@launch
            sessionRepository.updateSession(
                session.copy(
                    lastMessage = lastMessage,
                    lastMessageTime = System.currentTimeMillis()
                )
            )
            // 首次发送消息：设置会话标题
            if (session.title == "新会话") {
                sessionRepository.setSessionTitleByFirstMessage(sessionId, lastMessage)
            }
        }
    }
}