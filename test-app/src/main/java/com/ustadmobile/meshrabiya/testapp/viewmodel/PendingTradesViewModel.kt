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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

data class PendingTradesUiState(
    val appUiState: AppUiState = AppUiState(title = "Intercambios Pendientes"),
    val incomingOffers: List<TradeOffer> = emptyList(),
    val outgoingOffers: List<TradeOffer> = emptyList(),
    val myPokedex: List<PokemonEntry> = emptyList(),
    val actionError: String? = null,
)

class PendingTradesViewModel(di: DI) : ViewModel() {

    private val repository: PokemonRepository by di.instance()
    private val client: PokemonMeshClient by di.instance()

    private val _uiState = MutableStateFlow(PendingTradesUiState())
    val uiState: StateFlow<PendingTradesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.incomingOffers,
                repository.outgoingOffers,
                repository.userPokedex,
            ) { incoming, outgoing, pokedex ->
                Triple(incoming, outgoing, pokedex.entries)
            }.collect { (incoming, outgoing, entries) ->
                _uiState.update {
                    it.copy(
                        incomingOffers = incoming.filter { o -> o.status == TradeOfferStatus.PENDING },
                        outgoingOffers = outgoing,
                        myPokedex = entries,
                    )
                }
            }
        }
    }

    fun acceptOffer(offer: TradeOffer, myOfferedPokemonId: Int) {
        viewModelScope.launch {
            val myEntry = repository.userPokedex.value.entries.firstOrNull { it.id == myOfferedPokemonId }
                ?: return@launch

            repository.updateOfferStatus(offer.offerId, TradeOfferStatus.ACCEPTED)
            try {
                client.sendAccept(offer.fromIp, offer.offerId, myEntry)
                // completeTrade se ejecutará cuando el iniciador llame /trade/complete en nuestro servidor
            } catch (e: Exception) {
                repository.updateOfferStatus(offer.offerId, TradeOfferStatus.FAILED)
                _uiState.update { it.copy(actionError = "Error al aceptar: ${e.message}") }
            }
        }
    }

    fun declineOffer(offer: TradeOffer) {
        viewModelScope.launch {
            repository.updateOfferStatus(offer.offerId, TradeOfferStatus.DECLINED)
            try {
                client.sendDecline(offer.fromIp, offer.offerId)
            } catch (e: Exception) {
                // best-effort
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(actionError = null) }
    }
}
