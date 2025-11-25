package com.example.chatai

import android.os.Build
import android.os.Bundle
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
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.shadow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity

/**
 * 对话主界面（整合所有组件，连接 ViewModel）
 * @param viewModel 对话 ViewModel（从外部传入，便于测试）
 */
// MainActivity.kt
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel
) {

    // 1. 从 ViewModel 获取 UI 状态（collectAsStateWithLifecycle：页面可见时才收集状态，节省资源）
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    // 获取上下文（用于显示 Toast 提示）
    val context = LocalContext.current

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
                // 导航栏样式（背景色、阴影）
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary, // 导航栏背景（紫色）
                    titleContentColor = MaterialTheme.colorScheme.onPrimary // 标题颜色（白色）
                ),
                modifier = Modifier.shadow(4.dp) // 导航栏阴影（4dp：轻微阴影）
            )
        },
        // 底部内容：输入框（固定在底部）
        bottomBar = {
            ChatInputBar(
                inputText = uiState.value.inputText,
                // 输入文本变化时，发送 UpdateInputText 意图给 ViewModel
                onTextChange = { text ->
                    viewModel.handleIntent(ChatIntent.UpdateInputText(text))
                },
                // 发送按钮点击时，发送 SendMessage 意图给 ViewModel
                onSendClick = {
                    viewModel.handleIntent(ChatIntent.SendMessage(uiState.value.inputText))
                },
//                onImageClick = { viewModel.generateImage(uiState.value.inputText) }, // 新增图片生成
                isLoading = uiState.value.isLoading
            )
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






class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AIChatAppTheme {

//                // 创建 Repository 实例
                val repository = com.example.chatai.repository.RemoteChatRepository(com.example.chatai.repository.RetrofitClient.apiService)
                // 2. 创建 Factory，把 repository 塞进去
                val factory = com.example.chatai.viewmodel.ViewModelFactory(repository)
                // 3. 获取 ViewModel，使用刚才的 factory
                val viewModel: ChatViewModel = viewModel(factory = factory)
                // 4. 传入 ViewModel 给 Screen
                ChatScreen(viewModel = viewModel)
            }
        }
    }
}
