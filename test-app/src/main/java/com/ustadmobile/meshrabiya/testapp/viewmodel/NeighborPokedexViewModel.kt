package com.ustadmobile.meshrabiya.testapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ustadmobile.meshrabiya.pokemon.PokemonMeshClient
import com.ustadmobile.meshrabiya.pokemon.PokemonRepository
import com.ustadmobile.meshrabiya.pokemon.model.PokemonEntry
import com.ustadmobile.meshrabiya.pokemon.model.TradeOffer
import com.ustadmobile.meshrabiya.pokemon.model.TradeOfferStatus
import com.ustadmobile.meshrabiya.testapp.appstate.AppUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance
import java.util.UUID

data class NeighborPokedexUiState(
    val appUiState: AppUiState = AppUiState(title = "Pokédex del Vecino"),
    val entries: List<PokemonEntry> = emptyList(),
    val myEntries: List<PokemonEntry> = emptyList(),
    val targetIp: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

class NeighborPokedexViewModel(
    di: DI,
    val targetIp: String,
) : ViewModel() {

    private val client: PokemonMeshClient by di.instance()
    private val repository: PokemonRepository by di.instance()

    private val _uiState = MutableStateFlow(NeighborPokedexUiState(targetIp = targetIp))
    val uiState: StateFlow<NeighborPokedexUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                isLoading = true,
                appUiState = AppUiState(title = "Pokédex: $targetIp"),
            )
        }
        viewModelScope.launch {
            repository.userPokedex.collect { pokedex ->
                _uiState.update { it.copy(myEntries = pokedex.entries) }
            }
        }
        loadNeighborPokedex()
    }

    fun reload() = loadNeighborPokedex()

    private fun loadNeighborPokedex() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val pokedex = client.fetchPokedex(targetIp)
                _uiState.update { it.copy(entries = pokedex.entries, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun proposeTrade(requestedEntry: PokemonEntry, offeredPokemonId: Int) {
        viewModelScope.launch {
            val offer = TradeOffer(
                offerId = UUID.randomUUID().toString(),
                fromIp = repository.localIp,
                toIp = targetIp,
                offeredPokemonId = offeredPokemonId,
                requestedPokemonId = requestedEntry.id,
                status = TradeOfferStatus.PENDING,
            )
            repository.addOutgoingOffer(offer)
            try {
                client.sendTradeOffer(targetIp, offer)
                _uiState.update {
                    it.copy(successMessage = "¡Propuesta enviada a $targetIp!")
                }
            } catch (e: Exception) {
                repository.updateOfferStatus(offer.offerId, TradeOfferStatus.FAILED)
                _uiState.update { it.copy(error = "Error al enviar propuesta: ${e.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
