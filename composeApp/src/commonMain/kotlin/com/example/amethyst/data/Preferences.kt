package com.example.amethyst.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    // Computed property for the full vault path (root + tasks folder)
    val vaultPath: StateFlow<String> = _vaultRootPath

    fun setVaultRootPath(path: String) {
        _vaultRootPath.value = path
        store.saveString(KEY_VAULT_ROOT, path)
    }

    fun setTasksFolder(folder: String) {
        _tasksFolder.value = folder
        store.saveString(KEY_TASKS_FOLDER, folder)
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
