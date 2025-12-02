package com.example.chatai.ui.component

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background

/**
 * 聊天输入框（包含输入文本和发送按钮）
 * @param inputText 输入框中的文本（从 ViewModel 的 uiState 获取）
 * @param onTextChange 输入文本变化时的事件（传给 ViewModel）
 * @param onSendClick 发送按钮点击事件（传给 ViewModel）
 * @param isLoading 是否正在加载（控制发送按钮是否禁用）
 */

@Composable
fun ChatInputBar(
    inputText: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
//    onImageClick: () -> Unit, // 新增图片生成点击事件
    isLoading: Boolean,

    isVideoMode: Boolean = false, // 是否是视频模式
    onImagePickClick: () -> Unit = {} // 点击上传图片的回调
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            BasicTextField(
                value = inputText,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                decorationBox = { innerTextField ->
                    if (inputText.isEmpty()) {
                        Text(
                            text = "请输入消息...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                maxLines = 3,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.width(8.dp))

            // --- 新增：上传图片按钮 (只在视频模式且非加载状态下显示) ---
            if (isVideoMode && !isLoading) {
                IconButton(
                    onClick = onImagePickClick,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Upload Image",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 发送按钮
            IconButton(
                onClick = onSendClick,
                enabled = !isLoading && inputText.isNotBlank(),
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (!isLoading && inputText.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(50)
                    )
            ) {
                Icons.Default.Send?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = "发送消息",
                        tint = if (!isLoading && inputText.isNotBlank()) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

        }
    }
}
