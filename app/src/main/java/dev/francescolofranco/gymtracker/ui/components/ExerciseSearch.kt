package dev.francescolofranco.gymtracker.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import java.text.Normalizer
import java.util.Locale

@Composable
fun ExerciseSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        label = { Text("Search exercises") },
        placeholder = { Text("Name or muscle") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Clear search",
                    )
                }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
    )
}

/**
 * Matches every query term against the exercise's name and useful discovery metadata. Keeping
 * this client-side is deliberate: the active exercise catalogue is small and already loaded for
 * both callers, so results update immediately without restarting the database flow on each key.
 */
internal fun ExerciseEntity.matchesExerciseQuery(query: String): Boolean {
    val terms = query.normalizedForSearch()
        .split(WHITESPACE)
        .filter(String::isNotEmpty)
    if (terms.isEmpty()) return true

    val searchableText = buildString {
        append(name)
        primaryMuscles.forEach { muscle ->
            append(' ')
            append(muscle.displayName)
            append(' ')
            append(muscle.name.replace('_', ' '))
        }
        secondaryMuscles.forEach { muscle ->
            append(' ')
            append(muscle.displayName)
            append(' ')
            append(muscle.name.replace('_', ' '))
        }
        if (isBodyweight) append(" bodyweight bw")
        if (isUnilateral) append(" unilateral single side")
    }.normalizedForSearch()

    return terms.all(searchableText::contains)
}

private val WHITESPACE = Regex("\\s+")
private val COMBINING_MARKS = Regex("\\p{M}+")

private fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .lowercase(Locale.ROOT)
        .trim()
