package com.mdnote.markdownnote.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
// 设置页不可见时暂停业务 Flow 收集
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mdnote.markdownnote.BuildConfig
import com.mdnote.markdownnote.R
import com.mdnote.markdownnote.data.ai.AiProviderPreset
import com.mdnote.markdownnote.data.ai.TemperatureStyle
import com.mdnote.markdownnote.ui.components.glass.GlassDialog
import com.mdnote.markdownnote.ui.components.glass.GlassSheet
import com.mdnote.markdownnote.ui.theme.LocalFontSize
import com.mdnote.markdownnote.util.AppSettings
import com.mdnote.markdownnote.util.EditorFontSize
import java.util.Locale

/** 设置页内部子页。 */
enum class SettingsSubPage { MAIN, AI_CONFIG, OPEN_SOURCE_LICENSES }

internal fun settingsSheetTitle(subPage: SettingsSubPage): String = when (subPage) {
    SettingsSubPage.MAIN -> "设置"
    SettingsSubPage.AI_CONFIG -> "AI 助手"
    SettingsSubPage.OPEN_SOURCE_LICENSES -> "开源许可"
}

/**
 * 设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheetContent(
    onDismiss: () -> Unit,
    initialSubPage: SettingsSubPage = SettingsSubPage.MAIN,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // 保留原初始值，同时按宿主生命周期收集设置
    val settings by viewModel.settings.collectAsStateWithLifecycle(
        initialValue = AppSettings()
    )
    val context = LocalContext.current
    var subPage by remember { mutableStateOf(initialSubPage) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }

    GlassSheet(
        onDismissRequest = onDismiss,
        title = settingsSheetTitle(subPage),
        onClose = onDismiss
    ) {
        when (subPage) {
            SettingsSubPage.MAIN -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 编辑器设置
                    SettingsSection(title = "编辑器") {
                        SettingsItem(
                            title = "字号",
                            subtitle = settings.editorFontSize.displayName,
                            onClick = { showFontSizeDialog = true }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // 显示模式
                    SettingsSection(title = "显示") {
                        SettingsSwitch(
                            title = "打字机模式",
                            subtitle = "当前编辑行居中显示",
                            checked = settings.typewriterMode,
                            onCheckedChange = { viewModel.toggleTypewriterMode() }
                        )

                        SettingsSwitch(
                            title = "聚焦模式",
                            subtitle = "淡化其他段落",
                            checked = settings.focusMode,
                            onCheckedChange = { viewModel.toggleFocusMode() }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // 主题设置
                    SettingsSection(title = "外观") {
                        SettingsSwitch(
                            title = "跟随系统主题",
                            subtitle = "自动切换深色/浅色模式",
                            checked = settings.followSystemTheme,
                            onCheckedChange = { viewModel.setFollowSystemTheme(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // 数据管理
                    SettingsSection(title = "数据") {
                        SettingsItem(
                            title = "清除已完成待办",
                            subtitle = "删除所有已完成的待办事项",
                            onClick = { showClearDialog = true }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsSection(title = "AI 助手") {
                        SettingsItem(
                            title = "AI 助手",
                            subtitle = if (settings.aiEnabled) "已启用" else "未启用",
                            onClick = { subPage = SettingsSubPage.AI_CONFIG }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // 关于
                    SettingsSection(title = "关于") {
                        SettingsItem(
                            title = "版本",
                            subtitle = BuildConfig.VERSION_NAME,
                            showArrow = false
                        )
                        SettingsItem(
                            title = "开源许可",
                            subtitle = "查看第三方组件和许可证",
                            onClick = { subPage = SettingsSubPage.OPEN_SOURCE_LICENSES }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            SettingsSubPage.AI_CONFIG -> {
                AiConfigSheetContent(
                    settings = settings,
                    viewModel = viewModel,
                    onBack = { subPage = SettingsSubPage.MAIN }
                )
            }

            SettingsSubPage.OPEN_SOURCE_LICENSES -> {
                OpenSourceLicensesContent(onBack = { subPage = SettingsSubPage.MAIN })
            }
        }
    }

    // 字号选择对话框
    if (showFontSizeDialog) {
        FontSizeSelectionDialog(
            currentSize = settings.editorFontSize,
            onDismiss = { showFontSizeDialog = false },
            onSelect = { size ->
                viewModel.setEditorFontSize(size)
                showFontSizeDialog = false
            }
        )
    }

    // 清除确认对话框
    if (showClearDialog) {
        GlassDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除已完成待办") },
            text = { Text("确定要删除所有已完成的待办事项吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCompletedTodos()
                        showClearDialog = false
                        Toast.makeText(context, "已清除", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("清除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun OpenSourceLicensesContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val notices = remember(context) {
        context.resources.openRawResource(R.raw.third_party_notices)
            .bufferedReader()
            .use { it.readText() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("返回设置")
        }
        SelectionContainer {
            Text(
                text = notices,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
/**
 * AI 配置子页：服务商卡片 → 模型 → 密钥 → 测试 → 回答风格 → 高级 → 记忆
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiConfigSheetContent(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val fontSize = LocalFontSize.current
    var apiKeyVisible by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showPlanDialog by remember { mutableStateOf(false) }
    var advancedExpanded by remember { mutableStateOf(false) }
    // 连接测试状态按宿主生命周期收集
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val currentPreset = remember(settings.aiProviderPresetId) {
        viewModel.aiProviderPresets.byId(settings.aiProviderPresetId)
    }
    val currentPlan = remember(settings.aiProviderPresetId, settings.aiPlanId) {
        currentPreset.plans.firstOrNull { it.id == settings.aiPlanId }
            ?: currentPreset.plans.firstOrNull()
            ?: currentPreset.plans.first()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 返回设置
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBack() }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "返回设置",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize.sp)
            )
        }

        // 启用 AI 助手
        SettingsSwitch(
            title = "启用 AI 助手",
            checked = settings.aiEnabled,
            onCheckedChange = { viewModel.setAiEnabled(it) }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // 选择服务商
        SettingsSection(title = "选择服务商") {
            ProviderGrid(
                presets = viewModel.aiProviderPresets.all,
                currentId = settings.aiProviderPresetId,
                onSelect = { viewModel.setProvider(it) }
            )
        }
        if (currentPreset.plans.size > 1) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsSection(title = "选择套餐") {
                SettingsItem(
                    title = "套餐",
                    subtitle = currentPlan.displayName,
                    onClick = { showPlanDialog = true }
                )
            }
        }

        // 选择模型（当前套餐有模型时显示）
        if (currentPlan.models.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            val currentModel = currentPlan.models.find { it.id == settings.aiModel }
            SettingsSection(title = "选择模型") {
                SettingsItem(
                    title = "模型",
                    subtitle = currentModel?.displayName ?: "请选择",
                    onClick = { showModelDialog = true }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // API 密钥
        SettingsSection(title = "API 密钥") {
            var apiKeyField by remember { mutableStateOf(TextFieldValue(settings.aiApiKey)) }
            LaunchedEffect(settings.aiApiKey) {
                if (settings.aiApiKey != apiKeyField.text) {
                    apiKeyField = TextFieldValue(settings.aiApiKey)
                }
            }
            SettingsPasswordField(
                title = "密钥",
                value = apiKeyField,
                visible = apiKeyVisible,
                onVisibleChange = { apiKeyVisible = it },
                onValueChange = {
                    apiKeyField = it
                }
            )
            FilledTonalButton(
                onClick = {
                    viewModel.setAiApiKey(apiKeyField.text)
                    Toast.makeText(context, "密钥已保存", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Text("保存密钥")
            }

            if (currentPlan.apiKeyApplyUrl.isNotBlank()) {
                SettingsItem(
                    title = "如何获取 API 密钥？",
                    showArrow = false,
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(currentPlan.apiKeyApplyUrl))
                            )
                        }
                    }
                )
            }

            TestConnectionButton(
                state = connectionState,
                onClick = { viewModel.testConnection() }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // 回答风格
        SettingsSection(title = "回答风格") {
            TemperatureStyleSelector(
                current = TemperatureStyle.fromTemperature(settings.aiTemperature),
                onSelect = { viewModel.setTemperatureStyle(it) }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // 高级设置（可折叠）
        SettingsItem(
            title = "高级设置",
            subtitle = if (advancedExpanded) "点击收起" else "点击展开",
            onClick = { advancedExpanded = !advancedExpanded }
        )
        if (advancedExpanded) {
            AdvancedSettings(settings = settings, viewModel = viewModel)
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // 更多
        SettingsSection(title = "更多") {
            SettingsSwitch(
                title = "记忆系统",
                subtitle = "让 AI 记住你的对话和笔记",
                checked = settings.aiMemoryEnabled,
                onCheckedChange = { viewModel.setAiMemoryEnabled(it) }
            )

            SettingsSwitch(
                title = "自动整理记忆",
                subtitle = "空闲时后台梳理，让 AI 更懂你",
                checked = settings.aiDreamEnabled,
                onCheckedChange = { viewModel.setAiDreamEnabled(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
    if (showPlanDialog) {
        GlassDialog(
            onDismissRequest = { showPlanDialog = false },
            title = { Text("选择套餐") },
            text = {
                Column {
                    currentPreset.plans.forEach { plan ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = plan.id == settings.aiPlanId,
                                    onClick = {
                                        viewModel.setPlan(plan)
                                        showPlanDialog = false
                                    }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = plan.id == settings.aiPlanId,
                                onClick = {
                                    viewModel.setPlan(plan)
                                    showPlanDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = plan.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlanDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 模型选择对话框
    if (showModelDialog) {
        GlassDialog(
            onDismissRequest = { showModelDialog = false },
            title = { Text("选择模型") },
            text = {
                Column {
                    currentPlan.models.forEach { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = model.id == settings.aiModel,
                                    onClick = {
                                        viewModel.setModel(model)
                                        showModelDialog = false
                                    }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = model.id == settings.aiModel,
                                onClick = {
                                    viewModel.setModel(model)
                                    showModelDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = model.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = model.id,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/** 服务商卡片网格 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderGrid(
    presets: List<AiProviderPreset>,
    currentId: String,
    onSelect: (AiProviderPreset) -> Unit
) {
    val fontSize = LocalFontSize.current
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { preset ->
            val selected = preset.id == currentId
            Surface(
                onClick = { onSelect(preset) },
                shape = RoundedCornerShape(10.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                border = if (selected) androidx.compose.foundation.BorderStroke(
                    2.dp, MaterialTheme.colorScheme.primary
                ) else null
            ) {
                Text(
                    text = preset.displayName,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = fontSize.sp),
                    color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 回答风格三选一 */
