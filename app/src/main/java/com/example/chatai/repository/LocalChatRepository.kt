package com.example.chatai.repository

import com.example.chatai.model.data.ChatMessage
import com.example.chatai.model.data.MessageRole
import kotlinx.coroutines.delay

/**
 * 本地模拟数据仓库（第一阶段用：不调用真实API，直接返回模拟回复）
 */
class LocalChatRepository() : ChatRepository {
    override suspend fun sendMessage(text: String): ChatMessage {
        // 1. 模拟网络延迟
        delay(1500)

        // 2. 根据用户输入，返回不同的模拟回复
        val aiReplyContent = when {
            text.contains("你好") -> "你好呀！我是你的AI对话助手～有什么可以帮你的？"
            text.contains("名字") -> "我叫即梦AI，是专为这个App打造的智能伙伴～"
            text.contains("功能") -> "我现在能和你聊天啦，后续还会支持更多实用功能哦！"
            text.contains("再见") -> "再见～有需要的话随时来找我聊天呀！"
            else -> "感谢你的消息！我已经收到啦～目前我还在学习中，会努力变得更聪明～"
        }

        // 3. 封装成 ChatMessage 对象返回
        return ChatMessage(
            role = MessageRole.AI,
            content = aiReplyContent
        )
    }

    // --- 这里是新增的代码 ---
    override suspend fun generateImage(prompt: String): String {
        delay(2000)
        return "https://via.placeholder.com/512x512.png?text=AI+Image"
    }
}
