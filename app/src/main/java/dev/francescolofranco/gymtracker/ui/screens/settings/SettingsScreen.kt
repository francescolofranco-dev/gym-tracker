package dev.francescolofranco.gymtracker.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.R
import dev.francescolofranco.gymtracker.data.backup.drive.DriveSnapshot
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.ui.components.ErrorPane
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.LoadingPane
import dev.francescolofranco.gymtracker.ui.motion.GymMotion
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.DarkOnPrimary
import dev.francescolofranco.gymtracker.ui.theme.OliveSubtext
import dev.francescolofranco.gymtracker.ui.theme.SlateSubtext
import dev.francescolofranco.gymtracker.ui.theme.SurfaceBase
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard
import dev.francescolofranco.gymtracker.ui.theme.SurfaceChip
import dev.francescolofranco.gymtracker.ui.theme.SurfaceControl
import dev.francescolofranco.gymtracker.ui.theme.SurfaceDeep
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated
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
    val loadableSettings by settingsViewModel.content.collectAsStateWithLifecycle()
    val settings = (loadableSettings as? Loadable.Ready)?.value
    val backupState by backupViewModel.state.collectAsStateWithLifecycle()
    val driveState by driveViewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    (loadableSettings as? Loadable.Error)?.let {
        ErrorPane(it.message, settingsViewModel::retry)
        return
    }

    var confirmRecovery by remember { mutableStateOf(false) }

    val createDoc = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { backupViewModel.export(it) } }

    val openDoc = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) backupViewModel.inspect(uri) }

    val driveAuthorization = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result -> driveViewModel.onAuthorizationResult(result.data) }

    LaunchedEffect(driveState.authorizationRequest) {
        driveState.authorizationRequest?.let { pendingIntent ->
            driveAuthorization.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            driveViewModel.consumeAuthorizationRequest()
        }
    }

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
    LaunchedEffect(driveState.connected) {
        if (driveState.connected) driveViewModel.refreshSnapshots()
    }

    Scaffold(
        containerColor = SurfaceBase,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (settings == null) {
            LoadingPane(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "settings_top_header") {
                SettingsTopHeader()
            }

            item(key = "display_section") {
                Column {
                    SectionHeader("Display")
                    SettingsGroupCard {
                        UnitsRow(unit = settings.unit, onChange = settingsViewModel::setUnit)
                        SettingsGroupDivider()
                        KeepScreenOnRow(
                            enabled = settings.keepScreenOnDuringSession,
                            onChange = settingsViewModel::setKeepScreenOnDuringSession,
                        )
                    }
                }
            }

            item(key = "workout_section") {
                Column {
                    SectionHeader("Workout")
                    SettingsGroupCard {
                        SettingsRow(
                            icon = Icons.AutoMirrored.Filled.ListAlt,
                            title = "WORKOUT TEMPLATES",
                            subtitle = "Create, edit, delete reusable workouts.",
                            onClick = onOpenTemplates,
                        )
                        if (backupState.hasRecovery) {
                            SettingsGroupDivider()
                            SettingsRow(
                                icon = Icons.Filled.Restore,
                                title = "UNDO LAST RESTORE",
                                subtitle = "Recover the automatic safety copy made immediately before the last restore.",
                                onClick = { confirmRecovery = true },
                                enabled = !backupState.running,
                            )
                        }
                        SettingsGroupDivider()
                        NotificationRow()
                        SettingsGroupDivider()
                        FloatingTimerRow()
                    }
                }
            }

            item(key = "backup_section") {
                Column {
                    SectionHeader("Backup")
                    SettingsGroupCard {
                        SettingsRow(
                            icon = Icons.Filled.CloudUpload,
                            title = "EXPORT TO FILE",
                            subtitle = "Save a JSON snapshot of all workouts to a folder of your choice.",
                            onClick = { createDoc.launch(backupViewModel.suggestedFilename()) },
                            enabled = !backupState.running,
                        )
                        SettingsGroupDivider()
                        SettingsRow(
                            icon = Icons.Filled.CloudDownload,
                            title = "RESTORE FROM FILE",
                            subtitle = "Replace local data with a previously exported JSON backup.",
                            onClick = { openDoc.launch(arrayOf("application/json", "text/plain", "*/*")) },
                            enabled = !backupState.running,
                        )
                    }
                }
            }

            item(key = "drive_section") {
                Column(
                    modifier = Modifier.animateItem(
                        fadeInSpec = GymMotion.ItemFadeIn,
                        placementSpec = GymMotion.ItemPlacement,
                        fadeOutSpec = GymMotion.ItemFadeOut,
                    ),
                ) {
                    SectionHeader("Google Drive")
                    SettingsGroupCard {
                        DriveAccountRow(
                            state = driveState,
                            onConnect = driveViewModel::connect,
                            onDisconnect = driveViewModel::disconnect,
                        )
                        if (driveState.connected) {
                            SettingsGroupDivider()
                            SettingsRow(
                                icon = Icons.Filled.CloudSync,
                                title = "BACKUP NOW",
                                subtitle = "Upload a fresh JSON snapshot to your Drive's appDataFolder.",
                                onClick = { driveViewModel.backupNow() },
                                enabled = !driveState.running,
                            )
                        }
                    }
                }
            }

            if (driveState.connected) {
                if (driveState.snapshots.isEmpty()) {
                    item(key = "drive_empty") {
                        Column(
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = GymMotion.ItemFadeIn,
                                    placementSpec = GymMotion.ItemPlacement,
                                    fadeOutSpec = GymMotion.ItemFadeOut,
                                )
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "No Drive snapshots yet — daily 03:00 backups will populate them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OliveSubtext,
                            )
                        }
                    }
                } else {
                    item(key = "drive_recent_header") {
                        Text(
                            text = "RECENT SNAPSHOTS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp,
                            ),
                            color = OliveSubtext,
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = GymMotion.ItemFadeIn,
                                    placementSpec = GymMotion.ItemPlacement,
                                    fadeOutSpec = GymMotion.ItemFadeOut,
                                )
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    items(items = driveState.snapshots, key = { "drive_snapshot_${it.id}" }) { snap ->
                        Column(
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = GymMotion.ItemFadeIn,
                                    placementSpec = GymMotion.ItemPlacement,
                                    fadeOutSpec = GymMotion.ItemFadeOut,
                                )
                                .padding(horizontal = 16.dp),
                        ) {
                            SettingsGroupCard(horizontalPadding = 0.dp) {
                                SnapshotRow(
                                    snapshot = snap,
                                    onClick = { driveViewModel.inspectRestore(snap) },
                                    enabled = !driveState.running,
                                )
                            }
                        }
                    }
                }
            }

            item(key = "about_section") {
                Column(
                    modifier = Modifier.animateItem(
                        fadeInSpec = GymMotion.ItemFadeIn,
                        placementSpec = GymMotion.ItemPlacement,
                        fadeOutSpec = GymMotion.ItemFadeOut,
                    ),
                ) {
                    SectionHeader("About")
                    SettingsGroupCard {
                        AboutRow()
                    }
                }
            }
        }
    }

    backupState.preview?.let { preview ->
        val summary = preview.summary
        AlertDialog(
            onDismissRequest = backupViewModel::dismissPreview,
            shape = dev.francescolofranco.gymtracker.ui.theme.DialogShape,
            containerColor = SurfaceCard,
            title = { Text("Restore from this file?") },
            text = {
                Text(
                    buildString {
                        append("Validated snapshot")
                        summary.exportedAt?.let { append(" from ${formatRelativeTime(it)}") }
                        append(": ${summary.sessions} sessions, ${summary.exercises} exercises, ")
                        append("${summary.templates} templates and ${summary.setLogs} sets. ")
                        append("It will replace local data after saving an automatic recovery copy.")
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = backupViewModel::restorePreview) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = backupViewModel::dismissPreview) { Text("Cancel") }
            },
        )
    }

    if (confirmRecovery) {
        AlertDialog(
            onDismissRequest = { confirmRecovery = false },
            shape = dev.francescolofranco.gymtracker.ui.theme.DialogShape,
            containerColor = SurfaceCard,
            title = { Text("Undo the last restore?") },
            text = { Text("This replaces current data with the safety copy captured before the last restore.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRecovery = false
                    backupViewModel.restoreRecovery()
                }) { Text("Recover") }
            },
            dismissButton = { TextButton(onClick = { confirmRecovery = false }) { Text("Cancel") } },
        )
    }

    driveState.restorePreview?.let { preview ->
        val snap = preview.snapshot
        val summary = preview.summary
        AlertDialog(
            onDismissRequest = driveViewModel::dismissRestorePreview,
            shape = dev.francescolofranco.gymtracker.ui.theme.DialogShape,
            containerColor = SurfaceCard,
            title = { Text("Restore from Drive?") },
            text = {
                Text(
                    "Validated ${snap.name}: ${summary.sessions} sessions, ${summary.exercises} exercises, " +
                        "${summary.templates} templates and ${summary.setLogs} sets. Local data will be " +
                        "replaced after an automatic recovery copy is saved.",
                )
            },
            confirmButton = {
                TextButton(onClick = driveViewModel::restorePreview) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = driveViewModel::dismissRestorePreview) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SettingsTopHeader() {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            pi.versionName ?: "?"
        }.getOrDefault("?")
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                ),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceElevated)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "v$version",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = OliveSubtext,
                )
            }
        }
        Text(
            text = "SYSTEM PREFERENCES & DATA VAULT",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            ),
            color = OliveSubtext,
        )
    }
}

