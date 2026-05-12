package dev.francescolofranco.gymtracker.ui.screens.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.ui.components.MuscleChips
import dev.francescolofranco.gymtracker.ui.components.NumberStepper

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
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = state.name,
                onValueChange = { state = state.copy(name = it) },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = state.isBodyweight,
                    onCheckedChange = { state = state.copy(isBodyweight = it) },
                )
                Spacer(Modifier.width(12.dp))
                Text("Bodyweight exercise")
            }

            SectionLabel("Primary muscles (up to ${ExerciseFormState.MAX_PRIMARY_MUSCLES})")
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

            SectionLabel("Secondary muscles (optional)")
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

            StepperRow(
                label = "Target sets",
                value = state.targetSets,
                min = 1,
                max = ExerciseFormState.MAX_SETS,
                onChange = { state = state.copy(targetSets = it) },
            )

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

            StepperRow(
                label = "Rep range max",
                value = state.repRangeMax,
                min = state.repRangeMin,
                max = ExerciseFormState.MAX_REPS,
                onChange = { state = state.copy(repRangeMax = it) },
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { onSave(state) },
                enabled = state.isValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            style = MaterialTheme.typography.bodyLarge,
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

