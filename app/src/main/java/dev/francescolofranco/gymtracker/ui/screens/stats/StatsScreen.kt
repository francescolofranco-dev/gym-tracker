package dev.francescolofranco.gymtracker.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.domain.WeekMode
import dev.francescolofranco.gymtracker.ui.screens.sessions.formatDuration
import dev.francescolofranco.gymtracker.ui.screens.sessions.formatTotalVolume
import dev.francescolofranco.gymtracker.ui.theme.VolumeBlue
import dev.francescolofranco.gymtracker.ui.theme.VolumeGreen
import dev.francescolofranco.gymtracker.ui.theme.VolumeGrey
import dev.francescolofranco.gymtracker.ui.theme.VolumeRed

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedMuscle by viewModel.selectedMuscle.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            WeekModeToggle(
                mode = state.weekMode,
                onChange = viewModel::setWeekMode,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item {
            BodyDiagram(
                volumes = state.muscleVolumes,
                onMuscleTap = { viewModel.selectMuscle(it) },
            )
        }
        item {
            VolumeLegend(modifier = Modifier.padding(horizontal = 16.dp))
        }
        item {
            SummaryCards(
                state = state,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }

    selectedMuscle?.let { m ->
        val volume = state.muscleVolumes[m] ?: return@let
        MuscleDrillSheet(
            volume = volume,
            onDismiss = { viewModel.selectMuscle(null) },
        )
    }
}

@Composable
private fun WeekModeToggle(
    mode: WeekMode,
    onChange: (WeekMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = mode == WeekMode.ROLLING_7,
            onClick = { onChange(WeekMode.ROLLING_7) },
            shape = SegmentedButtonDefaults.itemShape(0, 2),
        ) { Text("Rolling 7d") }
        SegmentedButton(
            selected = mode == WeekMode.MON_SUN,
            onClick = { onChange(WeekMode.MON_SUN) },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
        ) { Text("Mon–Sun") }
    }
}

@Composable
private fun VolumeLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem("0", VolumeGrey, modifier = Modifier.weight(1f))
        LegendItem("1–2", VolumeBlue, modifier = Modifier.weight(1f))
        LegendItem("3–10", VolumeGreen, modifier = Modifier.weight(1f))
        LegendItem("11+", VolumeRed, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LegendItem(label: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SummaryCards(state: StatsUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryCard(
            title = "Sessions",
            week = "${state.week.sessions}",
            month = "${state.month.sessions}",
            year = "${state.year.sessions}",
        )
        SummaryCard(
            title = "Total volume",
            week = formatTotalVolume(state.week.volumeKg, state.unit),
            month = formatTotalVolume(state.month.volumeKg, state.unit),
            year = formatTotalVolume(state.year.volumeKg, state.unit),
        )
        SummaryCard(
            title = "Avg session duration",
            week = state.week.avgDuration?.let { formatDuration(it) } ?: "—",
            month = state.month.avgDuration?.let { formatDuration(it) } ?: "—",
            year = state.year.avgDuration?.let { formatDuration(it) } ?: "—",
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    week: String,
    month: String,
    year: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = 4.dp),
            )
            StatLine(label = "This week", value = week)
            StatLine(label = "This month", value = month)
            StatLine(label = "This year", value = year)
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}
