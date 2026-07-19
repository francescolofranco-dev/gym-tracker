package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.domain.Muscle

@Composable
fun ExercisePickerSheet(
    /** IDs that should appear disabled (already added to the current session). */
    excludeIds: Set<Long> = emptySet(),
    onDismiss: () -> Unit,
    onPick: (ExerciseEntity) -> Unit,
    viewModel: ExercisePickerViewModel = hiltViewModel(),
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()

    /**
     * Group by the "lead" primary muscle (lowest enum ordinal of the exercise's primary set)
     * and sort alphabetically within each group. Exercises already in the session stay
     * visible but become unselectable, with a checkmark badge.
     */
    val grouped = remember(rows) {
        rows.map { it.exercise }
            .groupBy { e -> e.primaryMuscles.minByOrNull { it.ordinal } ?: Muscle.CORE }
            .toSortedMap(compareBy { it.ordinal })
            .mapValues { (_, items) -> items.sortedBy { it.name.lowercase() } }
    }

    // A centred modal dialog rather than a bottom sheet: the previous sheet dragged itself shut
    // when the user scrolled the list to hunt for an exercise. A dialog only closes on an explicit
    // action (the close button, a back press, or a scrim tap).
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Select exercise",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))

                if (grouped.isEmpty()) {
                    Text(
                        text = "Create exercises in the Exercises tab first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp, horizontal = 4.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp),
                    ) {
                        grouped.forEach { (muscle, items) ->
                            item(key = "h-${muscle.name}") {
                                MuscleHeader(muscle = muscle)
                            }
                            items(items = items, key = { it.id }) { e ->
                                val alreadyAdded = e.id in excludeIds
                                PickerRow(
                                    exercise = e,
                                    alreadyAdded = alreadyAdded,
                                    onClick = {
                                        if (!alreadyAdded) {
                                            onPick(e)
                                            onDismiss()
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MuscleHeader(muscle: Muscle) {
    Text(
        text = muscle.displayName,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun PickerRow(
    exercise: ExerciseEntity,
    alreadyAdded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !alreadyAdded, onClick = onClick)
            .alpha(if (alreadyAdded) 0.45f else 1f)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = exercise.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = buildList {
                    if (exercise.isBodyweight) add("BW")
                    add("${exercise.targetSets}×${exercise.repRangeMin}–${exercise.repRangeMax}")
                    val secondaries = exercise.secondaryMuscles.sortedBy { it.ordinal }
                        .joinToString(", ") { it.displayName }
                    if (secondaries.isNotEmpty()) add(secondaries)
                }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (alreadyAdded) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Already in session",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
