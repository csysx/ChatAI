package com.example.chatai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.chatai.repository.ChatRepository // <--- 确保导入的是接口

// 这里的参数类型必须是接口 ChatRepository
class ViewModelFactory(private val repository: ChatRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // 这里把 repository 传给 ChatViewModel
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

