package com.example.chatai.ui.component
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chatai.model.data.ChatMessage
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.chatai.model.data.MessageType


/**
 * 对话消息列表（显示所有聊天记录）
 * @param messages 要显示的消息列表（从 ViewModel 的 uiState 获取）
 * @param onRetryClick 重试按钮点击事件（传给失败消息气泡）
 */
@Composable
fun ChatMessageList(
    messages: List<ChatMessage>,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // LazyColumn：类似后端的“分页列表”，只加载屏幕内的消息，避免卡顿
    LazyColumn(
        modifier = modifier,
        // 消息之间的间距（12dp：视觉上更舒适）
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        // 内边距（左右16dp，上下8dp：避免消息贴边）
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp
        )
    ) {
        // 遍历消息列表，每个消息生成一个item
        items(messages) { message ->
            // 根据消息角色，显示不同的气泡（用户消息靠右，AI消息靠左）
            when (message.role) {
                com.example.chatai.model.data.MessageRole.USER -> {
                    UserMessageBubble(message = message)
                }
                com.example.chatai.model.data.MessageRole.AI -> {
                    AIMessageBubble(
                        message = message,
                        onRetryClick = onRetryClick
                    )
                }
                else -> {
                    // 对于 SYSTEM 角色或其他未来可能添加的角色，我们不显示任何气泡，
                    // 或者可以选择显示一个日志，或者一个特殊的占位符。
                    // 在你的逻辑中，SYSTEM 消息不会被添加到 UI 列表，所以这里实际上不会执行。
                    Box(modifier = Modifier.height(0.dp)) // 渲染一个高度为0的不可见组件
                }
            }
        }

        // 如果最后一条消息是“加载中”，显示加载动画
        if (messages.lastOrNull()?.status == com.example.chatai.model.data.MessageStatus.LOADING) {
            item {
                LoadingIndicator(modifier = Modifier.fillMaxWidth().padding(8.dp))
            }
        }
    }
}

/**
 * 用户消息气泡（靠右显示，紫色背景）
 */
@Composable
private fun UserMessageBubble(message: ChatMessage) {
    // Row：水平布局（类似 HTML 的 flex 布局）
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        // 水平排列：靠右（用户消息在右边）
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
    ) {
        // Box：容器组件（用于设置背景、圆角）
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary, // 主色背景（紫色）
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 4.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ) // 圆角：右上角小，其他角大（符合聊天气泡习惯）
                )
                .padding(
                    horizontal = 16.dp, // 左右内边距
                    vertical = 12.dp    // 上下内边距
                )
        ) {
            // Text：显示消息内容
            androidx.compose.material3.Text(
                text = message.content,
                color = MaterialTheme.colorScheme.onPrimary, // 主色上的文字色（白色）
                style = MaterialTheme.typography.bodyLarge // 字体样式（16sp）
            )
        }
    }
}

/**
 * AI 消息气泡（靠左显示，灰色背景，带头像）
 */

@Composable
private fun AIMessageBubble(
    message: ChatMessage,
    onRetryClick: () -> Unit
) {
    // Row：水平布局（AI头像 + 消息内容）
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // AI 头像（圆形，青色背景）
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = CircleShape
                )
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "AI 头像",
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // AI 消息内容容器
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
        ) {
            // 首先根据消息状态判断
            when (message.status) {
                com.example.chatai.model.data.MessageStatus.SUCCESS -> {
                    // 如果成功，再根据消息类型显示文本或图像
                    when (message.type) {
                        com.example.chatai.model.data.MessageType.TEXT -> {
                            Text(
                                text = message.content,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        com.example.chatai.model.data.MessageType.IMAGE -> {
                            // 使用 Coil 的 rememberAsyncImagePainter 加载网络图片
                            val painter = rememberAsyncImagePainter(message.content)

                            Box(
                                modifier = Modifier
                                    .size(300.dp) // 设置图片大小
                                    .clip(RoundedCornerShape(8.dp)) // 图片圆角
                            ) {
                                Image(
                                    painter = painter,
                                    contentDescription = "Generated image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop // 填充方式
                                )

                                // 可以添加一个加载中的占位符
                                if (painter.state is AsyncImagePainter.State.Loading) {
                                    Text("Loading image...")
                                }
                            }
                        }
                        MessageType.VIDEO -> {
                            VideoMessageItem( // 新增视频组件
                                message = message
                            )
                        }
                        // 未来可以在这里添加对 其他类型的支持
                        else -> {
                            Text("Unsupported message type")
                        }
                    }
                }
                com.example.chatai.model.data.MessageStatus.FAILURE -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = message.content,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(
                            onClick = onRetryClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "重试",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                com.example.chatai.model.data.MessageStatus.LOADING -> {
                    // 加载状态可以显示一个进度条或文本
                    Text("Thinking...")
                }
            }
        }
    }
}