@Composable
private fun TemperatureStyleSelector(
    current: TemperatureStyle,
    onSelect: (TemperatureStyle) -> Unit
) {
    val fontSize = LocalFontSize.current
    TemperatureStyle.entries.forEach { style ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = style == current,
                    onClick = { onSelect(style) }
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = style == current,
                onClick = { onSelect(style) }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = style.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize.sp)
            )
        }
    }
}

/** 测试连接按钮与状态反馈 */
@Composable
private fun TestConnectionButton(
    state: ConnectionState,
    onClick: () -> Unit
) {
    val fontSize = LocalFontSize.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        FilledTonalButton(
            onClick = onClick,
            enabled = state !is ConnectionState.Testing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state is ConnectionState.Testing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("测试中...")
            } else {
                Text("测试连接")
            }
        }
        when (state) {
            is ConnectionState.Success -> Text(
                text = "✓ ${state.message}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = fontSize.sp),
                modifier = Modifier.padding(top = 6.dp)
            )
            is ConnectionState.Failed -> Text(
                text = "✗ ${state.message}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = fontSize.sp),
                modifier = Modifier.padding(top = 6.dp)
            )
            else -> {}
        }
    }
}

/** 高级设置：手动覆盖 baseUrl / 模型 / 输出长度 / 温度 */
@Composable
private fun AdvancedSettings(
    settings: AppSettings,
    viewModel: SettingsViewModel
) {
    // API 地址
    var baseUrlField by remember { mutableStateOf(TextFieldValue(settings.aiBaseUrl)) }
    LaunchedEffect(settings.aiBaseUrl) {
        if (settings.aiBaseUrl != baseUrlField.text) {
            baseUrlField = TextFieldValue(settings.aiBaseUrl)
        }
    }
    SettingsTextField(
        title = "API 地址",
        value = baseUrlField,
        onValueChange = {
            baseUrlField = it
            viewModel.setAiBaseUrl(it.text)
        }
    )

    // 模型名
    var modelField by remember { mutableStateOf(TextFieldValue(settings.aiModel)) }
    LaunchedEffect(settings.aiModel) {
        if (settings.aiModel != modelField.text) {
            modelField = TextFieldValue(settings.aiModel)
        }
    }
    SettingsTextField(
        title = "模型名",
        value = modelField,
        onValueChange = {
            modelField = it
            viewModel.setAiModel(it.text)
        }
    )

    // 最大输出长度（以 K 为单位）
    var tokenField by remember { mutableStateOf(TextFieldValue("${settings.aiMaxTokens / 1024}")) }
    LaunchedEffect(settings.aiMaxTokens) {
        val expected = "${settings.aiMaxTokens / 1024}"
        if (expected != tokenField.text) {
            tokenField = TextFieldValue(expected)
        }
    }
    SettingsTextField(
        title = "最大输出长度 (K)",
        value = tokenField,
        placeholder = "8",
        keyboardType = KeyboardType.Number,
        onValueChange = {
            tokenField = it
            val value = it.text.toIntOrNull()
            if (value != null && value in 1..2048) {
                viewModel.setAiMaxTokens(value * 1024)
            }
        }
    )

    // 温度
    SettingsSlider(
        title = "温度",
        value = settings.aiTemperature,
        valueRange = 0f..2f,
        onValueChange = { viewModel.setAiTemperature(it) }
    )
}

