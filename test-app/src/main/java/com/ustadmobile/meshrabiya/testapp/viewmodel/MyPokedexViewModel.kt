package com.ustadmobile.meshrabiya.testapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ustadmobile.meshrabiya.pokemon.PokemonRepository
import com.ustadmobile.meshrabiya.pokemon.model.PokemonEntry
import com.ustadmobile.meshrabiya.testapp.appstate.AppUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

data class MyPokedexUiState(
    val appUiState: AppUiState = AppUiState(title = "Mi Pokédex"),
    val entries: List<PokemonEntry> = emptyList(),
    val localIp: String = "",
)

class MyPokedexViewModel(di: DI) : ViewModel() {

    private val repository: PokemonRepository by di.instance()

    private val _uiState = MutableStateFlow(MyPokedexUiState())
    val uiState: StateFlow<MyPokedexUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(localIp = repository.localIp) }
        viewModelScope.launch {
            repository.userPokedex.collect { pokedex ->
                _uiState.update { prev ->
                    prev.copy(entries = pokedex.entries)
                }
            }
        }
    }
}
