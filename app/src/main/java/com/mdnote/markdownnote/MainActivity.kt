package com.mdnote.markdownnote

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
// 页面不可见时暂停业务 Flow 收集
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mdnote.markdownnote.data.repository.FolderRepository
import com.mdnote.markdownnote.data.repository.RecentExternalFile
import com.mdnote.markdownnote.ui.ai.SoulEditScreen
import com.mdnote.markdownnote.ui.folder.FolderBrowserSheetContent
import com.mdnote.markdownnote.ui.main.MainViewModel
import com.mdnote.markdownnote.ui.main.PendingNavigation
import com.mdnote.markdownnote.ui.notelist.NoteListScreen
import com.mdnote.markdownnote.ui.reader.MarkdownReaderScreen
import com.mdnote.markdownnote.ui.settings.SettingsSheetContent
import com.mdnote.markdownnote.ui.settings.SettingsSubPage
import com.mdnote.markdownnote.ui.theme.LocalFolderRepository
import com.mdnote.markdownnote.ui.theme.LocalFontSize
import com.mdnote.markdownnote.ui.theme.MarkdownNoteTheme
import com.mdnote.markdownnote.ui.trash.TrashSheetContent
import com.mdnote.markdownnote.util.AppSettings
import com.mdnote.markdownnote.util.ExternalFileManager
import com.mdnote.markdownnote.util.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 当前打开的毛玻璃 sheet 类型（null 表示无弹窗）
 */
enum class GlassSheetType { SETTINGS, TRASH, FOLDER }

@AndroidEntryPoint
class MainActivity : androidx.activity.ComponentActivity() {

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var folderRepository: FolderRepository

    @Inject
    lateinit var externalFileManager: ExternalFileManager

    // 用于在 onNewIntent 中更新 URI 触发 Compose 重组
    private var externalFileUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化 FolderRepository
        folderRepository.initialize()

        // 处理从外部打开文件
        externalFileUri.value = intent?.data

        setContent {
            // 保留原初始值，同时按 Activity 生命周期收集设置
            val settings by settingsManager.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )

            // 根据设置决定是否使用深色主题
            val darkTheme = if (settings.followSystemTheme) {
                isSystemInDarkTheme()
            } else {
                settings.darkMode
            }

            MarkdownNoteTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(
                    LocalFontSize provides settings.editorFontSize.size,
                    LocalFolderRepository provides folderRepository
                ) {
                    AppNavigation(
                        initialUri = externalFileUri.value,
                        folderRepository = folderRepository,
                        externalFileManager = externalFileManager
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // 更新 URI 触发 Compose 重新处理
        externalFileUri.value = intent?.data
    }
}

@Composable
fun AppNavigation(
    initialUri: Uri? = null,
    folderRepository: FolderRepository,
    externalFileManager: ExternalFileManager,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    var activeSheet by remember { mutableStateOf<GlassSheetType?>(null) }
    var pendingSettingsSubPage by remember { mutableStateOf(SettingsSubPage.MAIN) }

    // 观察 MainViewModel 状态
    // 导航 UI 状态仅在宿主可见时收集
    val isInitialized by mainViewModel.isInitialized.collectAsStateWithLifecycle()
    val currentFolderUri by mainViewModel.currentFolderUri.collectAsStateWithLifecycle()
    val recentFiles by mainViewModel.recentFiles.collectAsStateWithLifecycle()
    val externalFileState by mainViewModel.currentExternalFile.collectAsStateWithLifecycle()
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()

    // 当前活动的文件夹 URI（用于导航传递）
    var activeFolderUri by remember { mutableStateOf<Uri?>(null) }
    var activeFileUri by remember { mutableStateOf<Uri?>(null) }

    // 标记是否需要导航到外部文件
    var shouldNavigateToExternalFile by remember { mutableStateOf(false) }

    // 处理初始外部文件
    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            val state = externalFileManager.loadExternalFile(initialUri)
            if (state != null) {
                activeFileUri = state.fileUri
                activeFolderUri = state.folderUri ?: folderRepository.getCurrentFolderUri()
                shouldNavigateToExternalFile = true
            }
        }
    }

    // 导航到外部文件编辑页面
    LaunchedEffect(shouldNavigateToExternalFile) {
        if (shouldNavigateToExternalFile && activeFileUri != null) {
            navController.navigate("note_edit/0") {
                popUpTo("note_list") { inclusive = false }
            }
            shouldNavigateToExternalFile = false
        }
    }

    // 处理待处理的导航
    LaunchedEffect(mainUiState.pendingNavigation) {
        when (val navigation = mainUiState.pendingNavigation) {
            is PendingNavigation.ToExternalFile -> {
                activeFileUri = navigation.fileUri
                activeFolderUri = navigation.folderUri
                navController.navigate("note_edit/0") {
                    popUpTo("note_list") { inclusive = false }
                }
                mainViewModel.clearPendingNavigation()
            }
            is PendingNavigation.ToNote -> {
                activeFileUri = null
                activeFolderUri = null
                navController.navigate("note_edit/${navigation.noteId}")
                mainViewModel.clearPendingNavigation()
            }
            null -> {}
        }
    }

    NavHost(navController = navController, startDestination = "note_list") {
        composable("note_list") {
            NoteListScreen(
                onNoteClick = { noteId ->
                    mainViewModel.openNote(noteId)
                },
                onSettingsClick = { activeSheet = GlassSheetType.SETTINGS },
                onTrashClick = { activeSheet = GlassSheetType.TRASH },
                onFolderBrowserClick = { activeSheet = GlassSheetType.FOLDER },
                onExternalFileClick = { fileUri, folderUri ->
                    mainViewModel.openRecentFile(fileUri, folderUri)
                },
                mainViewModel = mainViewModel
            )
        }

        composable(
            route = "note_edit/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType; defaultValue = 0L })
        ) {
            MarkdownReaderScreen(
                onNavigateBack = {
                    // popBackStack() 在 startDestination（根目的地）上虽返回 false，
                    // 但仍会清空 currentDestination，导致 NavHost 无目的地渲染而白屏。
                    // 仅当存在上一个 back stack entry（当前不在根目的地）时才弹出。
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onNavigateToSettings = {
                    activeSheet = GlassSheetType.SETTINGS
                },
                onNavigateToAiConfig = {
                    pendingSettingsSubPage = SettingsSubPage.AI_CONFIG
                    activeSheet = GlassSheetType.SETTINGS
                },
                externalFileUri = activeFileUri,
                folderUri = activeFolderUri,
                folderRepository = folderRepository
            )
        }

        composable("soul_edit") {
            SoulEditScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
    activeSheet?.let { type ->
        when (type) {
            GlassSheetType.SETTINGS -> SettingsSheetContent(
                onDismiss = {
                    activeSheet = null
                    pendingSettingsSubPage = SettingsSubPage.MAIN
                },
                initialSubPage = pendingSettingsSubPage
            )
            GlassSheetType.TRASH -> TrashSheetContent(
                onDismiss = { activeSheet = null }
            )
            GlassSheetType.FOLDER -> FolderBrowserSheetContent(
                onDismiss = { activeSheet = null },
                onOpenFile = { fileUri, folderUriStr ->
                    activeSheet = null
                    val folderUri = Uri.parse(folderUriStr)
                    activeFileUri = fileUri
                    activeFolderUri = folderUri
                    folderRepository.addRecentFile(
                        RecentExternalFile(
                            fileUri = fileUri,
                            folderUri = folderUri,
                            fileName = folderRepository.getFileName(fileUri)
                        )
                    )
                    navController.navigate("note_edit/0") {
                        popUpTo("note_list") { inclusive = false }
                    }
                },
                folderRepository = folderRepository
            )
        }
    }
}
