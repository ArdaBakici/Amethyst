package com.example.amethyst.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import com.example.amethyst.widget.TaskWidgetProvider

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

    fun notifyWidgetsOfThemeChange() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TaskWidgetProvider::class.java)
            )

            if (appWidgetIds.isNotEmpty()) {
                println("PreferencesStore: Notifying ${appWidgetIds.size} widgets of theme change")
                for (appWidgetId in appWidgetIds) {
                    TaskWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
        } catch (e: Exception) {
            println("PreferencesStore: Failed to notify widgets: ${e.message}")
            e.printStackTrace()
        }
    }
}
