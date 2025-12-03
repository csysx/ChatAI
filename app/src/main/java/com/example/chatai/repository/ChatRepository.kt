package com.example.chatai.repository

import com.example.chatai.model.data.ChatMessage
import kotlinx.coroutines.flow.Flow


/**
 * 聊天数据仓库接口（定义数据操作契约，支持多模态）
 */

interface ChatRepository {
    /**
     * 发送消息并获取AI回复
     * @param text 用户发送的消息内容
     * @return AI 回复的消息（带角色、内容等信息）
     */
    suspend fun sendMessage(text: String,sessionId: String): ChatMessage

    // 生成图片的方法，返回图片的 URL (String)
    suspend fun generateImage(prompt: String,sessionId: String): ChatMessage

    // 发送视频生成请求（返回视频消息）
    suspend fun generateVideo(prompt: String,imagePath: String? = null,sessionId: String): ChatMessage

    // 查询指定会话的所有消息（文本/图像/视频）
    fun getMessages(sessionId: String = "default_session"): Flow<List<ChatMessage>>

    // 清空指定会话的消息
    suspend fun clearMessages(sessionId: String = "default_session")

    // 删除单条消息（比如删除生成失败的图片）
    suspend fun deleteMessage(messageId: String)




}