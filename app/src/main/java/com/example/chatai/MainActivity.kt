package com.example.chatai

import android.os.Build
import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chatai.ui.theme.AIChatAppTheme
import com.example.chatai.viewmodel.ChatViewModel
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.activity.ComponentActivity
import com.example.chatai.ui.ChatScreen



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
