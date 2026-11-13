package dev.francescolofranco.gymtracker.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.R
import dev.francescolofranco.gymtracker.data.backup.drive.DriveSnapshot
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.ui.components.AppLogo
import dev.francescolofranco.gymtracker.ui.components.LogoVariant
import dev.francescolofranco.gymtracker.ui.components.ErrorPane
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.LoadingPane
import dev.francescolofranco.gymtracker.ui.motion.GymMotion
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.DarkOnPrimary
import dev.francescolofranco.gymtracker.ui.theme.DarkOnSurface
import dev.francescolofranco.gymtracker.ui.theme.DestructiveCoral
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.OliveSubtext
import dev.francescolofranco.gymtracker.ui.theme.OswaldFontFamily
import dev.francescolofranco.gymtracker.ui.theme.SlateSubtext
import dev.francescolofranco.gymtracker.ui.theme.SpaceGroteskFontFamily
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

private const val MAX_VISIBLE_SNAPSHOTS = 4
private val SNAPSHOT_ROW_HEIGHT = 38.dp
private val SNAPSHOT_DIVIDER_HEIGHT = 1.dp
private val SNAPSHOT_LIST_HEIGHT =
    SNAPSHOT_ROW_HEIGHT * MAX_VISIBLE_SNAPSHOTS + SNAPSHOT_DIVIDER_HEIGHT * (MAX_VISIBLE_SNAPSHOTS - 1)

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
    var showAboutPopup by remember { mutableStateOf(false) }

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

    Scaffold(
        containerColor = SurfaceBase,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (settings == null) {
            LoadingPane(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        val recentSnapshots = remember(driveState.snapshots) {
            driveState.snapshots.take(MAX_VISIBLE_SNAPSHOTS)
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "settings_top_header") {
                SettingsTopHeader()
            }

            item(key = "display_section") {
                Column {
                    SectionHeader(
                        title = "TRAINING & DISPLAY PREFERENCES",
                        rightLabel = "Calibration",
                        rightLabelColor = BrandLime,
                    )
                    SettingsGroupCard {
                        UnitsRow(unit = settings.unit, onChange = settingsViewModel::setUnit)
                        SettingsGroupDivider()
                        KeepScreenOnRow(
                            enabled = settings.keepScreenOnDuringSession,
                            onChange = settingsViewModel::setKeepScreenOnDuringSession,
                        )
                        SettingsGroupDivider()
                        FloatingTimerRow(
                            enabled = settings.timerOverlayEnabled,
                            onChange = settingsViewModel::setTimerOverlayEnabled,
                        )
                    }
                }
            }

            item(key = "workout_section") {
                Column {
                    SectionHeader(
                        title = "TEMPLATES & WORKOUT",
                        rightLabel = "Routines & Alerts",
                    )
                    SettingsGroupCard {
                        SettingsRow(
                            icon = Icons.AutoMirrored.Filled.ListAlt,
                            iconTint = BrandLime,
                            title = "WORKOUT TEMPLATES",
                            subtitle = "Create, edit, or reorder saved split routines",
                            badgeText = "MANAGE",
                            badgeColor = BrandLime,
                            onClick = onOpenTemplates,
                        )
                        if (backupState.hasRecovery) {
                            SettingsGroupDivider()
                            SettingsRow(
                                icon = Icons.Filled.Restore,
                                iconTint = DestructiveCoral,
                                title = "UNDO LAST RESTORE",
                                subtitle = "Recover the automatic safety copy made before the last restore",
                                trailingIcon = Icons.Filled.WarningAmber,
                                trailingIconTint = DestructiveCoral,
                                onClick = { confirmRecovery = true },
                                enabled = !backupState.running,
                            )
                        }
                        SettingsGroupDivider()
                        NotificationRow()
                    }
                }
            }

            item(key = "data_backup_section") {
                Column(
                    modifier = Modifier.animateItem(
                        fadeInSpec = GymMotion.ItemFadeIn,
                        placementSpec = GymMotion.ItemPlacement,
                        fadeOutSpec = GymMotion.ItemFadeOut,
                    ),
                ) {
                    SectionHeader(
                        title = "DATA & BACKUP",
                        rightLabel = if (driveState.connected) "Encrypted Sync" else "Local & Cloud Vault",
                        rightLabelColor = BrandLime,
                        showRightDot = driveState.connected,
                    )
                    SettingsGroupCard {
                        DriveAccountRow(
                            state = driveState,
                            onConnect = driveViewModel::connect,
                            onDisconnect = driveViewModel::disconnect,
                            onBackupNow = driveViewModel::backupNow,
                        )
                        SettingsGroupDivider()
                        SettingsRow(
                            icon = Icons.Filled.CloudUpload,
                            iconTint = BrandLime,
                            title = "EXPORT WORKOUT DATA",
                            subtitle = "Save full workout history as JSON snapshot",
                            trailingLabel = "JSON",
                            trailingIcon = Icons.Filled.CloudUpload,
                            trailingIconTint = OliveSubtext,
                            onClick = { createDoc.launch(backupViewModel.suggestedFilename()) },
                            enabled = !backupState.running,
                        )
                        SettingsGroupDivider()
                        SettingsRow(
                            icon = Icons.Filled.CloudDownload,
                            iconTint = DestructiveCoral,
                            title = "RESTORE FROM BACKUP",
                            subtitle = "Overwrite local database from backup file",
                            trailingIcon = Icons.Filled.WarningAmber,
                            trailingIconTint = DestructiveCoral,
                            onClick = { openDoc.launch(arrayOf("application/json", "text/plain", "*/*")) },
                            enabled = !backupState.running,
                        )
                    }
                }
            }

            if (driveState.connected || driveState.checkingConnection) {
                item(key = "drive_snapshots_section") {
                    val isLoadingSnapshots = driveState.checkingConnection || driveState.loadingSnapshots
                    Column {
                        SectionHeader(
                            title = "RECENT SNAPSHOTS",
                            rightLabel = when {
                                isLoadingSnapshots -> "Syncing…"
                                recentSnapshots.isEmpty() -> "Awaiting Sync"
                                else -> "Last ${recentSnapshots.size} of $MAX_VISIBLE_SNAPSHOTS"
                            },
                        )
                        SettingsGroupCard {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(SNAPSHOT_LIST_HEIGHT),
                            ) {
                                when {
                                    isLoadingSnapshots -> {
                                        SnapshotsLoadingPlaceholder()
                                    }
                                    recentSnapshots.isEmpty() -> {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            IconBox(icon = Icons.Filled.CloudSync, tint = OliveSubtext)
                                            Text(
                                                text = "No Drive snapshots yet — daily 03:00 backups or tapping sync above will populate them.",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = SpaceGroteskFontFamily,
                                                    fontSize = 12.sp,
                                                ),
                                                color = OliveSubtext,
                                            )
                                        }
                                    }
                                    else -> {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            for (index in 0 until MAX_VISIBLE_SNAPSHOTS) {
                                                if (index > 0) {
                                                    SettingsGroupDivider()
                                                }
                                                val snap = recentSnapshots.getOrNull(index)
                                                if (snap != null) {
                                                    SnapshotRow(
                                                        snapshot = snap,
                                                        isLatest = index == 0,
                                                        onClick = { driveViewModel.inspectRestore(snap) },
                                                        enabled = !driveState.running,
                                                    )
                                                } else {
                                                    EmptySnapshotSlotRow()
                                                }
                                            }
                                        }
                                    }
                                }
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
                    SectionHeader(
                        title = "APP INFO & DIAGNOSTICS",
                        rightLabel = "Production Build",
                    )
                    SettingsGroupCard {
                        AboutRow(onClick = { showAboutPopup = true })
                    }
                }
            }

            item(key = "settings_footer") {
                SettingsFooter()
            }
        }
    }

    if (showAboutPopup) {
        GymTrackerAboutDialog(onDismiss = { showAboutPopup = false })
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = OswaldFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                ),
                color = DarkOnSurface,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceControl)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "V$version",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                    ),
                    color = BrandLime,
                )
            }
        }
        Text(
            text = "Kinetic Discipline System • Calibration",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 12.sp,
            ),
            color = OliveSubtext,
        )
    }
}

