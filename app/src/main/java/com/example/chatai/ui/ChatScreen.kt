package com.example.chatai.ui

import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chatai.model.intent.ChatIntent
import com.example.chatai.ui.component.ChatInputBar
import com.example.chatai.ui.component.ChatMessageList
import com.example.chatai.viewmodel.ChatViewModel
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import com.example.chatai.model.data.GenerationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.chatai.model.state.ChatUiState
import com.example.chatai.ui.component.GenerationModeSelector
import com.example.chatai.viewmodel.SessionViewModel
import kotlinx.coroutines.launch




/**
 * 对话主界面（整合所有组件，连接 ViewModel）
 * @param chatViewModel 对话 ViewModel（从外部传入，便于测试）
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel = viewModel(), // 自动创建 ViewModel（Android 提供的便捷方法）
    sessionViewModel: SessionViewModel,
    sessionId: String,
    onBackClick: () -> Unit
) {

//    val generationMode = viewModel.generationMode.collectAsState().value
    val generationMode by chatViewModel.generationMode.collectAsStateWithLifecycle()

    // 从 ViewModel 获取 UI 状态（collectAsStateWithLifecycle：页面可见时才收集状态，节省资源）
    val chatUiState = chatViewModel.uiState.collectAsStateWithLifecycle()
    val sessionUiState by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()


    // --- 新增：获取选中的图片 URI ---
    val selectedImageUri by chatViewModel.selectedImageUri.collectAsStateWithLifecycle()

    // 获取上下文（用于显示 Toast 提示）
    val context = LocalContext.current

    // --- 新增：图片选择器 Launcher ---
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        chatViewModel.setSelectedImage(uri) // 将选中的图片存入 ViewModel
    }


    // 初始化：加载当前会话的消息
    LaunchedEffect(sessionId) {
        chatViewModel.loadMessagesForSession(sessionId)
    }


    // 消息发送后滚动到底部
    LaunchedEffect(chatUiState.value.messages.size) {
        if (chatUiState.value.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.scrollToItem(chatUiState.value.messages.size - 1)
            }
        }
    }

    // 错误提示：当有错误信息时，显示 Toast（3秒后自动清除）
    LaunchedEffect(chatUiState.value.errorMessage) {
        chatUiState.value.errorMessage?.let { errorMsg ->
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            // 3秒后清除错误信息（调用 ViewModel 的清除错误方法）
            kotlinx.coroutines.delay(3000)
            chatViewModel.handleIntent(ChatIntent.ClearError)
        }
    }

    // 3. Scaffold：Android 提供的标准界面框架（包含顶部导航栏、底部内容）
    Scaffold(
        // 顶部导航栏
        topBar = {
            TopAppBar(
                title = {
                    // 显示当前会话标题
                    val currentSession = sessionUiState.sessions.find { it.id == sessionId }
                    Text(currentSession?.title ?: "新会话")

//                    Text(
//                        text = "AI 对话助手",
//                        style = MaterialTheme.typography.titleLarge,
//                        color = MaterialTheme.colorScheme.onPrimary
//                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    // 添加清除按钮
                    IconButton(onClick = {
                        chatViewModel.handleIntent(ChatIntent.ClearAllMessages(sessionId))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Chat History"
                        )
                    }
                },
                // 导航栏样式（背景色、阴影）
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary, // 导航栏背景（紫色）
                    titleContentColor = MaterialTheme.colorScheme.onPrimary // 标题颜色（白色）
                ),
//                elevation = TopAppBarDefaults.topAppBarElevation(4.dp) // 导航栏阴影（4dp：轻微阴影）
            )
        },
        // 底部内容：输入框（固定在底部）
        bottomBar = {
            // 将功能栏和输入框组合成一个垂直布局
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                // 1. 新增的下拉选择器
                GenerationModeSelector(
                    selectedMode = generationMode,
                    onModeSelected = chatViewModel::setGenerationMode,
                    isLoading = chatUiState.value.isLoading
                )

                if (selectedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        // 删除图片的按钮
                        IconButton(
                            onClick = { chatViewModel.setSelectedImage(null) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(20.dp)
                                .background(
                                    MaterialTheme.colorScheme.error,
                                    androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove Image",
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }
                }


                // 2. 原有的输入框
                ChatInputBar(
                    inputText = chatUiState.value.inputText,
                    onTextChange = { text ->
                        chatViewModel.handleIntent(ChatIntent.UpdateInputText(text,sessionId))
                    },
                    onSendClick = {
                        // 根据当前选中的模式执行不同操作
                        when (generationMode) {
                            GenerationMode.TEXT ->
                                chatViewModel.handleIntent(ChatIntent.SendMessage(chatUiState.value.inputText,sessionId))
                            GenerationMode.IMAGE ->
                                chatViewModel.handleIntent(ChatIntent.GenerateImage(chatUiState.value.inputText,sessionId))
                            GenerationMode.VIDEO ->
                                chatViewModel.handleIntent(ChatIntent.GenerateVideo(chatUiState.value.inputText,selectedImageUri?.toString(),sessionId))
                        }
                    },
                    isLoading = chatUiState.value.isLoading,
                    isVideoMode = (generationMode == GenerationMode.VIDEO),
                    // 传递点击图片的事件
                    onImagePickClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
        },
        // 主内容区域背景色
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        // 主内容：消息列表（innerPadding：避免内容被导航栏/输入框遮挡）
        ChatMessageList(
            messages = chatUiState.value.messages,
            // 重试按钮点击时，发送 RetryFailedMessage 意图给 ViewModel
            onRetryClick = {
                chatViewModel.handleIntent(ChatIntent.RetryFailedMessage(sessionId))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // 应用内边距（关键！避免消息被遮挡）
        )

    }
}




/**
 * 预览函数（Android Studio 专用，不用运行就能看到界面效果）
 */
//@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
//@Preview(showBackground = true, name = "浅色模式预览")
//@Composable
//fun ChatScreenLightPreview() {
//    AIChatAppTheme(darkTheme = false) {
//        ChatScreen()
//    }
//}
//
//@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
//@Preview(showBackground = true, name = "深色模式预览")
//@Composable
//fun ChatScreenDarkPreview() {
//    AIChatAppTheme(darkTheme = true) {
//        ChatScreen()
//    }
//}