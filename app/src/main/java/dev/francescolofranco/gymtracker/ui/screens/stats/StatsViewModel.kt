package dev.francescolofranco.gymtracker.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.francescolofranco.gymtracker.data.db.dao.SessionDao
import dev.francescolofranco.gymtracker.data.db.projections.SessionSummary
import dev.francescolofranco.gymtracker.data.db.projections.StatSetRow
import dev.francescolofranco.gymtracker.data.prefs.UserPrefs
import dev.francescolofranco.gymtracker.data.repository.SessionRepository
import dev.francescolofranco.gymtracker.domain.Muscle
import dev.francescolofranco.gymtracker.domain.WeekMode
import dev.francescolofranco.gymtracker.domain.WeightUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

data class RangeAggregate(
    val sessions: Int,
    val volumeKg: Double,
    val avgDuration: Duration?,
)

data class StatsUiState(
    val weekMode: WeekMode,
    val unit: WeightUnit,
    val muscleVolumes: Map<Muscle, MuscleVolume>,
    val week: RangeAggregate,
    val month: RangeAggregate,
    val year: RangeAggregate,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val sessionDao: SessionDao,
    private val repo: SessionRepository,
    private val prefs: UserPrefs,
) : ViewModel() {

    private val _selectedMuscle = MutableStateFlow<Muscle?>(null)
    val selectedMuscle: StateFlow<Muscle?> = _selectedMuscle

    @OptIn(ExperimentalCoroutinesApi::class)
    private val weekRows: StateFlow<List<StatSetRow>> = prefs.weekMode
        .flatMapLatest { mode -> sessionDao.observeLoggedSetsBetween(weekRange(mode).startInclusive, weekRange(mode).endExclusive) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val muscleVolumes: StateFlow<Map<Muscle, MuscleVolume>> = weekRows
        .map { rows -> computeMuscleVolumes(rows) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val uiState: StateFlow<StatsUiState> = combine(
        prefs.weekMode,
        prefs.unit,
        muscleVolumes,
        repo.observeAllSummaries(),
    ) { mode, unit, volumes, summaries ->
        val now = Instant.now()
        val w = weekRange(mode, now)
        val m = monthRange(now)
        val y = yearRange(now)
        StatsUiState(
            weekMode = mode,
            unit = unit,
            muscleVolumes = volumes,
            week = aggregate(summaries, w),
            month = aggregate(summaries, m),
            year = aggregate(summaries, y),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        StatsUiState(
            weekMode = WeekMode.ROLLING_7,
            unit = WeightUnit.KG,
            muscleVolumes = Muscle.entries.associateWith { MuscleVolume(it, 0, 0, emptyList()) },
            week = RangeAggregate(0, 0.0, null),
            month = RangeAggregate(0, 0.0, null),
            year = RangeAggregate(0, 0.0, null),
        ),
    )

    fun selectMuscle(m: Muscle?) {
        _selectedMuscle.value = m
    }

    fun setWeekMode(mode: WeekMode) = viewModelScope.launch {
        prefs.setWeekMode(mode)
    }

    private fun aggregate(all: List<SessionSummary>, range: DateRange): RangeAggregate {
        val inRange = all.filter {
            val t = it.session.startedAt
            !t.isBefore(range.startInclusive) && t.isBefore(range.endExclusive)
        }
        val volume = inRange.sumOf { it.totalVolume }
        val durations = inRange.mapNotNull { s ->
            s.session.endedAt?.let { Duration.between(s.session.startedAt, it) }
        }
        val avg = if (durations.isEmpty()) null
        else Duration.ofSeconds(durations.sumOf { it.seconds } / durations.size)
        return RangeAggregate(sessions = inRange.size, volumeKg = volume, avgDuration = avg)
    }
}
