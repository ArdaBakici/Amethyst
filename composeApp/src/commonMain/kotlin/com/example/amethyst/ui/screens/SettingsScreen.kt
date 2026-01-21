package com.example.amethyst.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.amethyst.data.*
import com.example.amethyst.ui.FolderPickerButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vaultPath: String,
    onVaultPathChange: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val preferences = remember { Preferences.instance }
    val themeMode by preferences.themeMode.collectAsState()
    val tasksFolder by preferences.tasksFolder.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var validationResult by remember { mutableStateOf<VaultValidationResult?>(null) }
    var isValidating by remember { mutableStateOf(false) }

    // Validate vault when path changes
    LaunchedEffect(vaultPath) {
        if (vaultPath.isNotBlank()) {
            isValidating = true
            val fileService = FileService()
            val validator = VaultValidator(fileService)
            val result = validator.validateVault(vaultPath)
            validationResult = result
            if (result.isValid && result.tasksFolder.isNotBlank()) {
                preferences.setTasksFolder(result.tasksFolder)
            }
            isValidating = false
        } else {
            validationResult = null
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Settings Section
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleLarge
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = { preferences.setThemeMode(mode) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (mode) {
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                    ThemeMode.SYSTEM -> "System Default"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Vault Configuration Section
            Text(
                text = "Obsidian Vault Configuration",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Select the root folder of your Obsidian vault. The app will automatically detect TaskNotes configuration.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = vaultPath,
                onValueChange = { newPath ->
                    preferences.setVaultRootPath(newPath)
                    onVaultPathChange(newPath)
                },
                label = { Text("Vault Root Path") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    FolderPickerButton(onFolderSelected = { newPath ->
                        preferences.setVaultRootPath(newPath)
                        onVaultPathChange(newPath)
                    })
                },
                supportingText = {
                    Text("Path to your Obsidian vault root folder")
                },
                enabled = !isValidating
            )

            // Validation Status
            if (isValidating) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Validating vault...")
                    }
                }
            }

            validationResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.isValid)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (result.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (result.isValid)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (result.isValid) "Valid Obsidian Vault" else "Invalid Vault",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (result.isValid)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        if (result.errorMessage != null) {
                            Text(
                                text = result.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (result.isValid)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        if (result.tasksFolder.isNotBlank()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                            )
                            Text(
                                text = "Tasks Folder:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = result.tasksFolder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // About Section
            Text(
                text = "About TaskNotes Format",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "This app uses the TaskNotes format for Obsidian, storing tasks as Markdown files with YAML frontmatter. Tasks created here are fully compatible with the TaskNotes plugin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
