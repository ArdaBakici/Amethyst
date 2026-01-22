package com.example.amethyst.data

import android.content.Context
import android.content.SharedPreferences

actual class PreferencesStore(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("amethyst_preferences", Context.MODE_PRIVATE)

    actual fun saveString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    actual fun getString(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    actual fun saveInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    actual fun notifyWidgetsOfThemeChange() {
        // Send broadcast to notify widgets of theme change
        // This is handled by the widget provider in the androidApp module
        val intent = android.content.Intent("com.example.amethyst.THEME_CHANGED")
        context.sendBroadcast(intent)
    }
}
