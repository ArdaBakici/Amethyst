package com.example.amethyst.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple preferences manager for storing app settings
 */
class Preferences {
    private val _vaultPath = MutableStateFlow("")
    val vaultPath: StateFlow<String> = _vaultPath.asStateFlow()

    fun setVaultPath(path: String) {
        _vaultPath.value = path
        // In a production app, this would persist to disk/SharedPreferences/UserDefaults
    }

    companion object {
        val instance = Preferences()
    }
}
