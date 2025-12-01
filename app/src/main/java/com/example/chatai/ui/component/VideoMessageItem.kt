package com.example.chatai.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.chatai.model.data.ChatMessage
import com.example.chatai.model.data.MessageStatus
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView

/**
 * 视频消息渲染组件：支持加载中、播放、失败状态
 */
@Composable
fun VideoMessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 视频消息容器（圆角卡片）
    Card(
        modifier = modifier
            .fillMaxWidth(0.8f) // 视频宽度占屏幕 80%
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        when (message.status) {
            // 1. 加载中状态：显示加载动画 + 提示文本
            MessageStatus.LOADING -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp), // 固定加载框高度
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(modifier = Modifier.size(48.dp))
                    Text(
                        text = "视频生成中...",
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 2. 成功状态：显示视频播放器
            MessageStatus.SUCCESS -> {
                // 使用 ExoPlayer 播放视频（Android 官方推荐播放器）
                val exoPlayer = remember {
                    ExoPlayer.Builder(context)
                        .build()
                        .apply {
                            // 设置视频数据源（从 URL 加载）
                            val mediaItem = MediaItem.fromUri(message.content)
                            setMediaItem(mediaItem)
                            prepare() // 预加载视频
                        }
                }

                // 生命周期管理：组件销毁时释放播放器
                androidx.compose.runtime.DisposableEffect(Unit) {
                    onDispose {
                        exoPlayer.release()
                    }
                }

                // AndroidView：将原生 ExoPlayer 的 PlayerView 嵌入 Compose
                AndroidView(
                    factory = { PlayerView(it).apply { player = exoPlayer } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp), // 固定视频高度（可根据屏幕适配）
                    update = { it.player = exoPlayer }
                )
            }

            // 3. 失败状态：显示错误图标 + 错误信息
            MessageStatus.FAILURE -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "视频生成失败",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = message.content ?: "视频生成失败",
                        modifier = Modifier.padding(top = 8.dp),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}