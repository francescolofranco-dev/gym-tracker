package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.data.db.projections.SessionSummary
import dev.francescolofranco.gymtracker.domain.workoutStartedAt
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.LoadingPane
import dev.francescolofranco.gymtracker.ui.theme.DialogShape

@Composable
internal fun MergeSessionDialog(
    candidates: Loadable<List<SessionSummary>>,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onMerge: (Long) -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = (candidates as? Loadable.Ready)?.value?.find { it.session.id == selectedId }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        title = { Text("Merge sessions") },
        text = {
            when (candidates) {
                Loadable.Loading -> LoadingPane(Modifier.height(80.dp))
                is Loadable.Error -> Column {
                    Text("Couldn’t load completed sessions.")
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
                is Loadable.Ready -> if (candidates.value.isEmpty()) {
                    Text("There are no other completed sessions to merge.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Choose a session to merge into this one. This session keeps its date and " +
                                "duration. The selected session’s exercises, sets, and notes move here, " +
                                "and its original entry is removed from history.",
                        )
                        LazyColumn(
                            Modifier.fillMaxWidth().heightIn(max = 280.dp).selectableGroup(),
                        ) {
                            items(candidates.value, key = { it.session.id }) { summary ->
                                val id = summary.session.id
                                val isSelected = id == selectedId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = isSelected,
                                            role = Role.RadioButton,
                                            onClick = { selectedId = id },
                                        )
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(selected = isSelected, onClick = null)
                                    Column {
                                        Text(formatSessionDate(summary.session.workoutStartedAt()))
                                        val exercises = if (summary.exerciseCount == 1) "exercise" else "exercises"
                                        val sets = if (summary.setCount == 1) "set" else "sets"
                                        Text(
                                            "${summary.exerciseCount} $exercises · ${summary.setCount} logged $sets",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = selected != null, onClick = { selected?.let { onMerge(it.session.id) } }) {
                Text("Merge sessions")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
