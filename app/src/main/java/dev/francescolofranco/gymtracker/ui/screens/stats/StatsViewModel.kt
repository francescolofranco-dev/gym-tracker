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
    /** Same shape as [muscleVolumes] but for the period immediately before [week]. Drives deltas. */
    val previousMuscleVolumes: Map<Muscle, MuscleVolume>,
    val week: RangeAggregate,
    val previousWeek: RangeAggregate,
    val month: RangeAggregate,
    val previousMonth: RangeAggregate,
    val year: RangeAggregate,
    val previousYear: RangeAggregate,
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
        .flatMapLatest { mode ->
            val r = weekRange(mode)
            sessionDao.observeLoggedSetsBetween(r.startInclusive, r.endExclusive)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val previousWeekRows: StateFlow<List<StatSetRow>> = prefs.weekMode
        .flatMapLatest { mode ->
            val prev = previousOf(weekRange(mode))
            sessionDao.observeLoggedSetsBetween(prev.startInclusive, prev.endExclusive)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val muscleVolumes: StateFlow<Map<Muscle, MuscleVolume>> = weekRows
        .map { computeMuscleVolumes(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val previousMuscleVolumes: StateFlow<Map<Muscle, MuscleVolume>> = previousWeekRows
        .map { computeMuscleVolumes(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val uiState: StateFlow<StatsUiState> = combine(
        combine(prefs.weekMode, prefs.unit, muscleVolumes, previousMuscleVolumes) { mode, unit, volumes, prevVolumes ->
            Quad(mode, unit, volumes, prevVolumes)
        },
        repo.observeAllSummaries(),
    ) { quad, summaries ->
        val (mode, unit, volumes, prevVolumes) = quad
        val now = Instant.now()
        val w = weekRange(mode, now)
        val pw = previousOf(w)
        val m = monthRange(now)
        val pm = previousMonthRange(now)
        val y = yearRange(now)
        val py = previousYearRange(now)
        StatsUiState(
            weekMode = mode,
            unit = unit,
            muscleVolumes = volumes,
            previousMuscleVolumes = prevVolumes,
            week = aggregate(summaries, w),
            previousWeek = aggregate(summaries, pw),
            month = aggregate(summaries, m),
            previousMonth = aggregate(summaries, pm),
            year = aggregate(summaries, y),
            previousYear = aggregate(summaries, py),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        StatsUiState(
            weekMode = WeekMode.ROLLING_7,
            unit = WeightUnit.KG,
            muscleVolumes = Muscle.entries.associateWith { MuscleVolume(it, 0, 0, 0.0, 0.0, emptyList()) },
            previousMuscleVolumes = Muscle.entries.associateWith { MuscleVolume(it, 0, 0, 0.0, 0.0, emptyList()) },
            week = RangeAggregate(0, 0.0, null),
            previousWeek = RangeAggregate(0, 0.0, null),
            month = RangeAggregate(0, 0.0, null),
            previousMonth = RangeAggregate(0, 0.0, null),
            year = RangeAggregate(0, 0.0, null),
            previousYear = RangeAggregate(0, 0.0, null),
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

    private data class Quad(
        val mode: WeekMode,
        val unit: WeightUnit,
        val volumes: Map<Muscle, MuscleVolume>,
        val previousVolumes: Map<Muscle, MuscleVolume>,
    )
}
