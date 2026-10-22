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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import dev.francescolofranco.gymtracker.data.db.projections.ExerciseWithRecency
import dev.francescolofranco.gymtracker.domain.Muscle
import dev.francescolofranco.gymtracker.domain.sortedTopToBottom
import dev.francescolofranco.gymtracker.ui.components.ErrorPane
import dev.francescolofranco.gymtracker.ui.components.ExerciseSearchField
import dev.francescolofranco.gymtracker.ui.components.ExerciseTopToBottomComparator
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.LoadingPane
import dev.francescolofranco.gymtracker.ui.components.matchesExerciseQuery
import dev.francescolofranco.gymtracker.ui.motion.GymMotion
import kotlinx.coroutines.launch

@Composable
fun ExercisePickerSheet(
    /** IDs already selected; tappable for removal when [onUnpick] is supplied. */
    excludeIds: Set<Long> = emptySet(),
    onDismiss: () -> Unit,
    onPick: (ExerciseEntity) -> Unit,
    onUnpick: ((ExerciseEntity) -> Unit)? = null,
    viewModel: ExercisePickerViewModel = hiltViewModel(),
) {
    val rowState by viewModel.rows.collectAsStateWithLifecycle()
    val rows = (rowState as? Loadable.Ready)?.value.orEmpty()
    var selectionOverrides by remember { mutableStateOf(emptyMap<Long, Boolean>()) }
    var filterKey by rememberSaveable { mutableStateOf(PickerFilter.All.saveKey()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filter = restorePickerFilter(filterKey)
    val exerciseListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun updateSearchQuery(query: String) {
        if (query == searchQuery) return
        searchQuery = query
        scope.launch { exerciseListState.scrollToItem(0) }
    }

    fun updateFilter(newFilter: PickerFilter) {
        if (newFilter == filter) return
        filterKey = newFilter.saveKey()
        scope.launch { exerciseListState.scrollToItem(0) }
    }

    fun resetSearchAndFilter() {
        searchQuery = ""
        filterKey = PickerFilter.All.saveKey()
        scope.launch { exerciseListState.scrollToItem(0) }
    }

    val availableCategories = remember(rows, filter) {
        availablePickerCategories(
            rows = rows,
            selected = (filter as? PickerFilter.ByCategory)?.category,
        )
    }
    val visibleExercises = remember(rows, filter, searchQuery) {
        filterPickerExercises(rows, filter, searchQuery)
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
                        ExerciseSearchField(
                            query = searchQuery,
                            onQueryChange = ::updateSearchQuery,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, end = 8.dp),
                        )

                        ExerciseFilters(
                            filter = filter,
                            availableCategories = availableCategories,
                            onFilter = ::updateFilter,
                        )

                        if (visibleExercises.isEmpty()) {
                            PickerNoSearchResults(
                                query = searchQuery,
                                filter = filter,
                                onReset = ::resetSearchAndFilter,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            )
                        } else {
                            LazyColumn(
                                state = exerciseListState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            ) {
                                items(items = visibleExercises, key = { it.id }) { e ->
                                    // Reflect taps immediately while persistence catches up.
                                    val alreadyAdded = selectionOverrides[e.id] ?: (e.id in excludeIds)
                                    PickerRow(
                                        exercise = e,
                                        alreadyAdded = alreadyAdded,
                                        canRemove = onUnpick != null,
                                        onClick = {
                                            if (alreadyAdded) {
                                                onUnpick?.let { remove ->
                                                    selectionOverrides = selectionOverrides + (e.id to false)
                                                    remove(e)
                                                }
                                            } else {
                                                selectionOverrides = selectionOverrides + (e.id to true)
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
private fun PickerNoSearchResults(
    query: String,
    filter: PickerFilter,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 24.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = when {
                query.isBlank() && filter is PickerFilter.ByCategory ->
                    "No exercises in ${filter.category.displayName}."
                query.isBlank() -> "No exercises in this filter."
                filter is PickerFilter.ByCategory ->
                    "No exercises in ${filter.category.displayName} match \"${query.trim()}\""
                else -> "No exercises match \"${query.trim()}\""
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onReset) {
            Text(
                when {
                    query.isNotBlank() && filter == PickerFilter.All -> "Clear search"
                    query.isBlank() -> "Show all exercises"
                    else -> "Clear search and filter"
                },
            )
        }
    }
}

@Composable
private fun ExerciseFilters(
    filter: PickerFilter,
    availableCategories: List<PickerCategory>,
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
        availableCategories.forEach { category ->
            val categoryFilter = PickerFilter.ByCategory(category)
            FilterChip(
                selected = filter == categoryFilter,
                onClick = { onFilter(categoryFilter) },
                label = { Text(category.displayName) },
            )
        }
    }
}

internal sealed interface PickerFilter {
    data object All : PickerFilter
    data class ByCategory(val category: PickerCategory) : PickerFilter
}

internal fun PickerFilter.saveKey(): String = when (this) {
    PickerFilter.All -> "all"
    is PickerFilter.ByCategory -> "category:${category.name}"
}

internal fun restorePickerFilter(key: String): PickerFilter = when {
    key.startsWith("category:") -> PickerCategory.entries
        .firstOrNull { it.name == key.substringAfter("category:") }
        ?.let { PickerFilter.ByCategory(it) }
        ?: PickerFilter.All
    // Migrate process-restored state saved by versions that exposed one chip per muscle.
    key.startsWith("muscle:") -> Muscle.entries
        .firstOrNull { it.name == key.substringAfter("muscle:") }
        ?.let(PickerCategory::containing)
        ?.let { PickerFilter.ByCategory(it) }
        ?: PickerFilter.All
    else -> PickerFilter.All
}

internal fun availablePickerCategories(
    rows: List<ExerciseWithRecency>,
    selected: PickerCategory?,
): List<PickerCategory> = PickerCategory.entries.filter { category ->
    category == selected || rows.any { category.containsAny(it.exercise.primaryMuscles) }
}

internal fun filterPickerExercises(
    rows: List<ExerciseWithRecency>,
    filter: PickerFilter,
    query: String,
): List<ExerciseEntity> {
    val filtered = when (filter) {
        PickerFilter.All -> rows.map { it.exercise }.sortedWith(ExerciseTopToBottomComparator)
        is PickerFilter.ByCategory -> rows.asSequence()
            .map { it.exercise }
            .filter { filter.category.containsAny(it.primaryMuscles) }
            .sortedWith(ExerciseTopToBottomComparator)
            .toList()
    }
    return filtered.filter { it.matchesExerciseQuery(query) }
}

@Composable
private fun PickerRow(
    exercise: ExerciseEntity,
    alreadyAdded: Boolean,
    canRemove: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !alreadyAdded || canRemove, onClick = onClick)
            .alpha(if (alreadyAdded && !canRemove) 0.45f else 1f)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = exercise.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = buildList {
                    val primaries = exercise.primaryMuscles.sortedTopToBottom()
                        .joinToString(" + ") { it.displayName }
                    if (primaries.isNotEmpty()) add(primaries)
                    if (exercise.isBodyweight) add("BW")
                    if (exercise.isUnilateral) add("Unilateral")
                    add("${exercise.targetSets}×${exercise.repRangeMin}–${exercise.repRangeMax}${if (exercise.isUnilateral) "/side" else ""}")
                    val secondaries = exercise.secondaryMuscles.sortedTopToBottom()
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
                contentDescription = "Already selected",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
