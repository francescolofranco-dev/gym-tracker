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
import dev.francescolofranco.gymtracker.ui.components.Loadable
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
    private val weekData = prefs.weekMode
        .flatMapLatest { mode ->
            val current = weekRange(mode)
            val prev = previousOf(weekRange(mode))
            combine(
                sessionDao.observeLoggedSetsBetween(current.startInclusive, current.endExclusive),
                sessionDao.observeLoggedSetsBetween(prev.startInclusive, prev.endExclusive),
            ) { rows, previousRows -> WeekData(mode, rows, previousRows) }
        }

    @OptIn(FlowPreview::class)
    val uiState: StateFlow<Loadable<StatsUiState>> = combine(
        weekData,
        prefs.unit,
        repo.observeAllSummaries(),
    ) { weekData, unit, summaries ->
        val now = Instant.now()
        val mode = weekData.mode
        val w = weekRange(mode, now)
        val pw = previousOf(w)
        val m = monthRange(now)
        val pm = previousMonthRange(now)
        val y = yearRange(now)
        val py = previousYearRange(now)
        StatsUiState(
            weekMode = mode,
            unit = unit,
            muscleVolumes = computeMuscleVolumes(weekData.rows),
            previousMuscleVolumes = computeMuscleVolumes(weekData.previousRows),
            week = aggregate(summaries, w),
            previousWeek = aggregate(summaries, pw),
            month = aggregate(summaries, m),
            previousMonth = aggregate(summaries, pm),
            year = aggregate(summaries, y),
            previousYear = aggregate(summaries, py),
        )
    }.debounce(24L)
        .map { Loadable.Ready(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            Loadable.Loading,
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

    private data class WeekData(
        val mode: WeekMode,
        val rows: List<StatSetRow>,
        val previousRows: List<StatSetRow>,
    )
}
