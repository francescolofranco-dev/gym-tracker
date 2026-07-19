package dev.francescolofranco.gymtracker.ui.screens.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.francescolofranco.gymtracker.data.db.entities.TemplateEntity
import dev.francescolofranco.gymtracker.data.repository.TemplateRepository
import dev.francescolofranco.gymtracker.ui.components.Loadable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val repo: TemplateRepository,
) : ViewModel() {

    val templates: StateFlow<Loadable<List<TemplateEntity>>> = repo.observeAll()
        .map { Loadable.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Loadable.Loading)

    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }
}
