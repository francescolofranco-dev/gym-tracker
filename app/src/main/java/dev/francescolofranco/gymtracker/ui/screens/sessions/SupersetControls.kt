package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.ui.components.GymIcons
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.OliveSubtext
import dev.francescolofranco.gymtracker.ui.theme.SurfaceBase
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SupersetPartnerSheet(
    lead: SessionExerciseDetail,
    choices: List<SessionExerciseDetail>,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceBase,
    ) {
        Text(
            text = "Pair ${lead.exercise.name} with…",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(choices, key = { it.sessionExercise.id }) { detail ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(role = Role.Button) { onPick(detail.sessionExercise.id) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(detail.exercise.name, style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider(color = EditorialBorder, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}

@Composable
internal fun SupersetHeader(onRemove: (() -> Unit)?) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(SurfaceCard)
            .border(1.dp, EditorialBorder, shape)
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Superset",
            color = BrandLime,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (onRemove != null) {
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(GymIcons.MoreVertical, contentDescription = "Superset options")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Separate exercises") },
                        onClick = {
                            expanded = false
                            onRemove()
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun SupersetSetLabel(exerciseName: String, round: Int?, active: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().background(SurfaceElevated)) {
        if (round != null) {
            Text(
                text = "Round $round",
                color = OliveSubtext,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceBase)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Text(
            text = exerciseName,
            color = if (active) BrandLime else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
