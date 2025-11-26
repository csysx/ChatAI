// app/src/main/java/com/example/chatai/ui/component/GenerationModeSelector.kt
package com.example.chatai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.example.chatai.model.data.GenerationMode

/**
 * 生成模式下拉选择器
 * @param selectedMode 当前选中的模式
 * @param onModeSelected 模式改变时的回调
 * @param isLoading 是否正在加载中（用于禁用选择器）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerationModeSelector(
    selectedMode: GenerationMode,
    onModeSelected: (GenerationMode) -> Unit,
    isLoading: Boolean
) {
    // 用于控制下拉菜单的显示/隐藏
    var expanded by remember { mutableStateOf(false) }

    // 将 GenerationMode 转换为友好的显示名称
    val displayText = when (selectedMode) {
        GenerationMode.TEXT -> "文本生成"
        GenerationMode.IMAGE -> "图像生成"
        GenerationMode.VIDEO -> "视频生成"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            // 只有在非加载状态下才能展开/收起菜单
            if (!isLoading) expanded = !expanded
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
    ) {
        // 文本输入框样式的选择器
        TextField(
            value = displayText,
            onValueChange = {}, // 不允许用户手动输入，只允许选择
            readOnly = true,
            label = { Text("选择生成内容") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            shape = RoundedCornerShape(20.dp), // 圆角更柔和
            colors = ExposedDropdownMenuDefaults.textFieldColors(
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            enabled = !isLoading,
            modifier = Modifier.menuAnchor() // 关键：将 TextField 标记为菜单的锚点
        )

        // 下拉菜单内容
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false } ,
//            shape = RoundedCornerShape(12.dp),
//            modifier = Modifier
//                .background(MaterialTheme.colorScheme.surface)
//                .shadow(2.dp, RoundedCornerShape(12.dp))
        ) {
            // 遍历所有模式，创建菜单项
            GenerationMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = when (mode) {
                                GenerationMode.TEXT -> "文本生成"
                                GenerationMode.IMAGE -> "图像生成"
                                GenerationMode.VIDEO -> "视频生成"
                            }
                        )
                    },
                    onClick = {
                        onModeSelected(mode) // 切换模式
                        expanded = false // 点击后收起菜单
                    },
                    enabled = !isLoading
                )
            }
        }
    }
}