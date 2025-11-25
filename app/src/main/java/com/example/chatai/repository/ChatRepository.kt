package com.example.chatai.repository

import com.example.chatai.model.data.ChatMessage

/**
 * 对话数据仓库接口（定义“获取AI回复”的方法，不关心具体实现）
 * 好处：后续切换“本地模拟”到“真实API”时，只需改实现类，不用改调用处（开闭原则）
 */
interface ChatRepository {
    /**
     * 发送消息并获取AI回复
     * @param text 用户发送的消息内容
     * @return AI 回复的消息（带角色、内容等信息）
     */
    suspend fun sendMessage(text: String): ChatMessage
}