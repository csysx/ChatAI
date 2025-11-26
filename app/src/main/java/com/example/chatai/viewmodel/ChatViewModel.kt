package com.example.chatai.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatai.model.data.ChatMessage
import com.example.chatai.model.data.GenerationMode
import com.example.chatai.model.data.MessageRole
import com.example.chatai.model.data.MessageStatus
import com.example.chatai.model.data.MessageType
import com.example.chatai.model.intent.ChatIntent
import com.example.chatai.model.state.ChatUiState
import com.example.chatai.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.chatai.repository.RemoteChatRepository
import com.example.chatai.repository.RetrofitClient
import kotlinx.coroutines.flow.StateFlow

/**
 * 对话 ViewModel（核心：接收用户意图，更新 UI 状态，调用仓库获取数据）
 * @param repository 数据仓库（默认用本地模拟实现，后续可替换）
 */
class ChatViewModel(
//    private val repository: ChatRepository = LocalChatRepository()
    private val repository: ChatRepository = RemoteChatRepository(RetrofitClient.apiService)
) : ViewModel() {


    // 新增：当前生成模式（默认文字）
    private val _generationMode = MutableStateFlow(GenerationMode.TEXT)
    val generationMode: StateFlow<GenerationMode> = _generationMode.asStateFlow()
    // 切换生成模式
    fun setGenerationMode(mode: GenerationMode) {
        _generationMode.value = mode
    }


    // 1. 私有状态流（MutableStateFlow：可修改的状态，只在 ViewModel 内部用）
    // 初始状态：空消息列表、空输入框、未加载、无错误
    private val _uiState = MutableStateFlow(ChatUiState())

    // 2. 公开状态流（StateFlow：不可修改，只给 UI 读取，避免 UI 直接改状态）
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /**
     * 处理用户意图（UI 层调用这个方法，传递用户操作）
     * 类似后端的 Controller 接收请求，然后分发给不同的方法处理
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInputText -> updateInputText(intent.text)
            is ChatIntent.SendMessage -> sendMessage(intent.text)
            ChatIntent.RetryFailedMessage -> retryFailedMessage()
            ChatIntent.ClearError -> clearError()
        }
    }

    /**
     * 1. 处理“更新输入框文本”（用户打字时调用）
     */
    private fun updateInputText(text: String) {
        // 用 update 方法修改状态（保证线程安全，类似后端的线程安全集合）
        _uiState.update { currentState ->
            // 复制当前状态，只修改 inputText 字段（不可变对象思想，避免状态混乱）
            currentState.copy(inputText = text)
        }
    }

    /**
     * 2.1 处理“发送消息”（用户点击发送按钮时调用）
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun sendMessage(text: String) {
        // 防呆：如果输入为空，不处理（避免发送空消息）
        if (text.isBlank()) return

        // 步骤1：立即添加“用户消息”到 UI 状态（让用户看到自己发的消息，提升体验）
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = text
        )
        // 获取当前消息列表，转成可变列表（因为 StateFlow 的数据是不可变的）
        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(userMessage)

        // 步骤2：添加“AI加载中”消息（让用户知道 AI 正在回复）
        val loadingMessage = ChatMessage(
            role = MessageRole.AI,
            content = "",  // 加载中无内容
            status = MessageStatus.LOADING  // 标记为加载中状态
        )
        currentMessages.add(loadingMessage)

        // 步骤3：更新 UI 状态（显示用户消息 + 加载中，清空输入框）
        _uiState.update { currentState ->
            currentState.copy(
                messages = currentMessages,
                inputText = "",  // 发送后清空输入框
                isLoading = true,  // 标记为加载中（禁用发送按钮）
                errorMessage = null  // 清除之前的错误
            )
        }

        // 步骤4：调用仓库获取 AI 回复（异步执行，避免卡 UI）
        // viewModelScope.launch：在 ViewModel 专属的协程中执行（类似后端的线程池）
        // Dispatchers.IO：指定在 IO 线程执行（适合网络/数据库操作，不占用 UI 线程）
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 调用仓库获取 AI 回复（本地模拟，1.5秒后返回）
                val aiMessage = repository.sendMessage(text)

                // 步骤5：更新状态（替换“加载中”为真实 AI 回复）
                val updatedMessages = currentMessages.toMutableList()
                updatedMessages.removeLast()  // 删除“加载中”消息
                updatedMessages.add(aiMessage)  // 添加真实 AI 回复

                // 更新 UI 状态（必须在主线程？不用！StateFlow 会自动切换到主线程）
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = updatedMessages,
                        isLoading = false  // 加载完成，启用发送按钮
                    )
                }
            } catch (e: Exception) {
                // 步骤6：处理异常（比如模拟网络失败，显示错误消息）
                val errorMessage = ChatMessage(
                    role = MessageRole.AI,
                    content = "消息发送失败，请点击重试～",
                    status = MessageStatus.FAILURE  // 标记为失败状态
                )
                val updatedMessages = currentMessages.toMutableList()
                updatedMessages.removeLast()  // 删除“加载中”消息
                updatedMessages.add(errorMessage)  // 添加错误消息

                // 更新 UI 状态（显示错误，结束加载）
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = updatedMessages,
                        isLoading = false,
                        errorMessage = e.message ?: "未知错误"  // 保存错误信息
                    )
                }
            }
        }
    }

    /**
     * 2.2 生成图片（用户点击发送按钮时调用）
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun generateImage(prompt: String) {
        // 1. 添加用户输入的文本消息
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = prompt,
            type = MessageType.TEXT // 消息类型：文本
        )

        // 从 _uiState 获取当前消息列表并转为可变列表
        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(userMessage)

        // 2. 添加 AI 正在生成的占位消息
        val loadingMessage = ChatMessage(
            role = MessageRole.AI,
            content = "正在生成图像...",
            type = MessageType.IMAGE, // 消息类型：图像
            isLoading = true,
            status = MessageStatus.LOADING // 显式标记状态
        )
        currentMessages.add(loadingMessage)

        // 更新 UI 状态：显示新消息，标记加载中
        _uiState.update { currentState ->
            currentState.copy(
                messages = currentMessages,
                isLoading = true
            )
        }

        // 3. 发起 API 请求
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imageUrl = repository.generateImage(prompt)


                // 4. 成功：替换占位消息为实际图像消息
                val updatedMessages = _uiState.value.messages.toMutableList()
                if (updatedMessages.isNotEmpty()) {
                    updatedMessages.removeLast() // 移除“正在生成...”
                }

                val imageMessage = ChatMessage(
                    role = MessageRole.AI,
                    content = imageUrl, // 存储图片 URL
                    type = MessageType.IMAGE,
                    isLoading = false,
                    status = MessageStatus.SUCCESS
                )
                updatedMessages.add(imageMessage)

                // 更新 UI
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = updatedMessages,
                        isLoading = false
                    )
                }

            } catch (e: Exception) {
                // 5. 失败：更新占位消息为错误提示
                val updatedMessages = _uiState.value.messages.toMutableList()
                if (updatedMessages.isNotEmpty()) {
                    updatedMessages.removeLast()
                }

                val errorMessage = ChatMessage(
                    role = MessageRole.AI,
                    content = "图像生成失败: ${e.message}",
                    type = MessageType.TEXT,
                    isLoading = false,
                    status = MessageStatus.FAILURE
                )
                updatedMessages.add(errorMessage)

                _uiState.update { currentState ->
                    currentState.copy(
                        messages = updatedMessages,
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }



    /**
     * 3. 处理“重试失败消息”（用户点击错误消息的重试按钮时调用）
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun retryFailedMessage() {
        // 获取当前消息列表
        val currentMessages = _uiState.value.messages.toMutableList()
        // 找到最后一条消息（如果是失败状态）
        val lastMessage = currentMessages.lastOrNull() ?: return
        if (lastMessage.status == MessageStatus.FAILURE) {
            // 找到上一条用户消息（重新发送这条消息）
            val userMessage = currentMessages.findLast { it.role == MessageRole.USER } ?: return
            // 调用 sendMessage 重新发送（复用已有的发送逻辑）
            sendMessage(userMessage.content)
        }
    }

    /**
     * 4. 处理“清除错误”（错误提示显示后，自动清除）
     */
    private fun clearError() {
        _uiState.update { currentState ->
            currentState.copy(errorMessage = null)
        }
    }
}