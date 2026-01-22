package com.example.amethyst.data

import platform.Foundation.NSUserDefaults

actual class PreferencesStore {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    actual fun saveString(key: String, value: String) {
        userDefaults.setObject(value, key)
    }

    actual fun getString(key: String, defaultValue: String): String {
        return userDefaults.stringForKey(key) ?: defaultValue
    }

    actual fun saveInt(key: String, value: Int) {
        userDefaults.setInteger(value.toLong(), key)
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return userDefaults.integerForKey(key).toInt()
    }

    actual fun notifyWidgetsOfThemeChange() {
        // No-op on iOS - widgets not implemented
    }
}
