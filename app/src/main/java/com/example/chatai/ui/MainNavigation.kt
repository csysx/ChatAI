package com.example.chatai.ui

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chatai.repository.ApiService
import com.example.chatai.repository.RemoteChatRepository
import com.example.chatai.repository.RemoteSessionRepository
import com.example.chatai.repository.RetrofitClient
import com.example.chatai.repository.local.AppDatabase
import com.example.chatai.viewmodel.ChatViewModel
import com.example.chatai.viewmodel.SessionViewModel
import com.example.chatai.viewmodel.ViewModelFactory

/**
 * 主导航（纯手动创建ViewModel，无任何自动注入，零报错）
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun MainNavigation(
    factory: ViewModelFactory
) {
    val navController = rememberNavController()
    val context = LocalContext.current // 获取当前Context（用于创建数据库和ViewModel）

    // 1. 手动初始化核心依赖（一步到位，无额外配置）
    val (chatViewModel, sessionViewModel) = initViewModels(context)

    NavHost(
        navController = navController,
        startDestination = "session_list" // 启动页：会话列表
    ) {
        // 2. 会话列表页（仅传 SessionViewModel）
        composable("session_list") {
            SessionListScreen(
                sessionViewModel = sessionViewModel,
                onSessionClick = { sessionId ->
                    navController.navigate("chat_screen/$sessionId")
                }
            )
        }

        // 3. 聊天页（传 ChatViewModel + SessionViewModel + 会话ID）
        composable("chat_screen/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            ChatScreen(
                chatViewModel = chatViewModel,
                sessionViewModel = sessionViewModel,
                sessionId = sessionId,
                onBackClick = {
                    // 3. 处理返回事件：弹出当前页面，回到列表
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * 手动初始化 ViewModel 及所有依
 */
private fun initViewModels(context: Context): Pair<ChatViewModel, SessionViewModel> {
    // 步骤1：创建数据库（Room）
    val appDatabase = AppDatabase.getDatabase(context)
    // 步骤2：获取 DAO
    val chatDao = appDatabase.chatDao()
    val sessionDao = appDatabase.sessionDao()
    // 步骤3：创建 API 服务
    val apiService: ApiService = RetrofitClient.apiService
    // 步骤4：创建 Repository
    val chatRepository = RemoteChatRepository(apiService, chatDao,context)
    val sessionRepository = RemoteSessionRepository(sessionDao)
    // 步骤5：创建 ViewModel
    val chatViewModel = ChatViewModel(chatRepository, sessionRepository,context)
    val sessionViewModel = SessionViewModel(sessionRepository)
    // 返回两个 ViewModel（用Pair封装，简洁）
    return Pair(chatViewModel, sessionViewModel)
}