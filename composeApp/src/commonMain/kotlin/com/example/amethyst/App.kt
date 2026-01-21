package com.example.amethyst

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amethyst.data.*
import com.example.amethyst.ui.screens.SettingsScreen
import com.example.amethyst.ui.screens.TaskDetailScreen
import com.example.amethyst.ui.screens.TaskListScreen
import com.example.amethyst.ui.theme.AmethystTheme
import com.example.amethyst.ui.theme.ThemeState
import com.example.amethyst.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@Composable
fun App() {
    val preferences = remember { Preferences.instance }
    val vaultRootPath by preferences.vaultRootPath.collectAsState()
    val tasksFolder by preferences.tasksFolder.collectAsState()
    val themeMode by preferences.themeMode.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Determine dark mode based on theme preference
    val systemInDarkTheme = isSystemInDarkTheme()
    val isDarkMode = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemInDarkTheme
    }

    // Navigation state
    var currentScreen by remember { mutableStateOf<Screen>(Screen.TaskList) }
    var selectedTaskId by remember { mutableStateOf<String?>(null) }

    // Compute full tasks path
    val fullTasksPath = remember(vaultRootPath, tasksFolder) {
        preferences.getFullTasksPath()
    }

    // Initialize repository and ViewModel
    val repository = remember(fullTasksPath) {
        if (fullTasksPath.isNotBlank()) {
            val fileService = FileService()
            val repo = TaskRepository(fileService, fullTasksPath)
            coroutineScope.launch {
                repo.loadTasks()
            }
            repo
        } else {
            null
        }
    }

    val viewModel: TaskViewModel? = repository?.let { repo ->
        remember(repo) {
            TaskViewModel(repo)
        }
    }

    AmethystTheme(darkTheme = isDarkMode) {
        when {
            vaultRootPath.isBlank() -> {
                // Show settings screen if vault path not configured
                SettingsScreen(
                    vaultPath = vaultRootPath,
                    onVaultPathChange = { preferences.setVaultRootPath(it) },
                    onNavigateBack = { /* No back navigation from initial setup */ }
                )
            }
            viewModel != null -> {
                when (val screen = currentScreen) {
                    is Screen.TaskList -> {
                        TaskListScreen(
                            viewModel = viewModel,
                            onTaskClick = { task ->
                                selectedTaskId = task.id
                                currentScreen = Screen.TaskDetail
                            },
                            onAddTask = {
                                selectedTaskId = null
                                currentScreen = Screen.TaskDetail
                            },
                            onSettingsClick = {
                                currentScreen = Screen.Settings
                            }
                        )
                    }
                    is Screen.TaskDetail -> {
                        TaskDetailScreen(
                            viewModel = viewModel,
                            taskId = selectedTaskId,
                            onNavigateBack = {
                                currentScreen = Screen.TaskList
                                selectedTaskId = null
                            }
                        )
                    }
                    is Screen.Settings -> {
                        SettingsScreen(
                            vaultPath = vaultRootPath,
                            onVaultPathChange = { preferences.setVaultRootPath(it) },
                            onNavigateBack = {
                                currentScreen = Screen.TaskList
                            }
                        )
                    }
                }
            }
        }
    }
}

sealed class Screen {
    object TaskList : Screen()
    object TaskDetail : Screen()
    object Settings : Screen()
}
