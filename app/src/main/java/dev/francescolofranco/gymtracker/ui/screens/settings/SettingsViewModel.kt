package dev.francescolofranco.gymtracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.francescolofranco.gymtracker.data.prefs.UserPrefs
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.ui.components.Loadable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsContent(
    val unit: WeightUnit,
    val keepScreenOnDuringSession: Boolean,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPrefs,
) : ViewModel() {

    val content: StateFlow<Loadable<SettingsContent>> = combine(
        prefs.unit,
        prefs.keepScreenOnDuringSession,
    ) { unit, keepScreenOn ->
        Loadable.Ready(SettingsContent(unit, keepScreenOn))
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        Loadable.Loading,
    )

    fun setUnit(u: WeightUnit) = viewModelScope.launch { prefs.setUnit(u) }

    fun setKeepScreenOnDuringSession(enabled: Boolean) = viewModelScope.launch {
        prefs.setKeepScreenOnDuringSession(enabled)
    }
}
