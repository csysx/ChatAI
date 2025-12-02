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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chatai.model.intent.ChatIntent
import com.example.chatai.ui.component.ChatInputBar
import com.example.chatai.ui.component.ChatMessageList
import com.example.chatai.ui.theme.AIChatAppTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.chatai.ui.component.GenerationModeSelector

/**
 * 对话主界面（整合所有组件，连接 ViewModel）
 * @param viewModel 对话 ViewModel（从外部传入，便于测试）
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel() // 自动创建 ViewModel（Android 提供的便捷方法）
) {

//    val generationMode = viewModel.generationMode.collectAsState().value
    val generationMode by viewModel.generationMode.collectAsStateWithLifecycle()

    // 从 ViewModel 获取 UI 状态（collectAsStateWithLifecycle：页面可见时才收集状态，节省资源）
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    // --- 新增：获取选中的图片 URI ---
    val selectedImageUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()

    // 获取上下文（用于显示 Toast 提示）
    val context = LocalContext.current

    // --- 新增：图片选择器 Launcher ---
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.setSelectedImage(uri) // 将选中的图片存入 ViewModel
    }

    // 2. 错误提示：当有错误信息时，显示 Toast（3秒后自动清除）
    LaunchedEffect(uiState.value.errorMessage) {
        uiState.value.errorMessage?.let { errorMsg ->
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            // 3秒后清除错误信息（调用 ViewModel 的清除错误方法）
            kotlinx.coroutines.delay(3000)
            viewModel.handleIntent(ChatIntent.ClearError)
        }
    }

    // 3. Scaffold：Android 提供的标准界面框架（包含顶部导航栏、底部内容）
    Scaffold(
        // 顶部导航栏
        topBar = {
            TopAppBar(
                title = {
                    // 导航栏标题
                    Text(
                        text = "AI 对话助手",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                actions = {
                    // 添加清除按钮
                    IconButton(onClick = {
                        viewModel.handleIntent(ChatIntent.ClearAllMessages)
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
                    onModeSelected = viewModel::setGenerationMode,
                    isLoading = uiState.value.isLoading
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
                            onClick = { viewModel.setSelectedImage(null) },
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
                    inputText = uiState.value.inputText,
                    onTextChange = { text ->
                        viewModel.handleIntent(ChatIntent.UpdateInputText(text))
                    },
                    onSendClick = {
                        // 根据当前选中的模式执行不同操作
                        when (generationMode) {
                            GenerationMode.TEXT ->
                                viewModel.handleIntent(ChatIntent.SendMessage(uiState.value.inputText))
                            GenerationMode.IMAGE ->
                                viewModel.handleIntent(ChatIntent.GenerateImage(uiState.value.inputText))
                            GenerationMode.VIDEO ->
                                viewModel.handleIntent(ChatIntent.GenerateVideo(uiState.value.inputText,selectedImageUri?.toString()))
                        }
                    },
                    isLoading = uiState.value.isLoading,
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
            messages = uiState.value.messages,
            // 重试按钮点击时，发送 RetryFailedMessage 意图给 ViewModel
            onRetryClick = {
                viewModel.handleIntent(ChatIntent.RetryFailedMessage)
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
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Preview(showBackground = true, name = "浅色模式预览")
@Composable
fun ChatScreenLightPreview() {
    AIChatAppTheme(darkTheme = false) {
        ChatScreen()
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Preview(showBackground = true, name = "深色模式预览")
@Composable
fun ChatScreenDarkPreview() {
    AIChatAppTheme(darkTheme = true) {
        ChatScreen()
    }
}