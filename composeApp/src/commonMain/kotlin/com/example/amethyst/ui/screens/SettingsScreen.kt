package com.example.amethyst.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.amethyst.ui.FolderPickerButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vaultPath: String,
    onVaultPathChange: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Obsidian Vault Configuration",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Select the folder where your TaskNotes tasks are stored. This should be a folder within your Obsidian vault.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = vaultPath,
                onValueChange = onVaultPathChange,
                label = { Text("Vault Path") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    FolderPickerButton(onFolderSelected = onVaultPathChange)
                },
                supportingText = {
                    Text("Path to your Obsidian vault task folder")
                }
            )

            if (vaultPath.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Current vault path:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = vaultPath,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Divider()

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