/**
 * 设置分组
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val fontSize = LocalFontSize.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = fontSize.sp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
        content()
    }
}

/**
 * 设置项
 */
@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    showArrow: Boolean = true,
    onClick: () -> Unit = {}
) {
    val fontSize = LocalFontSize.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize.sp)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = (fontSize - 2).sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showArrow) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 设置开关项
 */
@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val fontSize = LocalFontSize.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize.sp),
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = (fontSize - 2).sp),
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
            )
        )
    }
}

/**
 * 字号选择对话框
 */
@Composable
private fun FontSizeSelectionDialog(
    currentSize: EditorFontSize,
    onDismiss: () -> Unit,
    onSelect: (EditorFontSize) -> Unit
) {
    GlassDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择字号") },
        text = {
            Column {
                EditorFontSize.entries.forEach { size ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = size == currentSize,
                                onClick = { onSelect(size) }
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = size == currentSize,
                            onClick = { onSelect(size) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${size.displayName} (${size.size}sp)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 设置文本输入项
 */
@Composable
private fun SettingsTextField(
    title: String,
    value: TextFieldValue,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (TextFieldValue) -> Unit
) {
    val fontSize = LocalFontSize.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize.sp),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = keyboardType
            )
        )
    }
}

/**
 * 设置密码输入项（带显示/隐藏切换）
 */
@Composable
private fun SettingsPasswordField(
    title: String,
    value: TextFieldValue,
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    onValueChange: (TextFieldValue) -> Unit
) {
    val fontSize = LocalFontSize.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize.sp),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "输入 API 密钥",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            trailingIcon = {
                IconButton(onClick = { onVisibleChange(!visible) }) {
                    Icon(
                        imageVector = if (visible) Icons.Outlined.Visibility
                                      else Icons.Outlined.VisibilityOff,
                        contentDescription = if (visible) "隐藏" else "显示"
                    )
                }
            }
        )
    }
}

/**
 * 设置滑块项
 */
@Composable
private fun SettingsSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChange: (Float) -> Unit
) {
    val fontSize = LocalFontSize.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize.sp)
            )
            Text(
                text = String.format(Locale.getDefault(), "%.1f", value),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = fontSize.sp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
