package dev.francescolofranco.gymtracker.ui.screens.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.domain.MuscleCategory
import dev.francescolofranco.gymtracker.ui.components.ErrorPane
import dev.francescolofranco.gymtracker.ui.components.ExerciseSearchField
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.LoadingPane
import dev.francescolofranco.gymtracker.ui.components.matchesExerciseQuery
import dev.francescolofranco.gymtracker.ui.motion.GymMotion
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.DarkOnPrimary
import dev.francescolofranco.gymtracker.ui.theme.OliveSubtext
import dev.francescolofranco.gymtracker.ui.theme.SlateSubtext
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard
import dev.francescolofranco.gymtracker.ui.theme.SurfaceControl
import dev.francescolofranco.gymtracker.ui.theme.SurfaceDeep
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated
import dev.francescolofranco.gymtracker.ui.theme.TagShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ExercisesScreen(
    onOpenDetail: (Long) -> Unit = {},
    viewModel: ExercisesViewModel = hiltViewModel(),
) {
    val grouped by viewModel.grouped.collectAsStateWithLifecycle()
    val editing by viewModel.editing.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var deleteConfirmTarget by remember { mutableStateOf<ExerciseEntity?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCategory = remember(selectedCategoryName) {
        selectedCategoryName?.let { name -> MuscleCategory.entries.firstOrNull { it.name == name } }
    }
    val exerciseListState = rememberLazyListState()

    fun updateSearchQuery(query: String) {
        if (query == searchQuery) return
        searchQuery = query
        scope.launch { exerciseListState.scrollToItem(0) }
    }

    fun updateCategory(category: MuscleCategory?) {
        val nextName = category?.name
        if (nextName == selectedCategoryName) return
        selectedCategoryName = nextName
        scope.launch { exerciseListState.scrollToItem(0) }
    }

    fun deleteWithUndo(target: ExerciseEntity) {
        viewModel.softDelete(target.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Deleted ${target.name}",
                actionLabel = "Undo",
                withDismissAction = true,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restore(target.id)
            }
        }
    }

    Scaffold(
        containerColor = SurfaceDeep,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openCreate() },
                shape = RoundedCornerShape(12.dp),
                containerColor = BrandLime,
                contentColor = DarkOnPrimary,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = {
                    Text(
                        text = "NEW EXERCISE",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.75.sp,
                        ),
                    )
                },
            )
        },
    ) { padding ->
        when (val current = grouped) {
            Loadable.Loading -> LoadingPane(modifier = Modifier.padding(padding))
            is Loadable.Error -> ErrorPane(current.message, viewModel::retry, Modifier.padding(padding))
            is Loadable.Ready -> {
                if (current.value.isEmpty()) {
                    EmptyState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                    )
                } else {
                    val matchingGroups = remember(current.value, searchQuery, selectedCategory) {
                        val byCat = if (selectedCategory == null) {
                            current.value
                        } else {
                            current.value.filterKeys { it == selectedCategory }
                        }
                        if (searchQuery.isBlank()) {
                            byCat
                        } else {
                            byCat
                                .mapValues { (_, exercises) ->
                                    exercises.filter { it.matchesExerciseQuery(searchQuery) }
                                }
                                .filterValues { it.isNotEmpty() }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    ) {
                        ExerciseSearchField(
                            query = searchQuery,
                            onQueryChange = ::updateSearchQuery,
                            containerColor = SurfaceControl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                        )

                        LibraryCategoryChipRow(
                            selected = selectedCategory,
                            onSelect = ::updateCategory,
                        )

                        if (matchingGroups.isEmpty()) {
                            NoSearchResults(
                                query = searchQuery,
                                onClear = {
                                    updateSearchQuery("")
                                    updateCategory(null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(24.dp),
                            )
                        } else {
                            LazyColumn(
                                state = exerciseListState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(bottom = 96.dp),
                            ) {
                                matchingGroups.forEach { (category, items) ->
                                    stickyHeader(key = "header-${category.name}") {
                                        SectionHeader(text = category.displayName, count = items.size)
                                    }
                                    items(
                                        items = items,
                                        key = { it.id },
                                    ) { exercise ->
                                        ExerciseRow(
                                            modifier = Modifier.animateItem(
                                                fadeInSpec = GymMotion.ItemFadeIn,
                                                placementSpec = GymMotion.ItemPlacement,
                                                fadeOutSpec = GymMotion.ItemFadeOut,
                                            ),
                                            exercise = exercise,
                                            showPrimaryMuscles = true,
                                            onTap = { onOpenDetail(exercise.id) },
                                            onDeleteRequest = { deleteConfirmTarget = exercise },
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

    when (val mode = editing) {
        EditMode.None -> Unit
        is EditMode.Create -> {
            ExerciseFormSheet(
                initial = mode.initial,
                title = "New exercise",
                onDismiss = { viewModel.closeForm() },
                onSave = { viewModel.save(it) },
            )
        }
    }

    deleteConfirmTarget?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteConfirmTarget = null },
            shape = dev.francescolofranco.gymtracker.ui.theme.DialogShape,
            containerColor = SurfaceCard,
            title = { Text("Delete \"${target.name}\"?") },
            text = {
                Text(
                    "Past session history that references this exercise stays intact. You can " +
                        "restore the exercise from the snackbar's Undo action immediately after.",
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val t = target
                    deleteConfirmTarget = null
                    deleteWithUndo(t)
                }) { Text("Delete") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleteConfirmTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun LibraryCategoryChipRow(
    selected: MuscleCategory?,
    onSelect: (MuscleCategory?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CategoryChip(
            label = "ALL",
            isSelected = selected == null,
            onClick = { onSelect(null) },
        )
        MuscleCategory.entries.forEach { category ->
            CategoryChip(
                label = category.displayName.uppercase(),
                isSelected = selected == category,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) BrandLime else SurfaceControl)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.7.sp,
            ),
            color = if (isSelected) DarkOnPrimary else SlateSubtext,
        )
    }
}

@Composable
private fun NoSearchResults(
    query: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (query.isNotBlank()) "No exercises match \"${query.trim()}\"" else "No exercises in this category",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Try another name or muscle.",
            style = MaterialTheme.typography.bodyMedium,
            color = OliveSubtext,
        )
        TextButton(onClick = onClear) {
            Text("Clear search")
        }
    }
}

@Composable
private fun SectionHeader(text: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDeep)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .background(BrandLime),
            )
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(
            modifier = Modifier
                .clip(TagShape)
                .background(SurfaceElevated)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = "$count EXERCISE${if (count == 1) "" else "S"}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.66.sp,
                ),
                color = SlateSubtext,
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No exercises yet",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Tap the + button to create your first exercise.",
            style = MaterialTheme.typography.bodyMedium,
            color = OliveSubtext,
        )
    }
}
