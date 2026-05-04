package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.domain.Muscle
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExercisePickerSheet(
    excludeIds: Set<Long> = emptySet(),
    onDismiss: () -> Unit,
    onPick: (ExerciseEntity) -> Unit,
    viewModel: ExercisePickerViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    var muscleFilter by remember { mutableStateOf<Muscle?>(null) }

    val filtered = remember(rows, muscleFilter, excludeIds) {
        rows
            .asSequence()
            .filter { it.exercise.id !in excludeIds }
            .filter { row ->
                val m = muscleFilter ?: return@filter true
                row.exercise.primaryMuscle == m || m in row.exercise.secondaryMuscles
            }
            .map { it.exercise }
            .toList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Add exercise", style = MaterialTheme.typography.titleLarge)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(
                    selected = muscleFilter == null,
                    onClick = { muscleFilter = null },
                    label = { Text("All") },
                )
                Muscle.entries.forEach { m ->
                    FilterChip(
                        selected = muscleFilter == m,
                        onClick = { muscleFilter = if (muscleFilter == m) null else m },
                        label = { Text(m.displayName) },
                    )
                }
            }

            HorizontalDivider()

            if (filtered.isEmpty()) {
                Text(
                    text = if (rows.isEmpty()) "Create exercises in the Exercises tab first."
                    else "No matching exercises.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f),
                ) {
                    items(items = filtered, key = { it.id }) { e ->
                        PickerRow(
                            exercise = e,
                            onClick = {
                                onPick(e)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(exercise: ExerciseEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Column {
            Text(text = exercise.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = buildList {
                    add(exercise.primaryMuscle.displayName)
                    if (exercise.isBodyweight) add("BW")
                    add("${exercise.targetSets}×${exercise.repRangeMin}–${exercise.repRangeMax}")
                }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
