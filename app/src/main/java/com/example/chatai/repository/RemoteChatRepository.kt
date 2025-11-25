package com.example.chatai.repository

import android.util.Log
import com.example.chatai.model.config.ModelManager
import com.example.chatai.model.data.ChatCompletionRequest
import com.example.chatai.model.data.ChatMessage
import com.example.chatai.model.data.ImageGenerationRequest
import com.example.chatai.model.data.ImageGenerationResponse
import com.example.chatai.model.data.MessageRole
import javax.inject.Inject

class RemoteChatRepository @Inject constructor(
    private val apiService: ApiService
) : ChatRepository {

    // ---------------------------
    // 1) 发送文本消息（对话）
    // ---------------------------
    override suspend fun sendMessage(text: String): ChatMessage {

        val modelName = ModelManager.chatModel   // ⭐用对话模型！

        val request = ChatCompletionRequest(
            model = modelName,
            messages = listOf(ChatMessage(role = MessageRole.USER, content = text))
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
    suspend fun generateImage(prompt: String): ImageGenerationResponse {

        val modelName = ModelManager.imageModel  // ⭐用图片模型！

        val request = ImageGenerationRequest(
            model = modelName,
            prompt = prompt,
            size = "1024x1024",
            n = 1
        )

        val response = apiService.generateImage(request)

        if (response.isSuccessful) {
            return response.body()
                ?: throw Exception("Image API response body is null")
        } else {
            val error = response.errorBody()?.string()
            Log.e("API_ERROR", "Image error: $error")
            throw Exception("Image API failed: $error")
        }
    }
}