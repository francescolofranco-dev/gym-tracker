package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Keeps the granular set rows intact while dragging a header to another exercise's position. */
internal class SessionExerciseDrag(val listState: LazyListState) {
    var draggedId by mutableStateOf<Long?>(null)
    var targetId by mutableStateOf<Long?>(null)
    var fingerY by mutableFloatStateOf(0f)
    var grabOffset by mutableFloatStateOf(0f)
    var gestures: Modifier = Modifier
    var indicatorColor: Color = Color.Unspecified
    var dropAfter by mutableStateOf(false)

    fun headerModifier(id: Long): Modifier {
        val dragged = draggedId == id
        return Modifier
            .drawBehind {
                if (targetId == id && draggedId != null && draggedId != id && !dropAfter) {
                    drawLine(indicatorColor, Offset.Zero, Offset(size.width, 0f), 3.dp.toPx())
                }
            }
            .graphicsLayer { alpha = if (dragged) 0.35f else 1f }
    }

    fun footerModifier(id: Long): Modifier = Modifier.drawBehind {
        if (targetId == id && draggedId != null && draggedId != id && dropAfter) {
            drawLine(indicatorColor, Offset.Zero, Offset(size.width, 0f), 3.dp.toPx())
        }
    }

    fun updateTarget() {
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull {
            fingerY >= it.offset && fingerY < it.offset + it.size
        } ?: return
        // Headers, column labels and footers carry the section id; sets need their own mapping.
        val key = item.key as? String ?: return
        if (key.startsWith("exercise-")) {
            targetId = key.removePrefix("exercise-").substringBefore('-').toLongOrNull() ?: targetId
        }
    }
}

@Composable
internal fun rememberSessionExerciseDrag(
    details: List<SessionExerciseDetail>,
    onReorder: (List<Long>) -> Unit,
): SessionExerciseDrag {
    val listState = rememberLazyListState()
    val state = remember(listState) { SessionExerciseDrag(listState) }
    val latestDetails by rememberUpdatedState(details)
    val latestOnReorder by rememberUpdatedState(onReorder)
    state.indicatorColor = MaterialTheme.colorScheme.primary
    val haptics = LocalHapticFeedback.current
    val edge = with(LocalDensity.current) { 64.dp.toPx() }

    fun updateTarget() {
        state.updateTarget()
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull {
            state.fingerY >= it.offset && state.fingerY < it.offset + it.size
        }
        val key = item?.key as? String
        if (key?.startsWith("set-") == true) {
            val setId = key.removePrefix("set-").toLongOrNull()
            latestDetails.firstOrNull { detail -> detail.setLogs.any { it.id == setId } }
                ?.let { state.targetId = it.sessionExercise.id }
        }
        val ids = latestDetails.map { it.sessionExercise.id }
        state.dropAfter = ids.indexOf(state.targetId) > ids.indexOf(state.draggedId)
    }

    LaunchedEffect(state.draggedId) {
        if (state.draggedId == null) return@LaunchedEffect
        while (isActive) {
            val layout = listState.layoutInfo
            val speed = when {
                state.fingerY < layout.viewportStartOffset + edge ->
                    -((layout.viewportStartOffset + edge - state.fingerY) / edge).coerceIn(0f, 1f) * 24f
                state.fingerY > layout.viewportEndOffset - edge ->
                    ((state.fingerY - layout.viewportEndOffset + edge) / edge).coerceIn(0f, 1f) * 24f
                else -> 0f
            }
            if (speed != 0f) {
                listState.scrollBy(speed)
                updateTarget()
            }
            delay(16)
        }
    }
    state.gestures = Modifier.pointerInput(state) {
        detectDragGesturesAfterLongPress(
            onDragStart = { position ->
                val item = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                    position.y - listState.layoutInfo.beforeContentPadding >= it.offset &&
                        position.y - listState.layoutInfo.beforeContentPadding < it.offset + it.size &&
                        (it.key as? String)?.endsWith("-header") == true
                }
                val id = (item?.key as? String)?.removePrefix("exercise-")
                    ?.removeSuffix("-header")?.toLongOrNull()
                if (id != null && latestDetails.size > 1) {
                    state.draggedId = id
                    state.targetId = id
                    state.fingerY = position.y - listState.layoutInfo.beforeContentPadding
                    state.grabOffset = state.fingerY - item.offset
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            },
            onDrag = { change, amount ->
                if (state.draggedId != null) {
                    change.consume()
                    state.fingerY += amount.y
                    updateTarget()
                }
            },
            onDragEnd = {
                val ids = latestDetails.map { it.sessionExercise.id }
                val reordered = reorderedExerciseIds(ids, state.draggedId, state.targetId)
                if (reordered != ids) latestOnReorder(reordered)
                state.draggedId = null
                state.targetId = null
            },
            onDragCancel = {
                state.draggedId = null
                state.targetId = null
            },
        )
    }
    return state
}

internal fun reorderedExerciseIds(ids: List<Long>, draggedId: Long?, targetId: Long?): List<Long> {
    val from = ids.indexOf(draggedId)
    val to = ids.indexOf(targetId)
    if (from < 0 || to < 0 || from == to) return ids
    return ids.toMutableList().apply { add(to, removeAt(from)) }
}

/** Rendered outside the lazy items so the dragged label stays visible during edge scrolling. */
@Composable
internal fun SessionExerciseDragPreview(state: SessionExerciseDrag, details: List<SessionExerciseDetail>) {
    val dragged = details.firstOrNull { it.sessionExercise.id == state.draggedId } ?: return
    val target = details.firstOrNull { it.sessionExercise.id == state.targetId }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .offset { IntOffset(0, (state.fingerY - state.grabOffset + state.listState.layoutInfo.beforeContentPadding).roundToInt()) },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(dragged.exercise.name, style = MaterialTheme.typography.titleMedium)
            Text(
                if (target == null || target == dragged) "Drag up or down to reorder"
                else "Release to place ${if (state.dropAfter) "after" else "before"} ${target.exercise.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
