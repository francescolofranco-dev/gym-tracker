package dev.francescolofranco.gymtracker.ui.screens.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.domain.sortedTopToBottom
import dev.francescolofranco.gymtracker.ui.theme.CardShape
import dev.francescolofranco.gymtracker.ui.theme.DestructiveCoral
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.OliveSubtext
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard
import dev.francescolofranco.gymtracker.ui.theme.SurfaceDeep

@Composable
fun ExerciseRow(
    exercise: ExerciseEntity,
    onTap: () -> Unit,
    onDeleteRequest: () -> Unit,
    showPrimaryMuscles: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(CardShape)
            .background(SurfaceCard)
            .border(1.dp, EditorialBorder.copy(alpha = 0.25f), CardShape)
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = exercise.summaryLine(showPrimaryMuscles),
                style = MaterialTheme.typography.bodySmall,
                color = OliveSubtext,
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceDeep)
                .border(1.dp, EditorialBorder.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                .clickable(onClick = onDeleteRequest)
                .semantics {
                    role = Role.Button
                    contentDescription = "Delete ${exercise.name}"
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = null,
                tint = DestructiveCoral,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun ExerciseEntity.summaryLine(showPrimaryMuscles: Boolean): String {
    val sets = "${targetSets}×${repRangeMin}–${repRangeMax}${if (isUnilateral) "/side" else ""}"
    val secondaries = secondaryMuscles.sortedTopToBottom().joinToString(", ") { it.displayName }
    val parts = mutableListOf<String>()
    if (showPrimaryMuscles) {
        parts += primaryMuscles.sortedTopToBottom().joinToString(" + ") { it.displayName }
    }
    if (isBodyweight) parts += "BW"
    if (isUnilateral) parts += "Unilateral"
    parts += sets
    if (secondaries.isNotEmpty()) parts += secondaries
    return parts.joinToString(" · ")
}
