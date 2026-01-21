package com.example.amethyst.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

/**
 * Preferences manager for storing app settings with platform-specific persistence
 */
expect class PreferencesStore {
    fun saveString(key: String, value: String)
    fun getString(key: String, defaultValue: String): String
    fun saveInt(key: String, value: Int)
    fun getInt(key: String, defaultValue: Int): Int
}

/**
 * Simple preferences manager for storing app settings
 */
class Preferences(private val store: PreferencesStore) {
    private val _vaultRootPath = MutableStateFlow(store.getString(KEY_VAULT_ROOT, ""))
    val vaultRootPath: StateFlow<String> = _vaultRootPath.asStateFlow()

    private val _tasksFolder = MutableStateFlow(store.getString(KEY_TASKS_FOLDER, ""))
    val tasksFolder: StateFlow<String> = _tasksFolder.asStateFlow()

    private val _themeMode = MutableStateFlow(
        ThemeMode.entries.getOrNull(store.getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.ordinal)) ?: ThemeMode.SYSTEM
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // Combined flow that updates when either vault root or tasks folder changes
    private val _vaultPath = MutableStateFlow(getFullTasksPath())
    val vaultPath: StateFlow<String> = _vaultPath.asStateFlow()

    init {
        println("Preferences initialized:")
        println("  vaultRootPath: ${_vaultRootPath.value}")
        println("  tasksFolder: ${_tasksFolder.value}")
        println("  combined vaultPath: ${_vaultPath.value}")
    }

    fun setVaultRootPath(path: String) {
        println("Preferences.setVaultRootPath: $path")
        _vaultRootPath.value = path
        store.saveString(KEY_VAULT_ROOT, path)
        _vaultPath.value = getFullTasksPath()
        println("Preferences: Updated vaultPath to: ${_vaultPath.value}")
    }

    fun setTasksFolder(folder: String) {
        println("Preferences.setTasksFolder: $folder")
        _tasksFolder.value = folder
        store.saveString(KEY_TASKS_FOLDER, folder)
        _vaultPath.value = getFullTasksPath()
        println("Preferences: Updated vaultPath to: ${_vaultPath.value}")
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        store.saveInt(KEY_THEME_MODE, mode.ordinal)
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
        private const val KEY_VAULT_ROOT = "vault_root_path"
        private const val KEY_TASKS_FOLDER = "tasks_folder"
        private const val KEY_THEME_MODE = "theme_mode"

        lateinit var instance: Preferences
            private set

        fun initialize(store: PreferencesStore) {
            instance = Preferences(store)
        }
    }
}
