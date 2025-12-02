package com.example.chatai.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.chatai.repository.local.AppDatabase
import com.example.chatai.repository.RemoteChatRepository
import com.example.chatai.repository.RetrofitClient


//class ViewModelFactory(private val repository: ChatRepository) : ViewModelProvider.Factory {
//
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            // 这里把 repository 传给 ChatViewModel
//            return ChatViewModel(repository) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            // 1. 获取 Database 实例
            val database = AppDatabase.getDatabase(context)
            // 2. 获取 ChatDao 实例
            val chatDao = database.chatDao()
            // 3. 获取 ApiService 实例
            val apiService = RetrofitClient.apiService
            // 4. 创建 Repository 实例，并传入所有依赖
            val repository = RemoteChatRepository(apiService, chatDao,context)
            // 5. 创建并返回 ViewModel 实例
            return ChatViewModel(repository, context ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

