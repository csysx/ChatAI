package com.example.chatai.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatai.model.data.ChatMessage
import com.example.chatai.model.data.GenerationMode
import com.example.chatai.model.data.MessageRole
import com.example.chatai.model.data.MessageStatus
import com.example.chatai.model.data.MessageType
import com.example.chatai.model.data.Session
import com.example.chatai.model.intent.ChatIntent
import com.example.chatai.model.state.ChatUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.chatai.repository.RemoteChatRepository
import com.example.chatai.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject

/**
 * 对话 ViewModel（核心：接收用户意图，更新 UI 状态，调用仓库获取数据）
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: RemoteChatRepository,
    @ApplicationContext private val context: Context, // Inject Context here
) : ViewModel() {

    private val _generationMode = MutableStateFlow(GenerationMode.TEXT)
    val generationMode: StateFlow<GenerationMode> = _generationMode.asStateFlow()
    // 切换生成模式
    fun setGenerationMode(mode: GenerationMode) {
        _generationMode.value = mode
    }


    // 消息UI状态
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()



    // 当前活跃会话ID
    private var currentSessionId: String = ""
    private val currentUserId: String = "default_user" // 预留登录


    // 初始化：默认加载空消息（会话ID由外部传入）
    init {
        _uiState.update { it.copy(messages = mutableListOf()) }
    }



    // 接收当前活跃会话ID，加载对应消息
    fun loadMessagesForSession(sessionId: String) {
        _uiState.update { it.copy(currentSessionId = sessionId) }
        viewModelScope.launch {
            chatRepository.getMessages(sessionId).collectLatest { messages ->
                _uiState.update { it.copy(messages = messages.toMutableList()) }
            }
        }
    }

//
//    // 加载指定会话的消息
//    private fun loadSessionMessages(sessionId: String) {
//        viewModelScope.launch {
//           chatRepository.getMessages(sessionId).collectLatest { messages ->
//                _uiState.update { it.copy(messages = messages.toMutableList()) }
//            }
//        }
//    }


    /**
     * 处理用户意图（UI 层调用这个方法，传递用户操作）
     * 类似后端的 Controller 接收请求，然后分发给不同的方法处理
     */
    // 统一处理所有UI意图
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            // 消息相关意图
            is ChatIntent.UpdateInputText -> updateInputText(intent.text)
            is ChatIntent.SendMessage -> sendMessage(intent.text,intent.sessionId)
            is ChatIntent.GenerateImage -> generateImage(intent.prompt,intent.sessionId)
            is ChatIntent.GenerateVideo -> generateVideo(intent.prompt,intent.sessionId)
            is ChatIntent.DeleteMessage -> deleteMessage(intent.messageId)
            is ChatIntent.ClearAllMessages -> clearAllMessages(intent.sessionId)
            is ChatIntent.RetryFailedMessage -> retryFailedMessage(intent.sessionId)
            is ChatIntent.ClearError -> clearError()
        }
    }


    // -------------------------- 文本消息 --------------------------
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun sendMessage(text: String, sessionId: String) {
        if (text.isBlank() || sessionId.isBlank()) return
        

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = text,
            sessionId = sessionId
        )

        val loadingMessage = ChatMessage(
            role = MessageRole.AI,
            content = "正在加载中....",
            status = MessageStatus.LOADING,
            sessionId = sessionId
        )

        // 1. 立即更新 UI：添加用户消息和 Loading
        _uiState.update { currentState ->
            val newMessages = currentState.messages.toMutableList().apply {
                add(userMessage)
                add(loadingMessage)
            }

            currentState.copy(
                messages = newMessages,
                inputText = "",
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 注意：这里应该使用参数传入的 sessionId，而不是类变量 currentSessionId，防止上下文不一致
                val aiMessage = chatRepository.sendMessage(text, sessionId)

                // 2. 成功更新：基于【最新的】currentState 移除最后一条(Loading)并添加新消息
                _uiState.update { currentState ->
                    val newMessages = currentState.messages.toMutableList()
                    if (newMessages.isNotEmpty() && newMessages.last().role == MessageRole.AI
                        &&newMessages.last().type==MessageType.TEXT) {
                        newMessages.removeLast() // 移除 Loading
                    }
                    newMessages.add(aiMessage) // 添加 AI 回复

                    currentState.copy(
                        messages = newMessages,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                // 3. 失败更新
                val errorMessage = ChatMessage(
                    role = MessageRole.AI,
                    content = "消息发送失败，请点击重试～",
                    status = MessageStatus.FAILURE,
                    sessionId = sessionId
                )

                _uiState.update { currentState ->
                    val newMessages = currentState.messages.toMutableList()
                    if (newMessages.isNotEmpty() && newMessages.last().role == MessageRole.AI) {
                        newMessages.removeLast()
                    }
                    newMessages.add(errorMessage)

                    currentState.copy(
                        messages = newMessages,
                        isLoading = false,
                        errorMessage = e.message ?: "未知错误",
                    )
                }
            }
        }
    }


    // -------------------------- 图像生成 --------------------------
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun generateImage(prompt: String,sessionId: String) {
        if(prompt.isBlank()|| sessionId.isBlank()) return
        // 1. 添加用户输入的文本消息
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = prompt,
            type = MessageType.TEXT ,// 消息类型：文本
            sessionId = sessionId
        )

        // 2. 添加 AI 正在生成的占位消息
        val loadingMessage = ChatMessage(
            role = MessageRole.AI,
            content = "正在生成图像...",
            type = MessageType.IMAGE, // 消息类型：图像
            isLoading = true,
            status = MessageStatus.LOADING ,// 显式标记状态
            sessionId = sessionId
        )

        _uiState.update { currentState ->
            val newMessages = currentState.messages.toMutableList().apply {
                add(userMessage)
                add(loadingMessage)
            }

            currentState.copy(
                messages = newMessages,
                inputText = "",
                isLoading = true,
                errorMessage = null
            )
        }


        // 3. 发起 API 请求
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val aiMessage = chatRepository.generateImage(prompt,sessionId)


                _uiState.update { currentState ->
                    val newMessages = currentState.messages.toMutableList()
                    if (newMessages.isNotEmpty() && newMessages.last().role == MessageRole.AI
                        &&newMessages.last().type==MessageType.IMAGE) {
                        newMessages.removeLast() // 移除 Loading
                    }
                    newMessages.add(aiMessage) // 添加 AI 回复

                    currentState.copy(
                        messages = newMessages,
                        isLoading = false
                    )
                }

            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    role = MessageRole.AI,
                    content = "图像生成失败: ${e.message}",
                    status = MessageStatus.FAILURE,
                    sessionId = sessionId,
                    type = MessageType.TEXT,
                    isLoading = false,
                )

                _uiState.update { currentState ->
                    val newMessages = currentState.messages.toMutableList()
                    if (newMessages.isNotEmpty() && newMessages.last().role == MessageRole.AI
                        &&newMessages.last().type==MessageType.IMAGE) {
                        newMessages.removeLast()
                    }
                    newMessages.add(errorMessage)

                    currentState.copy(
                        messages = newMessages,
                        isLoading = false,
                        errorMessage = e.message ?: "未知错误",
                    )
                }
            }
        }
    }

    // -------------------------- 视频生成 --------------------------
    // 1. 新增状态：暂存用户选择的图片 URI
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri = _selectedImageUri.asStateFlow()

    // 用户选中图片后调用此方法
    fun setSelectedImage(uri: Uri?) {
        _selectedImageUri.value = uri
    }
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun generateVideo(prompt: String,sessionId: String) {
        if (prompt.isBlank()|| sessionId.isBlank()) return

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = prompt,
            type = MessageType.TEXT,
            status = MessageStatus.SUCCESS,
            sessionId = sessionId
        )

        val loadingMessage = ChatMessage(
            role = MessageRole.AI,
            content = "正在生成视频...",
            type = MessageType.VIDEO,
            isLoading = true,
            status = MessageStatus.LOADING,
            sessionId = sessionId
        )


        _uiState.update { currentState ->
            val newMessages = currentState.messages.toMutableList().apply {
                add(userMessage)
                add(loadingMessage)
            }

            currentState.copy(
                messages = newMessages,
                inputText = "",
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUri = _selectedImageUri.value
                // 如果有选图，将 URI 转为本地缓存文件路径
                val imagePath = currentUri?.let { uri ->
                    uriToFile(this@ChatViewModel.context, uri)?.absolutePath
                }

                // 1. 添加用户提示词消息
                val aiMessage=chatRepository.generateVideo(prompt,imagePath,sessionId) // 仓库已处理加载状态和本地存储


                _uiState.update { currentState ->
                    val newMessages = currentState.messages.toMutableList()
                    if (newMessages.isNotEmpty() && newMessages.last().role == MessageRole.AI
                        &&newMessages.last().type==MessageType.IMAGE) {
                        newMessages.removeLast() // 移除 Loading
                    }
                    newMessages.add(aiMessage) // 添加 AI 回复

                    currentState.copy(
                        messages = newMessages,
                        isLoading = false
                    )
                }


                _selectedImageUri.value = null


            } catch (e: Exception) {

                val errorMessage = ChatMessage(
                    role = MessageRole.AI,
                    content = "视频生成失败: ${e.message ?: "未知错误"}",
                    type = MessageType.TEXT,
                    isLoading = false,
                    status = MessageStatus.FAILURE,
                    sessionId = sessionId
                )

                _uiState.update { currentState ->
                    val newMessages = currentState.messages.toMutableList()
                    if (newMessages.isNotEmpty() && newMessages.last().role == MessageRole.AI) {
                        newMessages.removeLast()
                    }
                    newMessages.add(errorMessage)

                    currentState.copy(
                        messages = newMessages,
                        isLoading = false,
                        errorMessage = e.message ?: "未知错误",
                    )
                }
            }
        }
    }


    // -------------------------- 本地存储操作 --------------------------
    private fun deleteMessage(messageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.deleteMessage(messageId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "删除失败：${e.message ?: "未知错误"}")
                }
            }
        }
    }

    private fun clearAllMessages(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.clearMessages(sessionId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "清空失败：${e.message ?: "未知错误"}")
                }
            }
        }
    }



    // -------------------------- 辅助方法 --------------------------
    private fun updateInputText(text: String) {
        // 用 update 方法修改状态（保证线程安全，类似后端的线程安全集合）
        _uiState.update { currentState ->
            // 复制当前状态，只修改 inputText 字段（不可变对象思想，避免状态混乱）
            currentState.copy(inputText = text)
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun retryFailedMessage(sessionId: String) {
        // 找到最后一条失败的消息，重试对应操作（文本/图像/视频）
        val failedMessage = _uiState.value.messages.lastOrNull { it.status == MessageStatus.FAILURE }
        failedMessage?.let {
            when (it.type) {
                MessageType.TEXT -> sendMessage(it.content,sessionId)
                MessageType.IMAGE -> generateImage(_uiState.value.inputText,sessionId)
                MessageType.VIDEO -> generateVideo(_uiState.value.inputText,sessionId)
            }
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // 辅助方法：将 URI 复制到私有缓存目录变成 File (Android 10+ 必须这样做)
    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "upload_temp.jpg")
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}