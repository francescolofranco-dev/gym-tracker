package dev.francescolofranco.gymtracker.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.data.backup.drive.DriveSnapshot
import dev.francescolofranco.gymtracker.domain.WeightUnit
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    onOpenTemplates: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel(),
    driveViewModel: DriveBackupViewModel = hiltViewModel(),
) {
    val unit by settingsViewModel.unit.collectAsStateWithLifecycle()
    val backupState by backupViewModel.state.collectAsStateWithLifecycle()
    val driveState by driveViewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingDriveRestore by remember { mutableStateOf<DriveSnapshot?>(null) }

    val createDoc = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { backupViewModel.export(it) } }

    val openDoc = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) pendingRestoreUri = uri }

    val driveSignIn = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result -> driveViewModel.onSignInResult(result.data) }

    LaunchedEffect(backupState.message, backupState.error) {
        val text = backupState.error ?: backupState.message
        if (!text.isNullOrBlank()) {
            snackbar.showSnackbar(text)
            backupViewModel.consumeMessage()
        }
    }
    LaunchedEffect(driveState.message, driveState.error) {
        val text = driveState.error ?: driveState.message
        if (!text.isNullOrBlank()) {
            snackbar.showSnackbar(text)
            driveViewModel.consumeMessage()
        }
    }
    LaunchedEffect(driveState.account) {
        if (driveState.account != null) driveViewModel.refreshSnapshots()
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
            item {
                val keepOn by settingsViewModel.keepScreenOnDuringSession.collectAsStateWithLifecycle()
                KeepScreenOnRow(
                    enabled = keepOn,
                    onChange = settingsViewModel::setKeepScreenOnDuringSession,
                )
            }
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
            item { NotificationRow() }
            item { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh) }

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
            item { SectionHeader("Google Drive") }
            item {
                DriveAccountRow(
                    state = driveState,
                    onSignIn = { driveSignIn.launch(driveViewModel.signInIntent()) },
                    onSignOut = { driveViewModel.signOut() },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            }
            if (driveState.account != null) {
                item {
                    SettingsRow(
                        icon = Icons.Filled.CloudSync,
                        title = "Backup now",
                        subtitle = "Upload a fresh JSON snapshot to your Drive's appDataFolder.",
                        onClick = { driveViewModel.backupNow() },
                        enabled = !driveState.running,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                }
                if (driveState.snapshots.isEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                text = "No Drive snapshots yet — daily 03:00 backups will populate them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "Recent snapshots",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(items = driveState.snapshots, key = { it.id }) { snap ->
                        SnapshotRow(
                            snapshot = snap,
                            onClick = { pendingDriveRestore = snap },
                            enabled = !driveState.running,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    }
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

    pendingDriveRestore?.let { snap ->
        AlertDialog(
            onDismissRequest = { pendingDriveRestore = null },
            title = { Text("Restore from Drive?") },
            text = {
                Text(
                    "Replaces every workout, exercise, and template currently in the app with " +
                        "${snap.name}. There's no undo.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    driveViewModel.restore(snap)
                    pendingDriveRestore = null
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDriveRestore = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DriveAccountRow(
    state: DriveUiState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    val account = state.account
    if (account == null) {
        SettingsRow(
            icon = Icons.Filled.CloudOff,
            title = "Connect Google Drive",
            subtitle = "Daily 03:00 backups upload to your Drive's appDataFolder; keeps the last 7 snapshots.",
            onClick = onSignIn,
            enabled = !state.running,
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CloudDone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = account.email ?: "Connected", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = state.lastBackupAt?.let { "Last backup: ${formatRelativeTime(it)}" }
                        ?: "Connected — daily backup will run at 03:00.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onSignOut, enabled = !state.running) {
                Text("Disconnect")
            }
        }
    }
}

@Composable
private fun SnapshotRow(snapshot: DriveSnapshot, onClick: () -> Unit, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(imageVector = Icons.Filled.Restore, contentDescription = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = snapshot.createdAt?.let { formatRelativeTime(it) } ?: snapshot.name,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = snapshot.sizeBytes?.let { "${it / 1024} KB · ${snapshot.name}" } ?: snapshot.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatRelativeTime(at: Instant): String {
    val now = Instant.now()
    val diff = Duration.between(at, now)
    return when {
        diff.toMinutes() < 1 -> "just now"
        diff.toMinutes() < 60 -> "${diff.toMinutes()}m ago"
        diff.toHours() < 24 -> "${diff.toHours()}h ago"
        diff.toDays() < 7 -> "${diff.toDays()}d ago"
        else -> {
            val zone = ZoneId.systemDefault()
            DateTimeFormatter.ofPattern("d MMM").withZone(zone).format(at)
        }
    }
}

@Composable
private fun KeepScreenOnRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(imageVector = Icons.Filled.Visibility, contentDescription = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Keep screen on during session", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Prevents the screen from sleeping while the active session screen is open.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        androidx.compose.material3.Switch(
            checked = enabled,
            onCheckedChange = onChange,
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
private fun NotificationRow() {
    val context = LocalContext.current
    val granted by rememberNotificationGranted()
    SettingsRow(
        icon = if (granted) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
        title = "Timer notification",
        subtitle = if (granted) "Allowed — the workout timer notification is visible."
        else "Disabled — Android won't show the timer's persistent notification. Tap to fix in system settings.",
        onClick = {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // Older devices may not have this action — fall back to app-details settings.
            val target = if (intent.resolveActivity(context.packageManager) != null) intent
            else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(target)
        },
    )
}

@Composable
private fun rememberNotificationGranted(): State<Boolean> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember { androidx.compose.runtime.mutableStateOf(checkNotificationGranted(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.value = checkNotificationGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return state
}

private fun checkNotificationGranted(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
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
