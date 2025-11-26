package com.example.chatai.model.data

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * 对话消息实体（对应一条聊天内容）
 * @param id 消息唯一标识（自动生成，类似数据库主键）
 * @param role 消息发送者角色（用户/AI）
 * @param content 消息内容
 * @param timestamp 发送时间戳（默认当前时间）
 * @param status 消息状态（成功/加载中/失败，默认成功）
 */

enum class MessageType {
    TEXT,  // 文本消息
    IMAGE, // 图像消息
    VIDEO  // 视频消息（预留）
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,       // 文本内容或图像 URL
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SUCCESS,
    val isLoading: Boolean = false // 是否正在加载
)

/**
 * 角色枚举（限定只能是“用户”或“AI”，避免乱输入）
 */
enum class MessageRole {
//    USER,  // 用户发送的消息
//    AI     // AI 发送的消息
    @SerializedName("user")
    USER,
    @SerializedName("assistant")
    AI,
    @SerializedName("system")
    SYSTEM
}

/**
 * 消息状态枚举（管理消息的不同状态，比如“AI正在输入”“发送失败”）
 */
enum class MessageStatus {
    LOADING,  // 加载中（AI正在输入）
    SUCCESS,  // 发送成功
    FAILURE   // 发送失败
}