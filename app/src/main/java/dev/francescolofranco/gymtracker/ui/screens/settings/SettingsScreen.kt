package dev.francescolofranco.gymtracker.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.domain.WeightUnit

@Composable
fun SettingsScreen(
    onOpenTemplates: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel(),
) {
    val unit by settingsViewModel.unit.collectAsStateWithLifecycle()
    val backupState by backupViewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val createDoc = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { backupViewModel.export(it) } }

    val openDoc = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) pendingRestoreUri = uri }

    LaunchedEffect(backupState.message, backupState.error) {
        val text = backupState.error ?: backupState.message
        if (!text.isNullOrBlank()) {
            snackbar.showSnackbar(text)
            backupViewModel.consumeMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            item { SectionHeader("Display") }
            item { UnitsRow(unit = unit, onChange = settingsViewModel::setUnit) }
            item { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh) }

            item { SectionHeader("Workout") }
            item {
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    title = "Templates",
                    subtitle = "Create, edit, delete reusable workouts.",
                    onClick = onOpenTemplates,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            }

            item { SectionHeader("Backup") }
            item {
                SettingsRow(
                    icon = Icons.Filled.CloudUpload,
                    title = "Export to file",
                    subtitle = "Save a JSON snapshot of all workouts to a folder of your choice.",
                    onClick = { createDoc.launch(backupViewModel.suggestedFilename()) },
                    enabled = !backupState.running,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            }
            item {
                SettingsRow(
                    icon = Icons.Filled.CloudDownload,
                    title = "Restore from file",
                    subtitle = "Replace local data with a previously exported JSON backup.",
                    onClick = { openDoc.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    enabled = !backupState.running,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "Daily 03:00 backups run locally for now. Google Drive sync arrives once OAuth is set up.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { SectionHeader("About") }
            item { AboutRow() }
        }
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("Restore from this file?") },
            text = {
                Text(
                    "This replaces every workout, exercise, and template currently in the app " +
                        "with the contents of the backup file. There's no undo.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    backupViewModel.import(uri)
                    pendingRestoreUri = null
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun UnitsRow(unit: WeightUnit, onChange: (WeightUnit) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(imageVector = Icons.Filled.Scale, contentDescription = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Units", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Used for weight steppers and total-volume figures.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = unit == WeightUnit.KG,
                onClick = { onChange(WeightUnit.KG) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                modifier = Modifier.semantics { contentDescription = "Use kilograms" },
            ) { Text("kg") }
            SegmentedButton(
                selected = unit == WeightUnit.LBS,
                onClick = { onChange(WeightUnit.LBS) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                modifier = Modifier.semantics { contentDescription = "Use pounds" },
            ) { Text("lbs") }
        }
    }
}

@Composable
private fun AboutRow() {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            pi.versionName ?: "?"
        }.getOrDefault("?")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(imageVector = Icons.Filled.Info, contentDescription = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Gym Tracker", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Version $version",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
