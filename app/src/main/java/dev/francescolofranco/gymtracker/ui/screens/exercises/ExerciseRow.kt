package dev.francescolofranco.gymtracker.ui.screens.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity

@Composable
fun ExerciseRow(
    exercise: ExerciseEntity,
    onTap: () -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onTap)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = exercise.summaryLine(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDeleteRequest) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "Delete ${exercise.name}",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun ExerciseEntity.summaryLine(): String {
    val sets = "${targetSets}×${repRangeMin}–${repRangeMax}${if (isUnilateral) "/side" else ""}"
    val secondaries = secondaryMuscles.joinToString(", ") { it.displayName }
    val parts = mutableListOf<String>()
    if (isBodyweight) parts += "BW"
    if (isUnilateral) parts += "Unilateral"
    parts += sets
    if (secondaries.isNotEmpty()) parts += secondaries
    return parts.joinToString(" · ")
}
