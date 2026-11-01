package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.ButtonShape
import dev.francescolofranco.gymtracker.ui.theme.DarkOnPrimary
import dev.francescolofranco.gymtracker.ui.theme.DarkPrimaryContainer
import dev.francescolofranco.gymtracker.ui.theme.DestructiveCoral
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.OliveSubtext
import dev.francescolofranco.gymtracker.ui.theme.SlateSubtext
import dev.francescolofranco.gymtracker.ui.theme.SurfaceBase
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard
import dev.francescolofranco.gymtracker.ui.theme.SurfaceControl
import dev.francescolofranco.gymtracker.ui.theme.SurfaceDeep
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExerciseNoteSheet(
    exerciseName: String,
    initial: String,
    initialPinned: String,
    onCancel: () -> Unit,
    onConfirm: (notes: String, pinnedNotes: String) -> Unit,
) {
    var value by rememberSaveable(exerciseName, initial) { mutableStateOf(initial) }
    var pinnedValue by rememberSaveable(exerciseName, initialPinned) { mutableStateOf(initialPinned) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            // Keep the actions visible while the fields scroll above the keyboard.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "EXERCISE NOTES",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            ),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FitnessCenter,
                                contentDescription = null,
                                tint = BrandLime,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = exerciseName.uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp,
                                ),
                                color = OliveSubtext,
                            )
                        }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceElevated)
                            .clickable(
                                role = Role.Button,
                                onClick = onCancel,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancel",
                            tint = OliveSubtext,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                NoteField(
                    title = "Pinned note",
                    pillText = "PINNED SETUP NOTE",
                    badge = "EVERGREEN",
                    description = "Permanent setup & technique cue. Shown every time you do this exercise.",
                    placeholder = "Seat height, grip, technique…",
                    icon = Icons.Filled.PushPin,
                    isPinned = true,
                    value = pinnedValue,
                    onValueChange = { pinnedValue = it },
                )
                NoteField(
                    title = "Session note",
                    pillText = "NEXT SESSION ONLY",
                    badge = "AUTO-ARCHIVE",
                    description = "Carries forward to the immediate next workout, then automatically archives.",
                    placeholder = "Something to remember next time…",
                    icon = Icons.Filled.Schedule,
                    isPinned = false,
                    value = value,
                    onValueChange = { value = it },
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onCancel,
                    shape = ButtonShape,
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .clip(ButtonShape)
                        .background(SurfaceControl)
                        .padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = "CANCEL",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Button(
                    onClick = { onConfirm(value, pinnedValue) },
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandLime,
                        contentColor = DarkOnPrimary,
                    ),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "SAVE NOTES",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteField(
    title: String,
    pillText: String,
    badge: String,
    description: String,
    placeholder: String,
    icon: ImageVector,
    isPinned: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceElevated)
            .padding(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isPinned) DarkPrimaryContainer else SurfaceControl)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPinned) BrandLime else SlateSubtext,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = pillText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                    ),
                    color = if (isPinned) BrandLime else SlateSubtext,
                )
            }
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                ),
                color = if (isPinned) BrandLime else DestructiveCoral,
            )
        }
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = OliveSubtext,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDeep,
                unfocusedContainerColor = SurfaceDeep,
                unfocusedBorderColor = EditorialBorder,
                focusedBorderColor = BrandLime,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = title },
        )
    }
}
