package com.example.chatai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chatai.model.data.Session
import com.example.chatai.model.intent.SessionIntent
import com.example.chatai.viewmodel.ChatViewModel
import com.example.chatai.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 会话列表页（仅依赖SessionViewModel，不涉及任何消息逻辑）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    sessionViewModel: SessionViewModel,
    onSessionClick: (String) -> Unit // 点击会话跳转到聊天页
) {
    val uiState by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.CHINA) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("聊天会话") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { sessionViewModel.handleIntent(SessionIntent.CreateNewSession()) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建会话")
            }
        }
    ) { innerPadding ->
        // 错误提示
        uiState.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier.padding(innerPadding),
                action = { Button(onClick = { /* 清空错误 */ }) { Text("关闭") } }
            ) { Text(error) }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.sessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无会话，点击右下角+创建新会话")
                    }
                }
            } else {
                items(uiState.sessions, key = { it.id }) { session ->
                    SessionItem(
                        session = session,
                        dateFormat = dateFormat,
                        isSelected = session.id == uiState.selectedSessionId,
                        onClick = {
                            sessionViewModel.handleIntent(SessionIntent.SwitchSession(session.id))
                            onSessionClick(session.id)
                        },
                        onDelete = {
                            sessionViewModel.handleIntent(SessionIntent.DeleteSession(session.id))
                        }
                    )
                }
            }
        }
    }
}

/**
 * 单个会话项（仅依赖会话数据）
 */
@Composable
private fun SessionItem(
    session: Session,
    dateFormat: SimpleDateFormat,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = if (isSelected) CardDefaults.cardElevation(8.dp) else CardDefaults.cardElevation(2.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = session.lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(session.lastMessageTime)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }
            IconButton(
                onClick = { onDelete() },
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = "删除会话")
            }
        }
    }
}