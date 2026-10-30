package dev.francescolofranco.gymtracker.ui.screens.exercises

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.domain.sortedTopToBottom
import dev.francescolofranco.gymtracker.ui.theme.CardShape
import dev.francescolofranco.gymtracker.ui.theme.MutedOlive
import dev.francescolofranco.gymtracker.ui.theme.SlateSubtext
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard

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
            .clickable(onClick = onTap)
            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = exercise.name.uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.36.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = exercise.summaryLine(showPrimaryMuscles),
                style = MaterialTheme.typography.bodySmall,
                color = SlateSubtext,
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
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
                tint = MutedOlive,
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
