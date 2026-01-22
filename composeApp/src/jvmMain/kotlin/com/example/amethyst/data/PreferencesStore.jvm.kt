package com.example.amethyst.data

import java.util.prefs.Preferences as JavaPreferences

actual class PreferencesStore {
    private val preferences = JavaPreferences.userRoot().node("amethyst")

    actual fun saveString(key: String, value: String) {
        preferences.put(key, value)
    }

    actual fun getString(key: String, defaultValue: String): String {
        return preferences.get(key, defaultValue)
    }

    actual fun saveInt(key: String, value: Int) {
        preferences.putInt(key, value)
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return preferences.getInt(key, defaultValue)
    }

    actual fun notifyWidgetsOfThemeChange() {
        // No-op on JVM - widgets not implemented
    }
}
