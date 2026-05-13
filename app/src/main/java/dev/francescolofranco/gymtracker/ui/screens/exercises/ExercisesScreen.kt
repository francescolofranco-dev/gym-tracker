package dev.francescolofranco.gymtracker.ui.screens.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
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

    var actionTarget by remember { mutableStateOf<ExerciseEntity?>(null) }
    var deleteConfirmTarget by remember { mutableStateOf<ExerciseEntity?>(null) }

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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openCreate() },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New exercise") },
            )
        },
    ) { padding ->
        if (grouped.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                grouped.forEach { (muscle, items) ->
                    stickyHeader(key = "header-${muscle.name}") {
                        SectionHeader(text = muscle.displayName)
                    }
                    items(
                        items = items,
                        key = { it.id },
                    ) { exercise ->
                        ExerciseRow(
                            exercise = exercise,
                            onTap = { onOpenDetail(exercise.id) },
                            onLongPress = { actionTarget = exercise },
                            onDeleteRequest = { deleteConfirmTarget = exercise },
                        )
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
                title = if (mode.isDuplicate) "Duplicate exercise" else "New exercise",
                onDismiss = { viewModel.closeForm() },
                onSave = { viewModel.save(it) },
            )
        }
    }

    actionTarget?.let { target ->
        ExerciseActionsSheet(
            exercise = target,
            onDismiss = { actionTarget = null },
            onDuplicate = {
                viewModel.openDuplicate(target)
                actionTarget = null
            },
            onDelete = {
                actionTarget = null
                deleteConfirmTarget = target
            },
        )
    }

    deleteConfirmTarget?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteConfirmTarget = null },
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
private fun SectionHeader(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
