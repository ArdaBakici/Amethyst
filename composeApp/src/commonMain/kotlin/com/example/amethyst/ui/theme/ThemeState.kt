package com.example.amethyst.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

object ThemeState {
    val isDarkMode: MutableState<Boolean> = mutableStateOf(false)

    fun toggleTheme() {
        isDarkMode.value = !isDarkMode.value
    }
}

@Composable
fun rememberThemeState(): MutableState<Boolean> {
    return remember { ThemeState.isDarkMode }
}
