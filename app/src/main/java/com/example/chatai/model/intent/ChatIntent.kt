package com.example.chatai.model.intent

import android.content.Context

/**
 * 用户意图（用户在界面上的所有操作，都封装成 Intent 传给 ViewModel）
 * 好处：集中管理用户行为，避免 UI 直接操作数据，符合分层思想
 */
sealed class ChatIntent {
    // 1. 用户修改输入框文本（参数：新的输入文本）
    data class UpdateInputText(val text: String) : ChatIntent()

    // 2. 用户点击发送按钮（参数：要发送的消息内容）
    data class SendMessage(val text: String) : ChatIntent()

    data class GenerateImage(val prompt: String) : ChatIntent() // 图像生成
    data class GenerateVideo(val prompt: String,  val imagePath: String? = null) : ChatIntent() // 视频生成
    data class DeleteMessage(val messageId: String) : ChatIntent() // 删除单条消息
    object ClearAllMessages : ChatIntent() // 清空所有消息

    // 3. 用户点击重试按钮（重新发送失败的消息）
    object RetryFailedMessage : ChatIntent()

    // 4. 清除错误提示（比如用户关闭错误弹窗）
    object ClearError : ChatIntent()


}