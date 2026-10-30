package dev.francescolofranco.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.DarkOnSurface
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.SlateSubtext
import dev.francescolofranco.gymtracker.ui.theme.SurfaceControl
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated
import java.text.Normalizer
import java.util.Locale

@Composable
fun ExerciseSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceControl,
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(4.dp)
    val borderColor = if (isFocused) BrandLime else EditorialBorder.copy(alpha = 0.4f)

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .onFocusChanged { isFocused = it.isFocused },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            color = DarkOnSurface,
        ),
        cursorBrush = SolidColor(BrandLime),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = if (isFocused) BrandLime else SlateSubtext,
                    modifier = Modifier.size(18.dp),
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search exercises",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = SlateSubtext,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceElevated)
                            .clickable { onQueryChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear search",
                            tint = SlateSubtext,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        },
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
