package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.LoadingPane
import dev.francescolofranco.gymtracker.ui.motion.GymMotion
import dev.francescolofranco.gymtracker.ui.components.ErrorPane

@Composable
fun ExercisePickerSheet(
    /** IDs that should appear disabled (already added to the current session). */
    excludeIds: Set<Long> = emptySet(),
    onDismiss: () -> Unit,
    onPick: (ExerciseEntity) -> Unit,
    viewModel: ExercisePickerViewModel = hiltViewModel(),
) {
    val rowState by viewModel.rows.collectAsStateWithLifecycle()
    val rows = (rowState as? Loadable.Ready)?.value.orEmpty()
    var pickedIds by remember { mutableStateOf(emptySet<Long>()) }
    var filter by remember { mutableStateOf<PickerFilter>(PickerFilter.All) }

    val availableMuscles = remember(rows) {
        rows.flatMap { it.exercise.primaryMuscles }
            .distinct()
            .sortedBy { it.ordinal }
    }
    // Snapshot recency when the picker receives its first result. Adding an exercise to the active
    // session updates lastUsedAt; keeping this order fixed prevents rows jumping under the finger.
    val recentIds = remember(rows.isNotEmpty()) {
        rows.filter { it.lastUsedAt != null }.take(10).map { it.exercise.id }
    }
    val visibleExercises = remember(rows, filter, recentIds) {
        when (val selected = filter) {
            PickerFilter.All -> rows.map { it.exercise }.sortedBy { it.name.lowercase() }
            PickerFilter.Recent -> recentIds.mapNotNull { id ->
                rows.firstOrNull { it.exercise.id == id }?.exercise
            }
            is PickerFilter.ByMuscle -> rows.asSequence()
                .map { it.exercise }
                .filter { selected.muscle in it.primaryMuscles }
                .sortedBy { it.name.lowercase() }
                .toList()
        }
    }

    // Keep the picker open while several exercises are added. It is intentionally dismissed only
    // by its close button, so an accidental scrim tap or back press cannot discard the user's place.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f),
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

                when (val current = rowState) {
                    Loadable.Loading -> LoadingPane(modifier = Modifier.weight(1f))
                    is Loadable.Error -> ErrorPane(
                        current.message,
                        viewModel::retry,
                        Modifier.weight(1f),
                    )
                    is Loadable.Ready -> if (rows.isEmpty()) {
                        Text(
                            text = "Create exercises in the Exercises tab first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp, horizontal = 4.dp),
                        )
                    } else {
                        ExerciseFilters(
                            filter = filter,
                            availableMuscles = availableMuscles,
                            onFilter = { filter = it },
                        )

                        if (visibleExercises.isEmpty()) {
                            Text(
                                text = "No recently used exercises yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp, horizontal = 4.dp),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            ) {
                                items(items = visibleExercises, key = { it.id }) { e ->
                                    // Track the tap locally as well as relying on excludeIds.
                                    // Session persistence is asynchronous, so this closes the brief
                                    // window in which a double-tap could enqueue the exercise twice.
                                    val alreadyAdded = e.id in excludeIds || e.id in pickedIds
                                    PickerRow(
                                        exercise = e,
                                        alreadyAdded = alreadyAdded,
                                        onClick = {
                                            if (!alreadyAdded) {
                                                pickedIds += e.id
                                                onPick(e)
                                            }
                                        },
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = GymMotion.ItemFadeIn,
                                            placementSpec = GymMotion.ItemPlacement,
                                            fadeOutSpec = GymMotion.ItemFadeOut,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseFilters(
    filter: PickerFilter,
    availableMuscles: List<Muscle>,
    onFilter: (PickerFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = filter == PickerFilter.All,
            onClick = { onFilter(PickerFilter.All) },
            label = { Text("All") },
        )
        FilterChip(
            selected = filter == PickerFilter.Recent,
            onClick = { onFilter(PickerFilter.Recent) },
            label = { Text("Recent") },
        )
        availableMuscles.forEach { muscle ->
            val muscleFilter = PickerFilter.ByMuscle(muscle)
            FilterChip(
                selected = filter == muscleFilter,
                onClick = { onFilter(muscleFilter) },
                label = { Text(muscle.displayName) },
            )
        }
    }
}

private sealed interface PickerFilter {
    data object All : PickerFilter
    data object Recent : PickerFilter
    data class ByMuscle(val muscle: Muscle) : PickerFilter
}

@Composable
private fun PickerRow(
    exercise: ExerciseEntity,
    alreadyAdded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
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
                    if (exercise.isUnilateral) add("Unilateral")
                    add("${exercise.targetSets}×${exercise.repRangeMin}–${exercise.repRangeMax}${if (exercise.isUnilateral) "/side" else ""}")
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
