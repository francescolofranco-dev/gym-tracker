package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

@Composable
internal fun ExerciseNoteDialog(
    title: String,
    initial: String,
    initiallyPinned: Boolean,
    onCancel: () -> Unit,
    onConfirm: (notes: String, isPinned: Boolean) -> Unit,
) {
    var value by remember(initial) { mutableStateOf(initial) }
    var isPinned by remember(initiallyPinned) { mutableStateOf(initiallyPinned) }

    AlertDialog(
        onDismissRequest = onCancel,
        shape = dev.francescolofranco.gymtracker.ui.theme.DialogShape,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        if (it.isBlank()) isPinned = false
                    },
                    placeholder = { Text("Add a note") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = isPinned,
                            enabled = value.isNotBlank(),
                            role = Role.Checkbox,
                            onValueChange = { isPinned = it },
                        ),
                ) {
                    Checkbox(
                        checked = isPinned,
                        onCheckedChange = null,
                        enabled = value.isNotBlank(),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                    ) {
                        Text("Pin this note", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (isPinned) {
                                "It will appear every time you do this exercise."
                            } else {
                                "Unpinned notes remain through the next time you do this exercise."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value, isPinned) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}
