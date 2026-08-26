package com.mdnote.markdownnote.ui.ai

import com.mdnote.markdownnote.ui.theme.AiPurple
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
// 面板不可见时暂停业务 Flow 收集
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * AI 消息气泡背景色（浅紫）
 */
private val AiBubbleBg = Color(0xFFF5F5FF)

/**
 * 用户消息气泡背景色（浅灰）
 */
private val UserBubbleBg = Color(0xFFF0F0F0)

/**
 * AI 对话面板
 * 作为编辑器右侧的侧边面板展示，不占全屏
 *
 * @param viewModel AI 面板 ViewModel
 * @param onClose 关闭面板回调
 * @param onApplyResult 应用 AI 结果到编辑器的回调，参数为 (结果文本, 待执行操作)
 */
@Composable
fun AiPanelScreen(
    viewModel: AiPanelViewModel = hiltViewModel(),
    onClose: () -> Unit,
    onApplyResult: (String, PendingAction?) -> Unit
) {
    // AI 面板状态按宿主生命周期收集
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val currentResult by viewModel.currentResult.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val pendingAction by viewModel.pendingAction.collectAsStateWithLifecycle()
    val isRunning = isLoading || isStreaming

    var inputText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // 当消息列表或流式内容变化时，自动滚动到底部
    LaunchedEffect(messages.size, currentResult) {
        if (messages.isNotEmpty() || currentResult.isNotEmpty()) {
            val targetIndex = if (isStreaming || currentResult.isNotEmpty()) messages.size else messages.size - 1
            if (targetIndex >= 0) {
                listState.scrollToItem(targetIndex)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 400.dp)
            .background(Color.White)
    ) {
        // 顶部标题栏
        AiPanelTopBar(onClose = onClose)

        // 消息列表区域
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
        ) {
            // 渲染已有消息
            items(items = messages, key = { it.timestamp }) { message ->
                MessageBubble(
                    message = message,
                    isOperationMode = mode == AiPanelMode.OPERATION
                )
            }

            // 流式输出中：显示正在生成的 AI 消息
            if (isStreaming && currentResult.isNotEmpty()) {
                item {
                    StreamingBubble(content = currentResult)
                }
            }

            // 加载中（尚未收到流式内容）
            if (isLoading && !isStreaming) {
                item {
                    LoadingBubble()
                }
            }

            // 有待确认操作且 AI 已回复完成：显示应用/拒绝按钮
            if (pendingAction != null && !isStreaming && !isLoading && messages.lastOrNull()?.role == ChatRole.AI) {
                item {
                    ActionButtons(
                        isChatMode = mode == AiPanelMode.CHAT,
                        onApply = {
                            val result = messages.lastOrNull()?.content ?: ""
                            onApplyResult(result, pendingAction)
                            viewModel.applyResult()
                        },
                        onReject = {
                            viewModel.rejectResult()
                        }
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))

        // 底部输入区域
        AiPanelInput(
            inputText = inputText,
            onInputTextChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    viewModel.sendChat(inputText.trim())
                    inputText = ""
                }
            },
            onInterrupt = viewModel::interruptGeneration,
            inputEnabled = !isRunning,
            isRunning = isRunning
        )
    }
}

/**
 * 顶部标题栏
 * "AI 助手" 标题 + 关闭按钮
 */
@Composable
private fun AiPanelTopBar(onClose: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI 助手",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = AiPurple,
                modifier = Modifier.padding(start = 8.dp)
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))
    }
}

/**
 * 消息气泡
 * 用户消息靠右灰底，AI 消息靠左紫底
 */
@Composable
private fun MessageBubble(
    message: ChatMessage,
    isOperationMode: Boolean
) {
    val isUser = message.role == ChatRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    color = if (isUser) UserBubbleBg else AiBubbleBg,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 流式输出气泡
 * 带打字光标效果
 */
@Composable
private fun StreamingBubble(content: String) {
    // 光标闪烁动画
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 530, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    color = AiBubbleBg,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // 打字光标
                Text(
                    text = "|",
                    style = MaterialTheme.typography.bodySmall,
                    color = AiPurple,
                    modifier = Modifier.alpha(cursorAlpha)
                )
            }
        }
    }
}

/**
 * 加载中气泡
 * 显示简单的打字动画提示
 */
@Composable
private fun LoadingBubble() {
    // 点动画
    val infiniteTransition = rememberInfiniteTransition(label = "loading_dots")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = AiBubbleBg,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = "思考中...",
                style = MaterialTheme.typography.bodySmall,
                color = AiPurple,
                modifier = Modifier.alpha(dotAlpha)
            )
        }
    }
}

/**
 * 操作确认按钮
 * 应用 / 拒绝 AI 结果
 */
@Composable
private fun ActionButtons(
    isChatMode: Boolean = false,
    onApply: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onApply,
            modifier = Modifier.padding(end = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = if (isChatMode) "应用到文档" else "应用",
                tint = AiPurple,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isChatMode) "应用到文档" else "应用",
                color = AiPurple,
                style = MaterialTheme.typography.labelMedium
            )
        }
        TextButton(onClick = onReject) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = if (isChatMode) "关闭" else "拒绝",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isChatMode) "关闭" else "拒绝",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/**
 * 底部输入区域
 * 输入框 + 发送按钮
 */
@Composable
private fun AiPanelInput(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onInterrupt: () -> Unit,
    inputEnabled: Boolean,
    isRunning: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputTextChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = "输入消息...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            textStyle = MaterialTheme.typography.bodySmall,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AiPurple.copy(alpha = 0.5f),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                cursorColor = AiPurple
            ),
            maxLines = 4,
            enabled = inputEnabled
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
            onClick = {
                if (isRunning) onInterrupt() else onSend()
            },
            enabled = isRunning || inputText.isNotBlank()
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Close else Icons.AutoMirrored.Filled.Send,
                contentDescription = if (isRunning) "打断" else "发送",
                tint = if (isRunning || inputText.isNotBlank()) AiPurple
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
    }
}