@Composable
private fun SettingsGroupCard(
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceElevated),
    ) {
        content()
    }
}

@Composable
private fun SettingsGroupDivider() {
    HorizontalDivider(color = SurfaceControl, thickness = 1.dp)
}

@Composable
private fun DriveAccountRow(
    state: DriveUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    if (state.checkingConnection) {
        SettingsRow(
            icon = Icons.Filled.CloudSync,
            title = "CHECKING GOOGLE DRIVE…",
            subtitle = "Looking for an existing backup connection.",
            onClick = {},
            enabled = false,
        )
    } else if (!state.connected) {
        SettingsRow(
            icon = Icons.Filled.CloudOff,
            title = "CONNECT GOOGLE DRIVE",
            subtitle = "Daily 03:00 backups upload to your Drive's appDataFolder; keeps the last 7 snapshots.",
            onClick = onConnect,
            enabled = !state.running,
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBox(icon = Icons.Filled.CloudDone, tint = BrandLime)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "GOOGLE DRIVE CONNECTED",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Text(
                    text = state.lastBackupAt?.let { "Last backup: ${formatRelativeTime(it)}" }
                        ?: "Connected — daily backup will run at 03:00.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateSubtext,
                )
            }
            TextButton(onClick = onDisconnect, enabled = !state.running) {
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBox(icon = Icons.Filled.Restore)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = (snapshot.createdAt?.let { formatRelativeTime(it) } ?: snapshot.name).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = snapshot.sizeBytes?.let { "${it / 1024} KB · ${snapshot.name}" } ?: snapshot.name,
                style = MaterialTheme.typography.bodySmall,
                color = SlateSubtext,
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBox(icon = Icons.Filled.Visibility, tint = BrandLime)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "KEEP SCREEN AWAKE",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = "Prevents sleep during active workout sessions.",
                style = MaterialTheme.typography.bodySmall,
                color = SlateSubtext,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DarkOnPrimary,
                checkedTrackColor = BrandLime,
                uncheckedThumbColor = OliveSubtext,
                uncheckedTrackColor = SurfaceControl,
            ),
        )
    }
}

