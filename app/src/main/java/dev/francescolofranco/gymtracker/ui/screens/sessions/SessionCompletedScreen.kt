package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.domain.workoutDuration
import dev.francescolofranco.gymtracker.domain.workoutStartedAt
import dev.francescolofranco.gymtracker.ui.components.ErrorPane
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.LoadingPane
import androidx.compose.foundation.border
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.sp
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.ButtonShape
import dev.francescolofranco.gymtracker.ui.theme.DarkOnPrimary
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.MutedOlive
import dev.francescolofranco.gymtracker.ui.theme.SurfaceBase
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard
import dev.francescolofranco.gymtracker.ui.theme.SurfaceDeep
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated

@Composable
fun SessionCompletedScreen(
    onDone: () -> Unit,
    viewModel: SessionCompletedViewModel = hiltViewModel(),
) {
    val loadableContent by viewModel.content.collectAsStateWithLifecycle()
    when (val current = loadableContent) {
        Loadable.Loading -> LoadingPane()
        is Loadable.Error -> ErrorPane(current.message, viewModel::retry)
        is Loadable.Ready -> {
            val session = current.value.session
            if (session == null) {
                ErrorPane("This session no longer exists.", viewModel::retry)
            } else {
                CompletedSessionContent(
                    session = session,
                    details = current.value.details,
                    unit = current.value.unit,
                    onDone = onDone,
                )
            }
        }
    }
}

@Composable
private fun CompletedSessionContent(
    session: SessionEntity,
    details: List<dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail>,
    unit: WeightUnit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val summary = remember(details) { summarizeFinishedSession(details) }
    val shareText = remember(session, details, unit) {
        buildSessionShareText(session, details, unit)
    }

    Scaffold(
        containerColor = SurfaceDeep,
        bottomBar = {
            Surface(color = SurfaceBase, tonalElevation = 0.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    WhatsAppShareButton(
                        onClick = { shareSessionOnWhatsApp(context, shareText) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "DONE",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceDeep)
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, BrandLime, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = BrandLime,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "WORKOUT COMPLETE!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Great work — your session is saved.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = formatSessionDate(session.workoutStartedAt()).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = BrandLime,
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EditorialBorder, RoundedCornerShape(4.dp)),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceCard,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 12.dp),
                    ) {
                        CompletionMetric(
                            value = formatShareDuration(session.workoutDuration()),
                            label = "DURATION",
                            modifier = Modifier.weight(1f),
                        )
                        CompletionMetric(
                            value = summary.exerciseCount.toString(),
                            label = "EXERCISES",
                            modifier = Modifier.weight(1f),
                        )
                        CompletionMetric(
                            value = summary.setCount.toString(),
                            label = "SETS",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            if (summary.exercises.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, EditorialBorder, RoundedCornerShape(4.dp)),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceCard,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "SESSION SUMMARY",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                ),
                            )
                            HorizontalDivider(color = EditorialBorder, thickness = 1.dp)
                            summary.exercises.take(5).forEach { exercise ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = exercise.name.uppercase(),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = exercise.firstSetWeightLabel(unit),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandLime,
                                        )
                                        Text(
                                            text = exercise.setsLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MutedOlive,
                                        )
                                    }
                                }
                            }
                            if (summary.exercises.size > 5) {
                                Text(
                                    text = "+${summary.exercises.size - 5} more",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = BrandLime,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = BrandLime,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
            ),
            color = MutedOlive,
        )
    }
}

@Composable
fun WhatsAppShareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandLime,
            contentColor = DarkOnPrimary,
        ),
        modifier = modifier.height(52.dp),
    ) {
        Icon(Icons.Filled.Share, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "SHARE ON WHATSAPP",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            ),
        )
    }
}
