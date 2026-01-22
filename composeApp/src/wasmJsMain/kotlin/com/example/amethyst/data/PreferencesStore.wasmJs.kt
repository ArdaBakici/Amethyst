package com.example.amethyst.data

import kotlinx.browser.localStorage

actual class PreferencesStore {
    actual fun saveString(key: String, value: String) {
        localStorage.setItem(key, value)
    }

    actual fun getString(key: String, defaultValue: String): String {
        return localStorage.getItem(key) ?: defaultValue
    }

    actual fun saveInt(key: String, value: Int) {
        localStorage.setItem(key, value.toString())
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return localStorage.getItem(key)?.toIntOrNull() ?: defaultValue
    }

    actual fun notifyWidgetsOfThemeChange() {
        // No-op on WASM - widgets not implemented
    }
}
