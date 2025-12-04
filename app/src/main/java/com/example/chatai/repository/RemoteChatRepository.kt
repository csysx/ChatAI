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
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.core.net.toUri
import kotlinx.coroutines.flow.first


class RemoteChatRepository(
    private val apiService: ApiService,
    private val chatDao: ChatDao, // <-- 注入 ChatDao
    @ApplicationContext private val context: Context,
) : ChatRepository {

    private val MAX_CONTEXT_MESSAGES = 20

    private val currentUserId = "default_user" // 当前用户 ID


    // ---------------------------
    // 文本消息（对话）
    // ---------------------------
    override suspend fun sendMessage(text: String, sessionId: String): ChatMessage {

        // 1. 创建用户文本消息（本地先存，优化 UI 响应）----------
        val userTextMessage = ChatMessage(
            role = MessageRole.USER,
            content = text,
            type = MessageType.TEXT,
            status = MessageStatus.SUCCESS,
            sessionId = sessionId,
        )
        chatDao.insertMessage(userTextMessage)

        // 获取当前会话的历史消息（作为上下文）
        val historyMessages = getHistoryMessagesForContext(sessionId)
        // 拼接上下文（历史消息 + 当前用户消息）
        val contextMessages = historyMessages + userTextMessage
        // 转为API需要的格式
        val apiMessages = contextMessages.map { it.toApiMessage() }



        // 2. 调用文本生成 API------------------

        // 自定义 Prompt 模板
        val systemPrompt = "你是一位 helpful 的助手，你的回答要简洁明了。"
        val modelName = "Qwen/Qwen3-8B"

        val request = ChatCompletionRequest(
            model = modelName,
            messages = listOf(ApiMessage("system", systemPrompt)) + apiMessages
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
                    sessionId = sessionId
                )
                chatDao.insertMessage(aiTextMessage) // 本地存储 AI 文本消息
                return aiTextMessage
//            return body.toChatMessage()
            } else { // 响应体为空
                throw Exception("API response body is null")
            }

        } else {
            val error = response.errorBody()?.string()
            Log.e("API_ERROR", "Chat error: $error")
            throw Exception("Chat API failed: $error")
        }


    }


    // ---------------------------
    // 生成图片
    // ---------------------------
    override suspend fun generateImage(prompt: String, sessionId: String): ChatMessage {
        // 1. 保存用户的图像生成指令
        val userImagePromptMessage = ChatMessage(
            role = MessageRole.USER,
            content = prompt,
            type = MessageType.TEXT,
            status = MessageStatus.SUCCESS,
            sessionId = sessionId
        )
        chatDao.insertMessage(userImagePromptMessage)

        // 2. 插入 AI 图像加载中消息
        val aiLoadingMessage = ChatMessage(
            role = MessageRole.AI,
            content = "",
            type = MessageType.IMAGE,
            status = MessageStatus.LOADING,
            isLoading = true,
            sessionId = sessionId
        )
        chatDao.insertMessage(aiLoadingMessage)


        //比如根据历史对话理解用户需求
        val historyMessages = getHistoryMessagesForContext(sessionId)
        val contextPrompt = if (historyMessages.isNotEmpty()) {
            // 拼接历史对话摘要（避免prompt过长）
            val historySummary = historyMessages.takeLast(20).joinToString("，") { "${it.role}:${it.content}" }
            "基于历史对话：$historySummary，生成图像：$prompt"
        } else {
            prompt
        }

        // 3. 调用图像生成 API
        val request = ImageGenerationRequest(
            prompt = contextPrompt,
            model = "Qwen/Qwen-Image"
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
                isLoading = false,
                sessionId = sessionId,
                role = MessageRole.AI
            )
            chatDao.insertMessage(aiImageMessage)

            return aiImageMessage
        } else {
            val errorBody = response.errorBody()?.string() ?: "未知错误"
            throw Exception("图像生成失败: $errorBody")
        }
    }


    // ---------------------------
    // 生成视频
    // ---------------------------
    override suspend fun generateVideo(
        prompt: String,
        imagePath: String?,
        sessionId: String
    ): ChatMessage {

        // 1. 存用户 prompt 消息
        val userMsg = ChatMessage(
            role = MessageRole.USER,
            content = prompt,
            type = MessageType.TEXT,
            status = MessageStatus.SUCCESS,
            sessionId = sessionId
        )
        chatDao.insertMessage(userMsg)

        // 2. 插入一个 AI “加载中”消息
        val loadingMsg = ChatMessage(
            role = MessageRole.AI,
            content = "",
            type = MessageType.VIDEO,
            status = MessageStatus.LOADING,
            isLoading = true,
            sessionId = sessionId
        )
        chatDao.insertMessage(loadingMsg)

        val historyMessages = getHistoryMessagesForContext(sessionId)
        val contextPrompt = if (historyMessages.isNotEmpty()) {
            val historySummary = historyMessages.takeLast(10).joinToString("，") { "${it.role}:${it.content}" }
            "基于历史对话：$historySummary，生成视频：$prompt"
        } else {
            prompt
        }

        try {


            val requestBody = if (imagePath != null) {
                // ---- 分支 A: 图生视频 (I2V) ----
                val file = File(imagePath)
                if (!file.exists()) throw Exception("图片文件不存在")

                // 1. 将本地文件转为 Base64 字符串
                // 格式必须是: "data:image/jpeg;base64,......"
                val base64Image = file.readBytes().let { bytes ->
                    val base64Str =
                        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    "data:image/jpeg;base64,$base64Str"
                }

                // 2. 构造 JSON 请求
                // ⚠️ 修正模型名称：必须是 Wan2.1-I2V-14B (Wan2.2 不存在)
                // ⚠️ 分辨率：Wan 2.1 I2V 推荐 1280x720
                VideoGenerationRequest(
                    model = "Wan-AI/Wan2.2-I2V-A14B",
                    prompt = contextPrompt,
                    image = base64Image, // 传 Base64 字符串
                    imageSize = "1280x720",
                    seed = (1..100000).random() // 随机种子
                )
            } else {
                // ---- 分支 B: 文生视频 (T2V) ----
                // ⚠️ 修正模型名称：推荐 LTX-Video (此模型已经过期了)
                VideoGenerationRequest(
                    model = "Lightricks/LTX-Video",
                    prompt = contextPrompt,
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
            var errorCount = 0 // 连续错误计数器
            loop@ for (i in 0 until 500) {
                delay(3000)

                val statusResp = apiService.checkVideoStatus(VideoStatusRequest(requestId))

                if (!statusResp.isSuccessful || statusResp.body() == null) {
                    Log.w("VideoGen", "状态检查接口返回失败: ${statusResp.code()}")
                    errorCount++
                    // 如果连续失败超过 10 次，认为网络或服务器挂了，停止轮询
                    if (errorCount > 10) throw Exception("连续多次无法获取任务状态，请检查网络")
                    continue@loop
                }

                // 接口成功，重置错误计数
                errorCount = 0

                val body = statusResp.body()!!

                when (body.status) {
                    "Pending", "Running", "InProgress", "Processing" -> {
                        // 继续等
                    }

                    "Failed", "Error" -> {
                        throw Exception("视频生成失败: ${body.reason}")
                    }

                    "Succeed" -> {
                        finalUrl = body.results?.videos?.firstOrNull()?.url
                        if (!finalUrl.isNullOrBlank()) {
                            Log.i("VideoGen", "视频生成成功: $finalUrl")
                            break@loop // 成功拿到 URL，跳出循环
                        }
                    }

                    else -> {
                        Log.w("VideoGen", "未知状态: ${body.status}")
                    }
                }
            }

                // 轮询结束仍然没有
                val url = finalUrl ?: throw Exception("视频生成超时或未返回 URL")

                // 5. 成功 → 覆盖加载消息
                val successMsg = loadingMsg.copy(
                    content = url,
                    isLoading = false,
                    status = MessageStatus.SUCCESS,
                    sessionId = sessionId,
                    type = MessageType.VIDEO,
                    role = MessageRole.AI
                )
                chatDao.insertMessage(successMsg)
                return successMsg

              // 最多轮询 30 次（约 30 秒）

        } catch (e: Exception) {

            // 6. 失败 → 覆盖加载消息
            val failedMsg = loadingMsg.copy(
                content = "视频生成失败：${e.message}",
                isLoading = false,
                status = MessageStatus.FAILURE,
                sessionId = sessionId,
                type = MessageType.VIDEO,
                role = MessageRole.AI
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
    // 将 ChatMessage 转成 API 所需的 Message 格式
    private fun ChatMessage.toApiMessage(): ApiMessage {
        val apiRole = when (role) {
            MessageRole.USER -> "user"
            MessageRole.AI -> "assistant"
            MessageRole.SYSTEM -> "system"
        }
        return ApiMessage(
            role = apiRole,
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


    // -------------------------- 核心工具方法：获取会话历史消息（用于上下文）--------------------------
    /**
     * 获取当前会话的历史消息（按时间升序）
     * 1. 过滤掉加载中/失败的消息（避免干扰上下文）
     * 2. 限制最大条数（避免token超标）
     */
    private suspend fun getHistoryMessagesForContext(sessionId: String): List<ChatMessage> {
        return withContext(Dispatchers.IO) {
            chatDao.getMessagesBySessionId(sessionId) // 按sessionId查询当前会话消息
                .first() // 取当前最新的消息列表（Flow转一次性列表）
                .filter { it.status == MessageStatus.SUCCESS } // 只保留成功的消息
                .sortedBy { it.timestamp } // 按时间升序（最早的消息在前）
                .takeLast(MAX_CONTEXT_MESSAGES) // 限制最大条数（避免token过多）
        }
    }
}

