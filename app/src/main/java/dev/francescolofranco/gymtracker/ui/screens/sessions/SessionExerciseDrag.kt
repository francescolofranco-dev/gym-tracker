package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.ui.motion.GymMotion
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.OliveSubtext
import dev.francescolofranco.gymtracker.ui.theme.SurfaceControl
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

/** Keeps the granular set rows intact while dragging a header to another exercise's position. */
internal class SessionExerciseDrag(val listState: LazyListState) {
    var draggedId by mutableStateOf<Long?>(null)
    var targetId by mutableStateOf<Long?>(null)
    var fingerY by mutableFloatStateOf(0f)
    var grabOffset by mutableFloatStateOf(0f)
    var gestures: Modifier = Modifier
    var indicatorColor: Color = Color.Unspecified
    var dropAfter by mutableStateOf(false)
    var pendingOrder by mutableStateOf<List<Long>?>(null)
    var pickupProgress by mutableFloatStateOf(0f)
    var indicatorProgress by mutableFloatStateOf(0f)
    val displacementBySection = mutableStateMapOf<Long, State<Float>>()

    fun orderedDetails(details: List<SessionExerciseDetail>): List<SessionExerciseDetail> =
        orderedExerciseDetails(details, pendingOrder)

    fun isDragged(id: Long): Boolean = draggedId == id

    fun isDropTarget(id: Long): Boolean = targetId == id && draggedId != null && draggedId != id

    fun headerModifier(id: Long): Modifier {
        val base = Modifier.drawWithContent {
            drawContent()
            if (isDropTarget(id) && !dropAfter) {
                drawDropIndicator(indicatorColor, y = 0f, progress = indicatorProgress)
            }
        }
        if (draggedId == null) return base
        return base.graphicsLayer {
            if (isDragged(id)) {
                alpha = lerp(1f, 0.32f, pickupProgress)
                val scale = lerp(1f, 0.98f, pickupProgress)
                scaleX = scale
                scaleY = scale
            } else {
                val shiftDp = displacementBySection[id]?.value ?: 0f
                translationY = shiftDp.dp.toPx()
            }
        }
    }

    fun bodyModifier(id: Long): Modifier {
        if (draggedId == null) return Modifier
        return Modifier.graphicsLayer {
            if (isDragged(id)) {
                alpha = lerp(1f, 0.32f, pickupProgress)
            } else {
                val shiftDp = displacementBySection[id]?.value ?: 0f
                translationY = shiftDp.dp.toPx()
            }
        }
    }

