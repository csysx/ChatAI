package com.example.chatai.model.data

import com.google.gson.annotations.SerializedName

// API 请求体模型
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>
)

// API 响应体模型
data class ChatCompletionResponse(
    val id: String,
    @SerializedName("object")
    val objectX: String, // 注意：object 是 Kotlin 关键字，需要用 @SerializedName 或改名
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage
) {
    // 辅助函数，方便从响应中提取 AI 回复的消息
    fun toChatMessage(): ChatMessage {
        return choices.firstOrNull()?.message?.let {
            ChatMessage(
                role = when (it.role.lowercase()) {
                    "assistant" -> MessageRole.AI
                    "user" -> MessageRole.USER
                    else -> MessageRole.AI // 默认视为 AI
                },
                content = it.content
            )
        } ?: ChatMessage(role = MessageRole.AI, content = "抱歉，未能获取到有效回复。")
    }
}

data class Choice(
    val message: ApiMessage,
    val finish_reason: String,
    val index: Int
)

// API 响应中的消息结构
data class ApiMessage(
    val role: String,
    val content: String
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)



data class ImageGenerationRequest(
    val model: String,             // 模型名，例如 "siliconflow-image-v1"
    val prompt: String,            // 图片生成提示语
    val n: Int = 1,                // 生成图片数量
    val size: String = "1024x1024" // 图片尺寸，可选 "256x256", "512x512", "1024x1024"
)

data class ImageGenerationResponse(
    val created: Long,
    val data: List<ImageData>
)

data class ImageData(
    val url: String?,           // 图片 URL（在线访问）
    @SerializedName("b64_json")
    val b64Json: String?        // Base64 编码图片（可选）
)
