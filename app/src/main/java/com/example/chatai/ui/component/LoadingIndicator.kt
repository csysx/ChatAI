package com.example.chatai.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.filled.Chat

/**
 * AI 加载中动画（三个呼吸的小圆点）
 */
@Composable
fun LoadingIndicator(modifier: Modifier) {
    // Row：水平布局（AI头像 + 三个圆点）
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // AI 头像（和消息气泡的头像一致）
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
                imageVector = Icons.AutoMirrored.Filled.Chat, // 直接使用 Icons.Filled.Chat
                contentDescription = "AI 头像",
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 三个呼吸的小圆点（用循环生成）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            // 循环生成 3 个圆点
            repeat(3) { index ->
                // Animatable：用于单个圆点的透明度动画
                val alpha = remember { Animatable(0.4f) } // 初始透明度 40%

                // LaunchedEffect：在组件创建时启动动画（类似后端的“初始化方法”）
                LaunchedEffect(index) {
                    // 无限循环动画（呼吸效果：透明→不透明→透明）
                    alpha.animateTo(
                        targetValue = 1f, // 目标透明度 100%
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 600), // 动画时长 600ms
                            repeatMode = RepeatMode.Reverse // 反向重复（透明→不透明→透明）
                        ),
                        initialVelocity = 0f
                    )
                }

                // 单个圆点（圆形，随动画变化透明度）
                Box(
                    modifier = Modifier
                        .size(8.dp) // 圆点大小
                        .clip(CircleShape) // 圆形
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = alpha.value // 透明度随动画变化
                            )
                        )
                )
            }
        }
    }
}