package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.ui.theme.ButtonShape

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
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
        ) {
            // Keep the actions visible while the fields scroll above the keyboard.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Exercise notes", style = MaterialTheme.typography.titleLarge)
                    Text(
                        exerciseName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                NoteField(
                    title = "Pinned note",
                    description = "Shown every time you do this exercise.",
                    placeholder = "Seat height, grip, technique…",
                    icon = Icons.Filled.PushPin,
                    isPinned = true,
                    value = pinnedValue,
                    onValueChange = { pinnedValue = it },
                )
                NoteField(
                    title = "Session note",
                    description = "Carries forward for one session.",
                    placeholder = "Something to remember next time…",
                    icon = Icons.AutoMirrored.Filled.Notes,
                    isPinned = false,
                    value = value,
                    onValueChange = { value = it },
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onConfirm(value, pinnedValue) },
                    shape = ButtonShape,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text("Save notes")
                }
            }
        }
    }
}

@Composable
private fun NoteField(
    title: String,
    description: String,
    placeholder: String,
    icon: ImageVector,
    isPinned: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPinned) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = title },
        )
    }
}
