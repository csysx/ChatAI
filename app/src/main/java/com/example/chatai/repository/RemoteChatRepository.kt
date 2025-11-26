package com.example.chatai.repository

import android.util.Log
import com.example.chatai.model.config.ModelManager
import com.example.chatai.model.data.ChatCompletionRequest
import com.example.chatai.model.data.ChatMessage
import com.example.chatai.model.data.ImageGenerationRequest
import com.example.chatai.model.data.MessageRole
import javax.inject.Inject

class RemoteChatRepository @Inject constructor(
    private val apiService: ApiService
) : ChatRepository {

    // ---------------------------
    // 1) 发送文本消息（对话）
    // ---------------------------
    override suspend fun sendMessage(text: String): ChatMessage {

        // 自定义 Prompt 模板
        val systemPrompt = "你是一位 helpful 的助手，你的回答要简洁明了。"

        val modelName = ModelManager.chatModel   // ⭐用对话模型！

        val request = ChatCompletionRequest(
            model = modelName,
            messages = listOf(// 添加系统消息作为第一个元素
                ChatMessage(
                    role = MessageRole.SYSTEM, // 假设你有一个 SYSTEM 角色
                    content = systemPrompt
                ),
                ChatMessage(
                    role = MessageRole.USER,
                    content = text)
            )
        )

        val response = apiService.createChatCompletion(request)

        if (response.isSuccessful) {
            val body = response.body()

            if (body != null) { // 调用之前写的辅助函数，将 API 响应转换为ChatMessage
            return body.toChatMessage()
            } else { // 响应体为空
            throw Exception("API response body is null") }

        } else {
            val error = response.errorBody()?.string()
            Log.e("API_ERROR", "Chat error: $error")
            throw Exception("Chat API failed: $error")
        }
    }



    // ---------------------------
    // 2) 生成图片
    // ---------------------------
    override suspend fun generateImage(prompt: String): String {
        val request = ImageGenerationRequest(
            prompt = prompt,
            model = "Kwai-Kolors/Kolors" // 可替换为其他图像模型
        )
        val response = apiService.generateImage(request)
        if (response.isSuccessful) {
            return response.body()?.data?.firstOrNull()?.url ?: throw Exception("生成图片失败")
        } else {
            val errorBody = response.errorBody()?.string() ?: "未知错误"
            throw Exception("图像生成失败: $errorBody")
        }
    }
}