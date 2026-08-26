package com.mdnote.markdownnote.ui.ai

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
// 人格页不可见时暂停业务 Flow 收集
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mdnote.markdownnote.data.ai.soul.SoulManager
import com.mdnote.markdownnote.data.ai.soul.SoulTemplates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SOUL 人格编辑 ViewModel
 */
@HiltViewModel
class SoulEditViewModel @Inject constructor(
    private val soulManager: SoulManager
) : ViewModel() {

    private val _soulContent = MutableStateFlow("")
    val soulContent: StateFlow<String> = _soulContent.asStateFlow()

    private val _selectedTemplate = MutableStateFlow(-1)
    val selectedTemplate: StateFlow<Int> = _selectedTemplate.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    init {
        viewModelScope.launch {
            val content = soulManager.getSoulContent()
            _soulContent.value = content
            // 检查是否匹配某个模板
            val templateIndex = SoulTemplates.templates.indexOfFirst { it.content == content }
            _selectedTemplate.value = templateIndex
        }
    }

    /** 更新人格内容 */
    fun updateContent(content: String) {
        _soulContent.value = content
        _isSaved.value = false
        // 如果手动编辑后内容不再匹配模板，清除选中状态
        val currentIndex = _selectedTemplate.value
        if (currentIndex >= 0 && SoulTemplates.templates.getOrNull(currentIndex)?.content != content) {
            _selectedTemplate.value = -1
        }
    }

    /** 选择模板 */
    fun selectTemplate(index: Int) {
        if (_selectedTemplate.value == index) {
            // 取消选中，清空内容
            _soulContent.value = ""
            _selectedTemplate.value = -1
        } else {
            val template = SoulTemplates.templates[index]
            _soulContent.value = template.content
            _selectedTemplate.value = index
        }
        _isSaved.value = false
    }

    /** 保存人格内容 */
    fun saveContent() {
        viewModelScope.launch {
            soulManager.saveSoulContent(_soulContent.value)
            _isSaved.value = true
        }
    }
}

/**
 * SOUL 人格编辑页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoulEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: SoulEditViewModel = hiltViewModel()
) {
    // 人格编辑状态按宿主生命周期收集
    val soulContent by viewModel.soulContent.collectAsStateWithLifecycle()
    val selectedTemplate by viewModel.selectedTemplate.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "人格设置",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveContent() },
                        enabled = !isSaved
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "保存",
                            tint = if (isSaved) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                   else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 模板选择行
            Text(
                text = "预设模板",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SoulTemplates.templates.forEachIndexed { index, template ->
                    FilterChip(
                        selected = selectedTemplate == index,
                        onClick = { viewModel.selectTemplate(index) },
                        label = {
                            Text(
                                template.name,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // 模板描述
            if (selectedTemplate >= 0) {
                Text(
                    text = SoulTemplates.templates[selectedTemplate].description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 人格内容编辑区
            OutlinedTextField(
                value = soulContent,
                onValueChange = { viewModel.updateContent(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 300.dp)
                    .padding(horizontal = 16.dp),
                placeholder = {
                    Text(
                        "在此输入人格定义，定义 AI 的核心行为准则...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = MaterialTheme.shapes.medium
            )

            // 提示文字
            Text(
                text = "人格定义将作为 AI 的核心行为准则，优先级最高",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