    fun footerModifier(id: Long): Modifier = Modifier.drawBehind {
        if (isDropTarget(id) && dropAfter) {
            drawDropIndicator(indicatorColor, y = size.height / 2f, progress = indicatorProgress)
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

private fun DrawScope.drawDropIndicator(color: Color, y: Float, progress: Float) {
    val clamped = progress.coerceIn(0f, 1f)
    if (clamped <= 0.01f) return
    val capInset = 4.dp.toPx()
    val halfSpan = ((size.width / 2f) - capInset).coerceAtLeast(0f) * clamped
    val centerX = size.width / 2f
    val start = Offset(centerX - halfSpan, y)
    val end = Offset(centerX + halfSpan, y)
    drawLine(
        color = color.copy(alpha = 0.28f * clamped),
        start = start,
        end = end,
        strokeWidth = 10.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color.copy(alpha = clamped),
        start = start,
        end = end,
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
    )
    val capRadius = 4.5.dp.toPx() * clamped
    drawCircle(color = color.copy(alpha = clamped), radius = capRadius, center = start)
    drawCircle(color = color.copy(alpha = clamped), radius = capRadius, center = end)
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
    state.indicatorColor = BrandLime
    val haptics = LocalHapticFeedback.current
    val edge = with(LocalDensity.current) { 64.dp.toPx() }

    val orderedIds = remember(details, state.pendingOrder) {
        state.orderedDetails(details).map { it.sessionExercise.id }
    }

    val pickupAnim = remember { Animatable(0f) }
    LaunchedEffect(state.draggedId) {
        if (state.draggedId != null) {
            pickupAnim.snapTo(0f)
            pickupAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        } else {
            pickupAnim.snapTo(0f)
        }
    }
    state.pickupProgress = pickupAnim.value

    val indicatorAnim = remember { Animatable(0f) }
    LaunchedEffect(state.draggedId, state.targetId, state.dropAfter) {
        val activeTarget = state.draggedId != null &&
            state.targetId != null &&
            state.targetId != state.draggedId
        if (activeTarget) {
            indicatorAnim.snapTo(0f)
            indicatorAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(GymMotion.Quick, easing = GymMotion.EmphasizedEasing),
            )
        } else {
            indicatorAnim.snapTo(0f)
        }
    }
    state.indicatorProgress = indicatorAnim.value

    orderedIds.forEach { sectionId ->
        val targetShift = sectionDisplacementDp(
            orderedIds = orderedIds,
            draggedId = state.draggedId,
            targetId = state.targetId,
            dropAfter = state.dropAfter,
            sectionId = sectionId,
        )
        val animatedShift = animateFloatAsState(
            targetValue = targetShift,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            label = "section-displacement-$sectionId",
        )
        state.displacementBySection[sectionId] = animatedShift
    }

    LaunchedEffect(details, state.pendingOrder) {
        val pending = state.pendingOrder ?: return@LaunchedEffect
        val currentIds = details.map { it.sessionExercise.id }
        if (currentIds == pending || currentIds.toSet() != pending.toSet()) {
            state.pendingOrder = null
        }
    }

    fun updateTarget() {
        val previousTargetId = state.targetId
        val previousDropAfter = state.dropAfter
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
        val ids = state.orderedDetails(latestDetails).map { it.sessionExercise.id }
        state.dropAfter = ids.indexOf(state.targetId) > ids.indexOf(state.draggedId)
        if (state.draggedId != null &&
            (state.targetId != previousTargetId || state.dropAfter != previousDropAfter)
        ) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
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
                    state.dropAfter = false
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
                val ids = state.orderedDetails(latestDetails).map { it.sessionExercise.id }
                val reordered = reorderedExerciseIds(ids, state.draggedId, state.targetId)
                if (reordered != ids) {
                    // Keep the viewport anchored at its current scroll offset instead of jumping
                    // to follow the first visible item's key when the top exercise is moved.
                    listState.requestScrollToItem(
                        index = listState.firstVisibleItemIndex,
                        scrollOffset = listState.firstVisibleItemScrollOffset,
                    )
                    state.pendingOrder = reordered
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    latestOnReorder(reordered)
                }
                state.draggedId = null
                state.targetId = null
                state.dropAfter = false
            },
            onDragCancel = {
                state.draggedId = null
                state.targetId = null
                state.dropAfter = false
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

internal fun orderedExerciseDetails(
    details: List<SessionExerciseDetail>,
    pendingOrder: List<Long>?,
): List<SessionExerciseDetail> {
    if (pendingOrder == null || pendingOrder.size != details.size) return details
    val byId = details.associateBy { it.sessionExercise.id }
    val ordered = pendingOrder.mapNotNull { byId[it] }
    return if (ordered.size == details.size && ordered.map { it.sessionExercise.id }.toSet() == byId.keys) {
        ordered
    } else {
        details
    }
}

internal fun targetNudgeDirection(
    draggedId: Long?,
    targetId: Long?,
    dropAfter: Boolean,
    sectionId: Long,
): Int {
    if (draggedId == null || draggedId == sectionId || targetId != sectionId) return 0
    return if (dropAfter) -1 else 1
}

/**
 * Vertical displacement (in dp) applied to non-dragged exercise sections while a drag is active:
 * the drop target shifts by 10dp away from the insertion line to open a clear slot, and any
 * intermediate sections between the dragged exercise and the target shift by 6dp toward their
 * upcoming swapped position.
 */
internal fun sectionDisplacementDp(
    orderedIds: List<Long>,
    draggedId: Long?,
    targetId: Long?,
    dropAfter: Boolean,
    sectionId: Long,
): Float {
    if (draggedId == null || targetId == null || draggedId == targetId || draggedId == sectionId) {
        return 0f
    }
    if (sectionId == targetId) {
        return if (dropAfter) -10f else 10f
    }
    val fromIndex = orderedIds.indexOf(draggedId)
    val toIndex = orderedIds.indexOf(targetId)
    val sectionIndex = orderedIds.indexOf(sectionId)
    if (fromIndex < 0 || toIndex < 0 || sectionIndex < 0) return 0f
    return when {
        fromIndex < toIndex && sectionIndex in (fromIndex + 1) until toIndex -> -6f
        fromIndex > toIndex && sectionIndex in (toIndex + 1) until fromIndex -> 6f
        else -> 0f
    }
}

/** Rendered outside the lazy items so the dragged label stays visible during edge scrolling. */
@Composable
internal fun SessionExerciseDragPreview(state: SessionExerciseDrag, details: List<SessionExerciseDetail>) {
    val dragged = details.firstOrNull { it.sessionExercise.id == state.draggedId } ?: return
    val target = details.firstOrNull { it.sessionExercise.id == state.targetId }
    val statusText = if (target == null || target == dragged) {
        "DRAG UP OR DOWN TO REORDER"
    } else {
        "DROP ${if (state.dropAfter) "AFTER" else "BEFORE"} ${target.exercise.name.uppercase()}"
    }
    val previewScale = lerp(0.96f, 1.02f, state.pickupProgress)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .offset {
                IntOffset(
                    0,
                    (state.fingerY - state.grabOffset + state.listState.layoutInfo.beforeContentPadding).roundToInt(),
                )
            }
            .graphicsLayer {
                scaleX = previewScale
                scaleY = previewScale
            },
        shape = RoundedCornerShape(4.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.5.dp, BrandLime),
        shadowElevation = lerp(4f, 16f, state.pickupProgress).dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceControl)
                    .border(1.dp, EditorialBorder, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription = null,
                    tint = BrandLime,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = dragged.exercise.name.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(SurfaceControl)
                            .border(1.dp, EditorialBorder, RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = dragged.setPlanLabel().uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            ),
                            color = OliveSubtext,
                            maxLines = 1,
                        )
                    }
                }
                AnimatedContent(
                    targetState = statusText,
                    transitionSpec = {
                        val direction = if (state.dropAfter) 1 else -1
                        (slideInVertically(tween(GymMotion.Quick, easing = GymMotion.EmphasizedEasing)) { direction * it / 2 } +
                            fadeIn(tween(GymMotion.Quick, easing = GymMotion.EmphasizedEasing)))
                            .togetherWith(
                                slideOutVertically(tween(GymMotion.Quick / 2, easing = GymMotion.ExitEasing)) { -direction * it / 2 } +
                                    fadeOut(tween(GymMotion.Quick / 2, easing = GymMotion.ExitEasing)),
                            ) using SizeTransform(clip = false)
                    },
                    label = "drag-preview-status",
                ) { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                        ),
                        color = BrandLime,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