@Composable
private fun SettingsGroupCard(
    horizontalPadding: Dp = 16.dp,
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
    onBackupNow: () -> Unit,
) {
    if (state.checkingConnection) {
        SettingsRow(
            icon = Icons.Filled.CloudSync,
            iconTint = BrandLime,
            title = "CHECKING GOOGLE DRIVE…",
            subtitle = "Looking for an existing backup connection",
            onClick = {},
            enabled = false,
        )
    } else if (!state.connected) {
        SettingsRow(
            icon = Icons.Filled.CloudOff,
            iconTint = BrandLime,
            title = "GOOGLE DRIVE SYNC",
            subtitle = "Daily 03:00 backups to your Drive appDataFolder",
            badgeText = "CONNECT",
            badgeColor = BrandLime,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "GOOGLE DRIVE SYNC",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = OswaldFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.45.sp,
                        ),
                        color = DarkOnSurface,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(SurfaceChip)
                            .clickable(enabled = !state.running, onClick = onDisconnect)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "DISCONNECT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                            ),
                            color = OliveSubtext,
                        )
                    }
                }
                Text(
                    text = state.lastBackupAt?.let { "Last backup: ${formatRelativeTime(it)} • Auto-sync ON" }
                        ?: "Connected • Daily backup at 03:00",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 12.sp,
                    ),
                    color = OliveSubtext,
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceChip)
                    .clickable(enabled = !state.running, onClick = onBackupNow)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Backup now to Google Drive"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = null,
                    tint = BrandLime,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SnapshotRow(
    snapshot: DriveSnapshot,
    isLatest: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SNAPSHOT_ROW_HEIGHT)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Restore,
            contentDescription = null,
            tint = if (isLatest) BrandLime else OliveSubtext,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = (snapshot.createdAt?.let { formatRelativeTime(it) } ?: snapshot.name).uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = OswaldFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
            ),
            color = DarkOnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (isLatest) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceChip)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text(
                    text = "LATEST",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                    ),
                    color = BrandLime,
                )
            }
        }
        Text(
            text = formatSnapshotMeta(snapshot),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 11.sp,
            ),
            color = OliveSubtext,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(SurfaceChip)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = "RESTORE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                ),
                color = DarkOnSurface,
            )
        }
    }
}

