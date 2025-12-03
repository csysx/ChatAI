package com.example.chatai.model.state

import com.example.chatai.model.data.ChatMessage

/**
 * 对话界面的 UI 状态（所有界面展示的数据都在这里管理）
 * 特点：单向数据流——UI 只读取状态，不直接修改；修改需通过 ViewModel
 * @param messages 对话消息列表（界面要显示的所有聊天记录）
 * @param inputText 输入框中的文本（用户正在输入的内容）
 * @param isLoading 是否正在加载（AI是否在回复中，控制“发送按钮”是否禁用）
 * @param errorMessage 错误信息（如发送失败提示，为空表示无错误）
 */
data class ChatUiState(
    // 初始状态：空消息列表、空输入框、未加载、无错误
    public val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentSessionId: String = "" // 仅存储当前活跃会话ID（用于关联消息）
)