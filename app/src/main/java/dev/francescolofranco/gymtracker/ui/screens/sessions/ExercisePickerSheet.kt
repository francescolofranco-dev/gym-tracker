package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.projections.ExerciseWithRecency
import dev.francescolofranco.gymtracker.domain.Muscle
import dev.francescolofranco.gymtracker.domain.MuscleCategory
import dev.francescolofranco.gymtracker.domain.sortedTopToBottom
import dev.francescolofranco.gymtracker.ui.components.ErrorPane
import dev.francescolofranco.gymtracker.ui.components.ExerciseSearchField
import dev.francescolofranco.gymtracker.ui.components.ExerciseTopToBottomComparator
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.LoadingPane
import dev.francescolofranco.gymtracker.ui.components.matchesExerciseQuery
import dev.francescolofranco.gymtracker.ui.motion.GymMotion
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.DarkOnPrimary
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.OliveSubtext
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard
import dev.francescolofranco.gymtracker.ui.theme.SurfaceChip
import dev.francescolofranco.gymtracker.ui.theme.SurfaceControl
import dev.francescolofranco.gymtracker.ui.theme.SurfaceDeep
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated
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
                .fillMaxHeight(0.88f)
                .border(1.dp, EditorialBorder, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = SurfaceCard,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.fillMaxHeight()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceChip),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "SELECT EXERCISE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceElevated),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

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
                            modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
                        )
                    } else {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ExerciseSearchField(
                                query = searchQuery,
                                onQueryChange = ::updateSearchQuery,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                            )

                            ExerciseFilters(
                                filter = filter,
                                availableCategories = availableCategories,
                                onFilter = ::updateFilter,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDeep)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "${visibleExercises.size} EXERCISE${if (visibleExercises.size == 1) "" else "S"} AVAILABLE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    letterSpacing = 1.0.sp,
                                ),
                                color = OliveSubtext,
                            )
                        }

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
                                itemsIndexed(items = visibleExercises, key = { _, it -> it.id }) { index, e ->
                                    // Reflect taps immediately while persistence catches up.
                                    val alreadyAdded = selectionOverrides[e.id] ?: (e.id in excludeIds)
                                    PickerRow(
                                        exercise = e,
                                        alreadyAdded = alreadyAdded,
                                        isAlternate = index % 2 == 1,
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
    availableCategories: List<MuscleCategory>,
    onFilter: (PickerFilter) -> Unit,
) {
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = BrandLime,
        selectedLabelColor = DarkOnPrimary,
        containerColor = SurfaceElevated,
        labelColor = OliveSubtext,
    )
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
            shape = RoundedCornerShape(12.dp),
            colors = chipColors,
            label = {
                Text(
                    text = "All",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                )
            },
        )
        availableCategories.forEach { category ->
            val categoryFilter = PickerFilter.ByCategory(category)
            FilterChip(
                selected = filter == categoryFilter,
                onClick = { onFilter(categoryFilter) },
                shape = RoundedCornerShape(12.dp),
                colors = chipColors,
                label = {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    )
                },
            )
        }
    }
}

internal sealed interface PickerFilter {
    data object All : PickerFilter
    data class ByCategory(val category: MuscleCategory) : PickerFilter
}

internal fun PickerFilter.saveKey(): String = when (this) {
    PickerFilter.All -> "all"
    is PickerFilter.ByCategory -> "category:${category.name}"
}

internal fun restorePickerFilter(key: String): PickerFilter = when {
    key.startsWith("category:") -> MuscleCategory.entries
        .firstOrNull { it.name == key.substringAfter("category:") }
        ?.let { PickerFilter.ByCategory(it) }
        ?: PickerFilter.All
    // Migrate process-restored state saved by versions that exposed one chip per muscle.
    key.startsWith("muscle:") -> Muscle.entries
        .firstOrNull { it.name == key.substringAfter("muscle:") }
        ?.let(MuscleCategory::containing)
        ?.let { PickerFilter.ByCategory(it) }
        ?: PickerFilter.All
    else -> PickerFilter.All
}

internal fun availablePickerCategories(
    rows: List<ExerciseWithRecency>,
    selected: MuscleCategory?,
): List<MuscleCategory> = MuscleCategory.entries.filter { category ->
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
    isAlternate: Boolean,
    canRemove: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowBg = when {
        alreadyAdded -> SurfaceControl
        isAlternate -> SurfaceDeep
        else -> SurfaceCard
    }
    val category = exercise.primaryMuscles.sortedTopToBottom()
        .firstOrNull()
        ?.let(MuscleCategory::containing)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable(enabled = !alreadyAdded || canRemove, onClick = onClick)
            .alpha(if (alreadyAdded && !canRemove) 0.45f else 1f)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = if (alreadyAdded) BrandLime else MaterialTheme.colorScheme.onSurface,
                )
                if (category != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (alreadyAdded) BrandLime else SurfaceElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = category.displayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = if (alreadyAdded) DarkOnPrimary else OliveSubtext,
                        )
                    }
                }
            }
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
                color = OliveSubtext,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (alreadyAdded) BrandLime else SurfaceElevated)
                .then(
                    if (!alreadyAdded) Modifier.border(1.dp, EditorialBorder, RoundedCornerShape(4.dp))
                    else Modifier,
                ),
        ) {
            if (alreadyAdded) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Already selected",
                    tint = DarkOnPrimary,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