@Composable
private fun EmptySnapshotSlotRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SNAPSHOT_ROW_HEIGHT)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(SurfaceControl),
        )
        Text(
            text = "Available backup slot",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 11.sp,
            ),
            color = OliveSubtext.copy(alpha = 0.5f),
            maxLines = 1,
        )
    }
}

@Composable
private fun SnapshotsLoadingPlaceholder() {
    val transition = rememberInfiniteTransition(label = "snapshots_loading")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "snapshots_pulse_alpha",
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            for (index in 0 until MAX_VISIBLE_SNAPSHOTS) {
                if (index > 0) {
                    SettingsGroupDivider()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SNAPSHOT_ROW_HEIGHT)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SurfaceChip.copy(alpha = pulseAlpha)),
                    )
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SurfaceChip.copy(alpha = pulseAlpha)),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SurfaceChip.copy(alpha = pulseAlpha)),
                    )
                }
            }
        }
        CircularProgressIndicator(
            modifier = Modifier
                .size(22.dp)
                .align(Alignment.Center),
            color = BrandLime,
            strokeWidth = 2.dp,
        )
    }
}

private fun formatSnapshotMeta(snapshot: DriveSnapshot): String {
    val timestamp = snapshot.createdAt?.let {
        DateTimeFormatter.ofPattern("d MMM, HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(it)
    }
    val size = snapshot.sizeBytes?.let { "${(it / 1024).coerceAtLeast(1)} KB" }
    return when {
        timestamp != null && size != null -> "$timestamp • $size"
        timestamp != null -> timestamp
        size != null -> "$size • ${snapshot.name}"
        else -> snapshot.name
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
    SettingsSwitchRow(
        title = "KEEP SCREEN AWAKE",
        subtitle = "Prevent display sleep during active workout session",
        enabled = enabled,
        onChange = onChange,
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = OswaldFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.45.sp,
                ),
                color = DarkOnSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp,
                ),
                color = OliveSubtext,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onChange,
            modifier = Modifier.semantics { contentDescription = title },
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "WEIGHT UNITS",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = OswaldFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.45.sp,
                ),
                color = DarkOnSurface,
            )
            Text(
                text = "Primary load measurement across all templates",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp,
                ),
                color = OliveSubtext,
            )
        }
        Row(
            modifier = Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(SurfaceChip)
                .padding(4.dp),
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
                        .height(32.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (selected) SurfaceElevated else Color.Transparent)
                        .clickable { onChange(option) }
                        .semantics {
                            role = Role.RadioButton
                            contentDescription = a11yLabel
                        }
                        .padding(horizontal = 14.dp),
                ) {
                    Text(
                        text = shortLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 15.sp,
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
private fun FloatingTimerRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
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
    val openPermissionSettings = {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
        )
    }
    SettingsSwitchRow(
        title = stringResource(R.string.timer_overlay_title).uppercase(),
        subtitle = stringResource(R.string.timer_overlay_description),
        enabled = enabled,
        onChange = { show ->
            onChange(show)
            if (show && !granted) openPermissionSettings()
        },
    )
    if (enabled && !granted) {
        SettingsRow(
            icon = Icons.Filled.Visibility,
            iconTint = OliveSubtext,
            title = stringResource(R.string.timer_overlay_permission),
            subtitle = stringResource(R.string.timer_overlay_disabled),
            badgeText = "ALLOW",
            onClick = openPermissionSettings,
        )
    }
}

