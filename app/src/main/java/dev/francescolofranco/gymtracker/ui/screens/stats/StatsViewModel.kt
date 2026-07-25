package dev.francescolofranco.gymtracker.ui.screens.stats

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

data class PeriodAggregate(
    val sessions: Int,
    val loggedSets: Int,
    val trainingTime: Duration,
)

data class StatsUiState(
    val period: StatsPeriod,
    val unit: WeightUnit,
    val muscleVolumes: Map<Muscle, MuscleVolume>,
    val previousMuscleVolumes: Map<Muscle, MuscleVolume>,
    val currentPeriod: PeriodAggregate,
    val previousPeriod: PeriodAggregate,
    val muscleChanges: List<MuscleChange>,
    val exerciseProgress: List<ExerciseProgressSignal>,
    val personalRecords: PersonalRecordActivity,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val sessionDao: SessionDao,
    private val repo: SessionRepository,
    prefs: UserPrefs,
) : RetryableViewModel() {

    private val _selectedMuscle = MutableStateFlow<Muscle?>(null)
    val selectedMuscle: StateFlow<Muscle?> = _selectedMuscle

    private val _selectedPeriod = MutableStateFlow(StatsPeriod.DAYS_7)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val periodData = _selectedPeriod.flatMapLatest { period ->
        val current = periodRange(period)
        val previous = previousOf(current)
        combine(
            sessionDao.observeLoggedSetsBetween(current.startInclusive, current.endExclusive),
            sessionDao.observeLoggedSetsBetween(previous.startInclusive, previous.endExclusive),
            sessionDao.observeLoggedSetsBetween(Instant.EPOCH, current.endExclusive),
        ) { rows, previousRows, historyRows ->
            PeriodData(period, current, previous, rows, previousRows, historyRows)
        }
    }

    @OptIn(FlowPreview::class)
    val uiState: StateFlow<Loadable<StatsUiState>> = combine(
        periodData,
        prefs.unit,
        repo.observeAllSummaries(),
    ) { data, unit, summaries ->
        val currentVolumes = computeMuscleVolumes(data.rows)
        val previousVolumes = computeMuscleVolumes(data.previousRows)
        StatsUiState(
            period = data.period,
            unit = unit,
            muscleVolumes = currentVolumes,
            previousMuscleVolumes = previousVolumes,
            currentPeriod = aggregate(summaries, data.currentRange, data.rows.size),
            previousPeriod = aggregate(summaries, data.previousRange, data.previousRows.size),
            muscleChanges = meaningfulMuscleChanges(currentVolumes, previousVolumes, data.period),
            exerciseProgress = computeExerciseProgress(data.rows, data.previousRows),
            personalRecords = personalRecordActivity(data.historyRows, data.currentRange),
        )
    }
        .flowOn(Dispatchers.Default)
        .debounce(24L)
        .asLoadableState(viewModelScope)

    fun selectMuscle(muscle: Muscle?) {
        _selectedMuscle.value = muscle
    }

    fun setPeriod(period: StatsPeriod) {
        _selectedPeriod.value = period
    }

    private fun aggregate(
        all: List<SessionSummary>,
        range: DateRange,
        loggedSets: Int,
    ): PeriodAggregate {
        val sessions = all.filter {
            val startedAt = it.session.workoutStartedAt()
            !startedAt.isBefore(range.startInclusive) && startedAt.isBefore(range.endExclusive)
        }
        val trainingSeconds = sessions.sumOf { summary ->
            if (summary.session.endedAt == null) 0L else summary.session.workoutDuration().seconds
        }
        return PeriodAggregate(
            sessions = sessions.size,
            loggedSets = loggedSets,
            trainingTime = Duration.ofSeconds(trainingSeconds),
        )
    }

    private data class PeriodData(
        val period: StatsPeriod,
        val currentRange: DateRange,
        val previousRange: DateRange,
        val rows: List<StatSetRow>,
        val previousRows: List<StatSetRow>,
        val historyRows: List<StatSetRow>,
    )
}
