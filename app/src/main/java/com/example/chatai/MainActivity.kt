package com.example.chatai

import android.os.Build
import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chatai.ui.theme.AIChatAppTheme
import com.example.chatai.viewmodel.ChatViewModel
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.example.chatai.repository.local.AppDatabase
import com.example.chatai.repository.RemoteChatRepository
import com.example.chatai.repository.RetrofitClient
import com.example.chatai.ui.ChatScreen
import com.example.chatai.viewmodel.ViewModelFactory


class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 创建 ViewModelFactory 实例
        val factory = ViewModelFactory(applicationContext)
        // 使用 Factory 创建 ViewModel
        val viewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]

        setContent {
            AIChatAppTheme {
                ChatScreen(viewModel = viewModel)
            }
        }
    }
}