@Composable
private fun NotificationRow() {
    val context = LocalContext.current
    val granted by rememberNotificationGranted()
    SettingsRow(
        icon = if (granted) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
        iconTint = if (granted) BrandLime else DestructiveCoral,
        title = "TIMER NOTIFICATION",
        subtitle = if (granted) "Allowed — persistent rest timer notification active"
        else "Disabled — tap to enable rest timer alerts in system settings",
        badgeText = if (granted) "ALLOWED" else "BLOCKED",
        badgeColor = if (granted) BrandLime else DestructiveCoral,
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
private fun AboutRow(onClick: () -> Unit) {
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
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "Gym Tracker system manifesto and diagnostics"
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppLogo(
            size = 36.dp,
            showContainer = true,
            interactive = false,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "GYM TRACKER",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = OswaldFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.45.sp,
                ),
                color = DarkOnSurface,
            )
            Text(
                text = "Kinetic Brutalist Framework • Tap to inspect engine",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp,
                ),
                color = OliveSubtext,
            )
        }
        Text(
            text = "v$version",
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = BrandLime,
        )
    }
}

@Composable
private fun SettingsFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AppLogo(
                size = 16.dp,
                variant = LogoVariant.Monochrome,
                monochromeTint = OliveSubtext,
                interactive = false,
            )
            Text(
                text = "CRAFTED WITH CARE. BUILT FOR YOUR PROGRESS.",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                ),
                color = OliveSubtext,
            )
        }
        Text(
            text = "Your personal gym tracker. Always offline.",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 12.sp,
            ),
            color = OliveSubtext.copy(alpha = 0.7f),
        )
    }
}

