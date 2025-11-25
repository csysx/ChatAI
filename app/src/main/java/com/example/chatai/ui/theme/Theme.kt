package com.example.chatai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.dp


// ==================== 1. 定义颜色常量（主题色的具体值）====================
// 主色（紫色：导航栏、发送按钮）
val Purple500 = Color(0xFF6200EE)
val Purple800 = Color(0xFF6200EE) // 深色模式主色（和浅色一致，简化处理）
// 辅助色（青色：AI头像背景）
val Teal200 = Color(0xFF03DAC6)
val Teal500 = Color(0xFF0288D1)
// 背景色（浅色/深色）
val Gray100 = Color(0xFFF5F5F5) // 浅色模式背景
val Gray900 = Color(0xFF121212) // 深色模式背景
// 表面色（输入框、消息气泡背景）
val Gray200 = Color(0xFFEEEEEE) // 浅色模式表面
val Gray800 = Color(0xFF1E1E1E) // 深色模式表面
val Gray700 = Color(0xFF333333) // 深色模式消息气泡
// 文字色（白色/黑色）
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
// 错误色（红色：失败消息）
val Red500 = Color(0xFFFF5252)
val Red600 = Color(0xFFE53935)

// ==================== 2. 定义颜色方案（浅色/深色模式）====================
// 深色模式颜色方案（手机开启深色模式时用）
private val DarkColorScheme = darkColorScheme(
    primary = Purple800,          // 主色（导航栏、发送按钮）
    secondary = Teal200,          // 辅助色（AI头像）
    background = Gray900,         // 整个界面背景
    surface = Gray800,            // 输入框背景
    surfaceVariant = Gray700,     // AI消息气泡背景
    onPrimary = White,            // 主色上的文字（如导航栏标题）
    onSecondary = Black,          // 辅助色上的文字（如AI头像图标）
    onBackground = White,         // 背景上的文字
    onSurface = White,            // 输入框提示文字
    onSurfaceVariant = White,     // 消息气泡文字
    error = Red500                // 错误提示文字
)

// 浅色模式颜色方案（手机默认模式时用）
private val LightColorScheme = lightColorScheme(
    primary = Purple500,          // 主色（导航栏、发送按钮）
    secondary = Teal500,          // 辅助色（AI头像）
    background = Gray100,         // 整个界面背景
    surface = White,              // 输入框背景
    surfaceVariant = Gray200,     // AI消息气泡背景
    onPrimary = White,            // 主色上的文字（如导航栏标题）
    onSecondary = White,          // 辅助色上的文字（如AI头像图标）
    onBackground = Black,         // 背景上的文字
    onSurface = Black,            // 输入框提示文字
    onSurfaceVariant = Black,     // 消息气泡文字
    error = Red600                // 错误提示文字
)

// ==================== 3. 主题入口（对外提供 MaterialTheme）====================
/**
 * 整个App的主题入口：所有界面都要包裹在这个函数内，才能使用 MaterialTheme
 * @param darkTheme 是否为深色模式（自动判断手机系统设置）
 * @param content 要显示的界面内容（比如 ChatScreen）
 */
@Composable
fun AIChatAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),  // 自动跟随系统深色模式
    content: @Composable () -> Unit              // 界面内容（类似“插槽”）
) {
    // 根据系统模式选择颜色方案
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // 👇 MaterialTheme 是 Compose 库提供的，这里通过配置“注入”颜色、字体等
    MaterialTheme(
        colorScheme = colorScheme,  // 传入我们定义的颜色方案
        typography = AppTypography,    // 传入我们定义的字体（下面定义）
        shapes = Shapes,            // 传入我们定义的形状（下面定义）
        content = content           // 显示界面内容
    )
}

// ==================== 4. 定义字体样式（统一App内文字大小）====================
val AppTypography = Typography(
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 14.sp,
        color = Color.Gray.copy(alpha = 0.6f)
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
)

// ==================== 5. 定义形状（统一圆角等样式）====================
val Shapes = Shapes(
    // 默认的小圆角
    small = RoundedCornerShape(8.dp),
    // 中等圆角，用于消息气泡
    medium = RoundedCornerShape(16.dp),
    // 大圆角，用于输入框
    large = RoundedCornerShape(24.dp)
)