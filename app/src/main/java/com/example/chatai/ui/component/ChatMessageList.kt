package com.example.chatai.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState // 1. 引入 ListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect // 2. 引入副作用处理
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.chatai.model.data.ChatMessage
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
    // 3. 创建列表状态，用于控制滚动
    val listState = rememberLazyListState()

    // 4. 监听消息数量变化，自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            // 平滑滚动到最后一条消息
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // LazyColumn：类似后端的“分页列表”，只加载屏幕内的消息，避免卡顿
    LazyColumn(
        modifier = modifier,
        state = listState, // 5. 绑定状态
        // 消息之间的间距（12dp：视觉上更舒适）
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        // 内边距（左右16dp，上下8dp：避免消息贴边）
        contentPadding = PaddingValues(
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
                    // 对于 SYSTEM 角色或其他未来可能添加的角色，我们不显示任何气泡
                    Box(modifier = Modifier.height(0.dp))
                }
            }
        }

        // 如果最后一条消息是“加载中”，显示加载动画
        // 注意：这里通常不需要额外的 LoadingIndicator，因为你在 AIMessageBubble 里也处理了 LOADING 状态
        // 但如果你想在气泡之外再显示一个圈圈，保留这个也没问题。
        if (messages.lastOrNull()?.status == com.example.chatai.model.data.MessageStatus.LOADING) {
            item {
                // 这里的 modifier 参数之前报错是因为没传值，现在显式传入 Modifier
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 4.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
        ) {
            Text(
                text = message.content,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyLarge
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
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
            when (message.status) {
                com.example.chatai.model.data.MessageStatus.SUCCESS -> {
                    when (message.type) {
                        com.example.chatai.model.data.MessageType.TEXT -> {
                            Text(
                                text = message.content,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        com.example.chatai.model.data.MessageType.IMAGE -> {
                            val painter = rememberAsyncImagePainter(message.content)

                            Box(
                                modifier = Modifier
                                    .size(300.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                Image(
                                    painter = painter,
                                    contentDescription = "Generated image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (painter.state is AsyncImagePainter.State.Loading) {
                                    Text("Loading image...", modifier = Modifier.align(Alignment.Center))
                                }
                            }
                        }
                        MessageType.VIDEO -> {
                            VideoMessageItem(
                                message = message
                            )
                        }
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
                    Text("Thinking...")
                }
            }
        }
    }
}