private data class TrainingTip(
    val title: String,
    val message: String,
)

private val TRAINING_TIPS = listOf(
    TrainingTip(
        title = "Track your progress",
        message = "Log your sets, reps, and weights to see how your training changes over time.",
    ),
    TrainingTip(
        title = "Keep a backup",
        message = "Your workouts are saved on this device. Back them up to a file or Google Drive from Settings.",
    ),
    TrainingTip(
        title = "Review your training",
        message = "Check your exercise history and muscle coverage to help plan your next workout.",
    ),
    TrainingTip(
        title = "Build a routine",
        message = "Set a weekly session goal and use templates to make getting started easier.",
    ),
)

@Composable
private fun GymTrackerAboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            pi.versionName ?: "?"
        }.getOrDefault("?")
    }
    var tipIndex by remember { mutableIntStateOf(0) }
    val currentTip = TRAINING_TIPS[tipIndex]

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceCard)
                .border(1.dp, BrandLime.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(SurfaceChip)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "BUILD v$version",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = OliveSubtext,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppLogo(
                    size = 42.dp,
                    showContainer = true,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "GYM TRACKER",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = OswaldFontFamily,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp,
                        ),
                        color = DarkOnSurface,
                    )
                    Text(
                        text = "Log your workouts, follow your progress, and plan your next session.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 12.sp,
                        ),
                        color = SlateSubtext,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceDeep)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DiagnosticMetricCell(
                    label = "1RM FORMULA",
                    value = "EPLEY (≤12 REPS)",
                    modifier = Modifier.weight(1f),
                )
                DiagnosticMetricCell(
                    label = "VOLUME MODEL",
                    value = "1.0x / 0.5x SPLIT",
                    modifier = Modifier.weight(1f),
                )
            }

            // Tap the card to cycle through training tips.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, EditorialBorder, RoundedCornerShape(4.dp))
                    .clickable(onClickLabel = "Show next tip", role = Role.Button) {
                        tipIndex = (tipIndex + 1) % TRAINING_TIPS.size
                    }
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = currentTip.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp,
                        ),
                        color = BrandLime,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = OliveSubtext,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "${tipIndex + 1} / ${TRAINING_TIPS.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = OliveSubtext,
                        )
                    }
                }
                Text(
                    text = currentTip.message,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = OswaldFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp,
                        lineHeight = 21.sp,
                    ),
                    color = DarkOnSurface,
                )
                Text(
                    text = "Tap for another tip",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 11.sp,
                    ),
                    color = OliveSubtext,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BrandLime)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "BACK TO TRAINING",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = OswaldFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        ),
                        color = DarkOnPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticMetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = DarkOnSurface,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            ),
            color = OliveSubtext,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = OswaldFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            ),
            color = valueColor,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    rightLabel: String? = null,
    rightLabelColor: Color = OliveSubtext,
    showRightDot: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            ),
            color = OliveSubtext,
        )
        if (rightLabel != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (showRightDot) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BrandLime),
                    )
                }
                Text(
                    text = rightLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                    ),
                    color = rightLabelColor,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun IconBox(
    icon: ImageVector,
    tint: Color = BrandLime,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(SurfaceChip),
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
    iconTint: Color = BrandLime,
    badgeText: String? = null,
    badgeColor: Color = BrandLime,
    trailingLabel: String? = null,
    trailingIcon: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
    trailingIconTint: Color = OliveSubtext,
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
        IconBox(icon = icon, tint = iconTint)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = OswaldFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.45.sp,
                ),
                color = DarkOnSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp,
                ),
                color = OliveSubtext,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(SurfaceChip)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                        ),
                        color = badgeColor,
                    )
                }
            }
            if (trailingLabel != null) {
                Text(
                    text = trailingLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = DarkOnSurface,
                )
            }
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = trailingIconTint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
