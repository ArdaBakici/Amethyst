package com.example.amethyst.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

/**
 * Simple preferences manager for storing app settings
 */
class Preferences {
    private val _vaultRootPath = MutableStateFlow("")
    val vaultRootPath: StateFlow<String> = _vaultRootPath.asStateFlow()

    private val _tasksFolder = MutableStateFlow("")
    val tasksFolder: StateFlow<String> = _tasksFolder.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // Computed property for the full vault path (root + tasks folder)
    val vaultPath: StateFlow<String> = _vaultRootPath

    fun setVaultRootPath(path: String) {
        _vaultRootPath.value = path
        // In a production app, this would persist to disk/SharedPreferences/UserDefaults
    }

    fun setTasksFolder(folder: String) {
        _tasksFolder.value = folder
        // In a production app, this would persist to disk/SharedPreferences/UserDefaults
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        // In a production app, this would persist to disk/SharedPreferences/UserDefaults
    }

    fun getFullTasksPath(): String {
        val root = _vaultRootPath.value
        val folder = _tasksFolder.value
        return if (root.isNotBlank()) {
            if (folder.isNotBlank()) {
                "$root/$folder"
            } else {
                root
            }
        } else {
            ""
        }
    }

    companion object {
        val instance = Preferences()
    }
}
