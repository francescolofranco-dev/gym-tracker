package dev.francescolofranco.gymtracker.ui.screens.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.ui.components.MuscleChips
import dev.francescolofranco.gymtracker.ui.components.NumberStepper
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.ButtonShape
import dev.francescolofranco.gymtracker.ui.theme.CardShape
import dev.francescolofranco.gymtracker.ui.theme.DarkOnPrimary
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.MutedOlive
import dev.francescolofranco.gymtracker.ui.theme.OliveSubtext
import dev.francescolofranco.gymtracker.ui.theme.SurfaceBase
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard
import dev.francescolofranco.gymtracker.ui.theme.SurfaceControl
import dev.francescolofranco.gymtracker.ui.theme.SurfaceDeep
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated
import dev.francescolofranco.gymtracker.ui.theme.TagShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseFormSheet(
    initial: ExerciseFormState,
    title: String,
    onDismiss: () -> Unit,
    onSave: (ExerciseFormState) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var state by remember { mutableStateOf(initial) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(EditorialBorder),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Name card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(SurfaceBase)
                    .border(1.dp, EditorialBorder.copy(alpha = 0.25f), CardShape)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel("EXERCISE NAME")
                    Box(
                        modifier = Modifier
                            .clip(TagShape)
                            .background(BrandLime.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "REQUIRED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandLime,
                        )
                    }
                }
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { state = state.copy(name = it) },
                    label = { Text("Name") },
                    singleLine = true,
                    shape = ButtonShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDeep,
                        unfocusedContainerColor = SurfaceDeep,
                        focusedBorderColor = BrandLime,
                        unfocusedBorderColor = EditorialBorder.copy(alpha = 0.4f),
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Toggles card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(SurfaceBase)
                    .border(1.dp, EditorialBorder.copy(alpha = 0.25f), CardShape)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bodyweight exercise",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = "Track reps without external load.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OliveSubtext,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = state.isBodyweight,
                        onCheckedChange = { state = state.copy(isBodyweight = it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkOnPrimary,
                            checkedTrackColor = BrandLime,
                            uncheckedThumbColor = OliveSubtext,
                            uncheckedTrackColor = SurfaceControl,
                        ),
                    )
                }

                HorizontalDivider(color = EditorialBorder.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Unilateral exercise",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = "Logs left and right separately; target sets apply per side.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OliveSubtext,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = state.isUnilateral,
                        onCheckedChange = { state = state.copy(isUnilateral = it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkOnPrimary,
                            checkedTrackColor = BrandLime,
                            uncheckedThumbColor = OliveSubtext,
                            uncheckedTrackColor = SurfaceControl,
                        ),
                    )
                }

                if (state.isUnilateral != initial.isUnilateral) {
                    Text(
                        "Applies to future sessions and empty sets in the open session. " +
                            "Entered, completed and skipped sets keep their original sides; " +
                            "past sessions are unchanged.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OliveSubtext,
                    )
                }
            }

            // Primary muscles card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(SurfaceBase)
                    .border(1.dp, EditorialBorder.copy(alpha = 0.25f), CardShape)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel("Primary muscles (up to ${ExerciseFormState.MAX_PRIMARY_MUSCLES})")
                    Box(
                        modifier = Modifier
                            .clip(TagShape)
                            .background(SurfaceElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "${state.primaryMuscles.size} / ${ExerciseFormState.MAX_PRIMARY_MUSCLES} SELECTED",
                            style = MaterialTheme.typography.labelSmall,
                            color = OliveSubtext,
                        )
                    }
                }
                MuscleChips(
                    selected = { it in state.primaryMuscles },
                    enabled = { m ->
                        // Allow toggling off any selected one; allow toggling on while under the cap.
                        m in state.primaryMuscles || state.primaryMuscles.size < ExerciseFormState.MAX_PRIMARY_MUSCLES
                    },
                    onTap = { m ->
                        val newPrimaries = if (m in state.primaryMuscles) {
                            state.primaryMuscles - m
                        } else if (state.primaryMuscles.size < ExerciseFormState.MAX_PRIMARY_MUSCLES) {
                            state.primaryMuscles + m
                        } else {
                            state.primaryMuscles
                        }
                        state = state.copy(
                            primaryMuscles = newPrimaries,
                            // A muscle can't simultaneously be primary and secondary.
                            secondaryMuscles = state.secondaryMuscles - newPrimaries,
                        )
                    },
                )
            }

            // Secondary muscles card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(SurfaceBase)
                    .border(1.dp, EditorialBorder.copy(alpha = 0.25f), CardShape)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel("Secondary muscles (optional)")
                    Box(
                        modifier = Modifier
                            .clip(TagShape)
                            .background(SurfaceElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "OPTIONAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedOlive,
                        )
                    }
                }
                MuscleChips(
                    selected = { it in state.secondaryMuscles },
                    enabled = { it !in state.primaryMuscles },
                    onTap = { m ->
                        val newSecondaries = if (m in state.secondaryMuscles) {
                            state.secondaryMuscles - m
                        } else {
                            state.secondaryMuscles + m
                        }
                        state = state.copy(secondaryMuscles = newSecondaries)
                    },
                )
            }

            // Prescription defaults card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(SurfaceBase)
                    .border(1.dp, EditorialBorder.copy(alpha = 0.25f), CardShape)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionLabel("PRESCRIPTION DEFAULTS")
                StepperRow(
                    label = if (state.isUnilateral) "Target sets per side" else "Target sets",
                    value = state.targetSets,
                    min = 1,
                    max = ExerciseFormState.MAX_SETS,
                    onChange = { state = state.copy(targetSets = it) },
                )

                HorizontalDivider(color = EditorialBorder.copy(alpha = 0.2f))

                StepperRow(
                    label = "Rep range min",
                    value = state.repRangeMin,
                    min = 1,
                    max = ExerciseFormState.MAX_REPS,
                    onChange = {
                        val newMin = it
                        val newMax = maxOf(state.repRangeMax, newMin)
                        state = state.copy(repRangeMin = newMin, repRangeMax = newMax)
                    },
                )

                HorizontalDivider(color = EditorialBorder.copy(alpha = 0.2f))

                StepperRow(
                    label = "Rep range max",
                    value = state.repRangeMax,
                    min = state.repRangeMin,
                    max = ExerciseFormState.MAX_REPS,
                    onChange = { state = state.copy(repRangeMax = it) },
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = { onSave(state) },
                enabled = state.isValid,
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandLime,
                    contentColor = DarkOnPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = OliveSubtext,
    )
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f),
        )
        NumberStepper(
            value = value.toDouble(),
            onValueChange = { onChange(it.toInt().coerceIn(min, max)) },
            step = 1.0,
            min = min.toDouble(),
            max = max.toDouble(),
        )
    }
}
