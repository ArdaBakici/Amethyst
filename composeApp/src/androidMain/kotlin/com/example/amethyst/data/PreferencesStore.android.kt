package com.example.amethyst.data

import android.content.Context
import android.content.SharedPreferences

actual class PreferencesStore(context: Context) {
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
}
