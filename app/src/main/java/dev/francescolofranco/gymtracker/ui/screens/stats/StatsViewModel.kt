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
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.domain.workoutDuration
import dev.francescolofranco.gymtracker.domain.workoutStartedAt
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.RetryableViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
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
    val period: StatsPeriod,
    val unit: WeightUnit,
    val muscleVolumes: Map<Muscle, MuscleVolume>,
    /** Same shape as [muscleVolumes] but for the period immediately before [period]. Drives deltas. */
    val previousMuscleVolumes: Map<Muscle, MuscleVolume>,
    val currentPeriod: RangeAggregate,
    val previousPeriod: RangeAggregate,
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
) : RetryableViewModel() {

    private val _selectedMuscle = MutableStateFlow<Muscle?>(null)
    val selectedMuscle: StateFlow<Muscle?> = _selectedMuscle

    private val _selectedPeriod = MutableStateFlow(StatsPeriod.DAYS_7)
    val selectedPeriod: StateFlow<StatsPeriod> = _selectedPeriod

    @OptIn(ExperimentalCoroutinesApi::class)
    private val periodData = _selectedPeriod
        .flatMapLatest { period ->
            val current = periodRange(period)
            val prev = previousOf(current)
            combine(
                sessionDao.observeLoggedSetsBetween(current.startInclusive, current.endExclusive),
                sessionDao.observeLoggedSetsBetween(prev.startInclusive, prev.endExclusive),
            ) { rows, previousRows -> PeriodData(period, rows, previousRows) }
        }

    @OptIn(FlowPreview::class)
    val uiState: StateFlow<Loadable<StatsUiState>> = combine(
        periodData,
        prefs.unit,
        repo.observeAllSummaries(),
    ) { periodData, unit, summaries ->
        val now = Instant.now()
        val current = periodRange(periodData.period, now)
        val previous = previousOf(current)
        val m = monthRange(now)
        val pm = previousMonthRange(now)
        val y = yearRange(now)
        val py = previousYearRange(now)
        StatsUiState(
            period = periodData.period,
            unit = unit,
            muscleVolumes = computeMuscleVolumes(periodData.rows),
            previousMuscleVolumes = computeMuscleVolumes(periodData.previousRows),
            currentPeriod = aggregate(summaries, current),
            previousPeriod = aggregate(summaries, previous),
            month = aggregate(summaries, m),
            previousMonth = aggregate(summaries, pm),
            year = aggregate(summaries, y),
            previousYear = aggregate(summaries, py),
        )
    }.debounce(24L)
        .asLoadableState(viewModelScope)

    fun selectMuscle(m: Muscle?) {
        _selectedMuscle.value = m
    }

    fun setPeriod(period: StatsPeriod) {
        _selectedPeriod.value = period
    }

    private fun aggregate(all: List<SessionSummary>, range: DateRange): RangeAggregate {
        val inRange = all.filter {
            val t = it.session.workoutStartedAt()
            !t.isBefore(range.startInclusive) && t.isBefore(range.endExclusive)
        }
        val volume = inRange.sumOf { it.totalVolume }
        val durations = inRange.mapNotNull { s ->
            s.session.endedAt?.let { s.session.workoutDuration() }
        }
        val avg = if (durations.isEmpty()) null
        else Duration.ofSeconds(durations.sumOf { it.seconds } / durations.size)
        return RangeAggregate(sessions = inRange.size, volumeKg = volume, avgDuration = avg)
    }

    private data class PeriodData(
        val period: StatsPeriod,
        val rows: List<StatSetRow>,
        val previousRows: List<StatSetRow>,
    )
}
