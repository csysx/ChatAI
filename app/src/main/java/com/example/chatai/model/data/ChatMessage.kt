package com.example.chatai.model.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * 对话消息实体（支持文本/图像/视频多模态，适配 Room 存储）
 * @param id 消息唯一标识（UUID 生成，作为数据库主键）
 * @param role 消息发送者角色（用户/AI/系统）
 * @param content 消息内容：文本消息存文本，图像/视频消息存 URL
 * @param type 消息类型（文本/图像/视频）
 * @param status 消息状态（加载中/成功/失败）
 * @param isLoading 是否正在加载（辅助 UI 显示加载动画）
 * @param timestamp 发送时间戳（用于本地排序，默认当前时间）
 * @param sessionId 会话 ID（用于区分不同聊天会话，默认单会话）
 */
@Entity(tableName = "chat_messages") // Room 表名：聊天消息表
data class ChatMessage(
    @PrimaryKey // 主键：使用原有 UUID 作为唯一标识（不自动生成，保持你原有逻辑）
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,       // 复用字段：文本内容 / 图像 URL / 视频 URL
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SUCCESS,
    val isLoading: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(), // 新增：用于本地排序和展示
    val sessionId: String = "default_session" // 新增：会话 ID，支持后续多会话扩展
)

/**
 * 消息类型枚举（支持多模态）
 */
enum class MessageType {
    TEXT,  // 文本消息
    IMAGE, // 图像消息（content 存图像 URL）
    VIDEO  // 视频消息（content 存视频 URL）
}

/**
 * 角色枚举（与 API 字段对齐，支持 system 角色）
 */
enum class MessageRole {
    @SerializedName("user")
    USER,
    @SerializedName("assistant")
    AI,
    @SerializedName("system")
    SYSTEM
}

/**
 * 消息状态枚举（管理加载/成功/失败状态）
 */
enum class MessageStatus {
    LOADING,  // 加载中（AI 生成文本/图像/视频时）
    SUCCESS,  // 生成成功
    FAILURE   // 生成失败
}