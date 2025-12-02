package com.example.chatai.repository

import android.content.Context
import android.util.Log
import com.example.chatai.repository.local.ChatDao
import com.example.chatai.model.data.ApiMessage
//import com.example.chatai.model.config.ModelManager
import com.example.chatai.model.data.ChatCompletionRequest
import com.example.chatai.model.data.ChatMessage
import com.example.chatai.model.data.ImageGenerationRequest
import com.example.chatai.model.data.MessageRole
import com.example.chatai.model.data.MessageStatus
import com.example.chatai.model.data.MessageType
import com.example.chatai.model.data.VideoGenerationRequest
import com.example.chatai.model.data.VideoStatusRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.core.net.toUri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody


class RemoteChatRepository @Inject constructor(
    private val apiService: ApiService,
    private val chatDao: ChatDao, // <-- 注入 ChatDao
    @ApplicationContext private val context: Context
) : ChatRepository {

    private val currentSessionId = "default_session" // 当前会话 ID
    // ---------------------------
    // 文本消息（对话）
    // ---------------------------
    override suspend fun sendMessage(text: String): ChatMessage {

        // 1. 创建用户文本消息（本地先存，优化 UI 响应）----------
        val userTextMessage = ChatMessage(
            role = MessageRole.USER,
            content = text,
            type = MessageType.TEXT,
            status = MessageStatus.SUCCESS,
            sessionId = currentSessionId
        )
        chatDao.insertMessage(userTextMessage)


        // 2. 调用文本生成 API------------------

        // 自定义 Prompt 模板
        val systemPrompt = "你是一位 helpful 的助手，你的回答要简洁明了。"
//        val modelName = ModelManager.chatModel   // ⭐用对话模型！
        val modelName="Qwen/Qwen3-8B"

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

                // 3. 解析 API 响应，创建 AI 文本消息-------------
                val aiTextMessage = ChatMessage(
                    role = MessageRole.AI,
                    content = response.body()!!.choices.first().message.content,
                    type = MessageType.TEXT,
                    status = MessageStatus.SUCCESS,
                    sessionId = currentSessionId
                )
                chatDao.insertMessage(aiTextMessage) // 本地存储 AI 文本消息
                return aiTextMessage
//            return body.toChatMessage()
            } else { // 响应体为空
            throw Exception("API response body is null") }

        } else {
            val error = response.errorBody()?.string()
            Log.e("API_ERROR", "Chat error: $error")
            throw Exception("Chat API failed: $error")
        }


    }


    // ---------------------------
    // 生成图片
    // ---------------------------
    override suspend fun generateImage(prompt: String): String {
        // 1. 保存用户的图像生成指令
        val userImagePromptMessage = ChatMessage(
            role = MessageRole.USER,
            content = prompt,
            type = MessageType.TEXT,
            status = MessageStatus.SUCCESS,
            sessionId = currentSessionId
        )
        chatDao.insertMessage(userImagePromptMessage)

        // 2. 插入 AI 图像加载中消息
        val aiLoadingMessage = ChatMessage(
            role = MessageRole.AI,
            content = "",
            type = MessageType.IMAGE,
            status = MessageStatus.LOADING,
            isLoading = true,
            sessionId = currentSessionId
        )
        chatDao.insertMessage(aiLoadingMessage)

        // 3. 调用图像生成 API
        val request = ImageGenerationRequest(
            prompt = prompt,
            model = "Kwai-Kolors/Kolors"
        )
        val response = apiService.generateImage(request)
        if (response.isSuccessful) {
            val imageUrl = response.body()?.data?.firstOrNull()?.url
                ?: throw Exception("生成图片失败：未获取到有效图像 URL")

            // 4. 下载图片到本地存储
            val bitmap = withContext(Dispatchers.IO) { loadBitmapFromUrl(imageUrl) }
            val file = File(context.filesDir, "ai_image_${System.currentTimeMillis()}.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val localUri = file.toUri().toString() // 生成可访问 URI

            // 5. 替换加载中消息为成功消息，content 改为本地 URI
            val aiImageMessage = aiLoadingMessage.copy(
                content = localUri, // 使用本地路径
                type = MessageType.IMAGE,
                status = MessageStatus.SUCCESS,
                isLoading = false
            )
            chatDao.insertMessage(aiImageMessage)

            return aiImageMessage.content
        } else {
            val errorBody = response.errorBody()?.string() ?: "未知错误"
            throw Exception("图像生成失败: $errorBody")
        }
    }



    // ---------------------------
    // 生成视频
    // ---------------------------
    override suspend fun generateVideo(prompt: String,imagePath: String?): ChatMessage {

        // 1. 存用户 prompt 消息
        val userMsg = ChatMessage(
            role = MessageRole.USER,
            content = prompt,
            type = MessageType.TEXT,
            status = MessageStatus.SUCCESS,
            sessionId = currentSessionId
        )
        chatDao.insertMessage(userMsg)

        // 2. 插入一个 AI “加载中”消息
        val loadingMsg = ChatMessage(
            role = MessageRole.AI,
            content = "",
            type = MessageType.VIDEO,
            status = MessageStatus.LOADING,
            isLoading = true,
            sessionId = currentSessionId
        )
        chatDao.insertMessage(loadingMsg)

        try {


            val requestBody = if (imagePath != null) {
                // ---- 分支 A: 图生视频 (I2V) ----
                val file = File(imagePath)
                if (!file.exists()) throw Exception("图片文件不存在")

                // 1. 将本地文件转为 Base64 字符串
                // 格式必须是: "data:image/jpeg;base64,......"
                val base64Image = file.readBytes().let { bytes ->
                    val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    "data:image/jpeg;base64,$base64Str"
                }

                // 2. 构造 JSON 请求
                // ⚠️ 修正模型名称：必须是 Wan2.1-I2V-14B (Wan2.2 不存在)
                // ⚠️ 分辨率：Wan 2.1 I2V 推荐 1280x720
                VideoGenerationRequest(
                    model = "Wan-AI/Wan2.2-I2V-A14B",
                    prompt = prompt,
                    image = base64Image, // 传 Base64 字符串
                    imageSize = "1280x720",
                    seed = (1..100000).random() // 随机种子
                )
            } else {
                // ---- 分支 B: 文生视频 (T2V) ----
                // ⚠️ 修正模型名称：推荐 LTX-Video (Wan2.2 T2V 也不存在)
                VideoGenerationRequest(
                    model = "Lightricks/LTX-Video",
                    prompt = prompt,
                    imageSize = "768x512" // LTX-Video 必须用这个分辨率
                )
            }
            val submitResp = apiService.submitVideoTask(requestBody)


            if (!submitResp.isSuccessful || submitResp.body() == null) {
                throw Exception("视频任务提交失败: ${submitResp.errorBody()?.string()}")
            }

            val requestId = submitResp.body()!!.requestId
            if (requestId.isBlank()) throw Exception("submit 未返回 requestId")


            // 4. 开始轮询 status
            var finalUrl: String? = null
            repeat(500) {   // 最多轮询 30 次（约 30 秒）
                delay(3000)

                val statusResp = apiService.checkVideoStatus(VideoStatusRequest(requestId))

                if (!statusResp.isSuccessful || statusResp.body() == null) {
                    return@repeat
                }

                val body = statusResp.body()!!

                when (body.status) {
                    "Pending", "Running","InProgress","Processing" -> {
                        // 继续等
                    }

                    "Failed", "Error" -> {
                        throw Exception("视频生成失败: ${body.reason}")
                    }

                    "Succeed" -> {
                        finalUrl = body.results
                            ?.videos
                            ?.firstOrNull()
                            ?.url

                        if (finalUrl != null) return@repeat
                    }
                }
            }

            // 轮询结束仍然没有
            val url = finalUrl ?: throw Exception("视频生成超时或未返回 URL")

            // 5. 成功 → 覆盖加载消息
            val successMsg = loadingMsg.copy(
                content = url,
                isLoading = false,
                status = MessageStatus.SUCCESS
            )
            chatDao.insertMessage(successMsg)
            return successMsg

        } catch (e: Exception) {

            // 6. 失败 → 覆盖加载消息
            val failedMsg = loadingMsg.copy(
                content = "视频生成失败：${e.message}",
                isLoading = false,
                status = MessageStatus.FAILURE
            )
            chatDao.insertMessage(failedMsg)
            throw e
        }
    }




    // -------------------------- 本地存储操作 --------------------------
    override fun getMessages(sessionId: String): Flow<List<ChatMessage>> {
        // 监听指定会话的所有消息（文本/图像/视频）
        return chatDao.getMessagesBySessionId(sessionId)
    }

    override suspend fun clearMessages(sessionId: String) {
        chatDao.deleteAllMessagesForSession(sessionId)
    }

    override suspend fun deleteMessage(messageId: String) {
        chatDao.deleteMessageById(messageId)
    }

    // -------------------------- 辅助方法 --------------------------
    // 将 ChatMessage 转成 API 所需的 Message 格式（适配你原有 API 逻辑）
    private fun ChatMessage.toApiMessage(): ApiMessage {
        return ApiMessage(
            role = this.role.name.lowercase(),
            content = this.content
        )
    }

    // -------------------------- 网络图片下载辅助方法 --------------------------
    private fun loadBitmapFromUrl(urlString: String): Bitmap {
        val url = java.net.URL(urlString)
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.doInput = true
        connection.connect()
        val input = connection.inputStream
        return android.graphics.BitmapFactory.decodeStream(input)
    }



}