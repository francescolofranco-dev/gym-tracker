package dev.francescolofranco.gymtracker.ui.screens.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.domain.sortedTopToBottom
import dev.francescolofranco.gymtracker.ui.theme.CardShape
import dev.francescolofranco.gymtracker.ui.theme.SlateSubtext
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard

@Composable
fun ExerciseRow(
    exercise: ExerciseEntity,
    onTap: () -> Unit,
    showPrimaryMuscles: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(CardShape)
            .background(SurfaceCard)
            .clickable(onClick = onTap)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = exercise.summaryLine(showPrimaryMuscles),
            style = MaterialTheme.typography.bodySmall,
            color = SlateSubtext,
        )
    }
}

private fun ExerciseEntity.summaryLine(showPrimaryMuscles: Boolean): String {
    val reps = if (repRangeMin == repRangeMax) "$repRangeMin" else "$repRangeMin–$repRangeMax"
    val sets = "${targetSets}×${reps}${if (isUnilateral) "/side" else ""}"
    val secondaries = secondaryMuscles.sortedTopToBottom().joinToString(", ") { it.displayName }
    val parts = mutableListOf<String>()
    if (showPrimaryMuscles) {
        parts += primaryMuscles.sortedTopToBottom().joinToString(" + ") { it.displayName }
    }
    if (isBodyweight) parts += "BW"
    if (isNegativeWeight) parts += "Assisted"
    if (isUnilateral) parts += "Unilateral"
    parts += sets
    if (secondaries.isNotEmpty()) parts += secondaries
    return parts.joinToString(" · ")
}