@Composable
private fun UnitsRow(unit: WeightUnit, onChange: (WeightUnit) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBox(icon = Icons.Filled.Scale, tint = BrandLime)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "WEIGHT UNITS",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = "Used for weight steppers and total-volume figures.",
                style = MaterialTheme.typography.bodySmall,
                color = SlateSubtext,
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceChip)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                WeightUnit.KG to ("KG" to "Use kilograms"),
                WeightUnit.LBS to ("LBS" to "Use pounds"),
            ).forEach { (option, labels) ->
                val (shortLabel, a11yLabel) = labels
                val selected = unit == option
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (selected) SurfaceElevated else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onChange(option) }
                        .semantics {
                            role = Role.RadioButton
                            contentDescription = a11yLabel
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = shortLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                        ),
                        color = if (selected) BrandLime else OliveSubtext,
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingTimerRow() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    SettingsRow(
        icon = Icons.Filled.Visibility,
        title = stringResource(R.string.timer_overlay_title).uppercase(),
        subtitle = stringResource(
            if (granted) R.string.timer_overlay_enabled else R.string.timer_overlay_disabled,
        ),
        onClick = {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
            )
        },
    )
}

@Composable
private fun NotificationRow() {
    val context = LocalContext.current
    val granted by rememberNotificationGranted()
    SettingsRow(
        icon = if (granted) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
        title = "TIMER NOTIFICATION",
        subtitle = if (granted) "Allowed — the workout timer notification is visible."
        else "Disabled — Android won't show the timer's persistent notification. Tap to fix in system settings.",
        onClick = {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        },
    )
}

@Composable
private fun rememberNotificationGranted(): State<Boolean> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember { mutableStateOf(checkNotificationGranted(context)) }
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBox(icon = Icons.Filled.Info)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "GYM TRACKER",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = "Version $version",
                style = MaterialTheme.typography.bodySmall,
                color = SlateSubtext,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(14.dp)
                .background(BrandLime),
        )
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            ),
            color = OliveSubtext,
        )
    }
}

@Composable
private fun IconBox(
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color = SlateSubtext,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceDeep),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBox(icon = icon)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SlateSubtext,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = OliveSubtext,
        )
    }
}
