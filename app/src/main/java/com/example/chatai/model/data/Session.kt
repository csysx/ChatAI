package com.example.chatai.model.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 会话实体（代表一个独立的聊天会话）
 * @param id 会话唯一ID（UUID）
 * @param title 会话标题（自动提取首条消息，或用户自定义）
 * @param createTime 会话创建时间
 * @param lastMessage 最后一条消息（用于列表预览）
 * @param lastMessageTime 最后一条消息时间
 * @param userId 所属用户ID（预留：后续用户登录后关联）
 */
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    var title: String = "新会话", // 默认标题
    val createTime: Long = System.currentTimeMillis(),
    var lastMessage: String = "",
    var lastMessageTime: Long = System.currentTimeMillis(),
    val userId: String = "default_user" // 预留：默认用户，后续登录后替换
)