package com.ustadmobile.meshrabiya.pokemon

import android.util.Log
import com.ustadmobile.meshrabiya.log.MNetLogger
import com.ustadmobile.meshrabiya.pokemon.model.PokemonEntry
import com.ustadmobile.meshrabiya.pokemon.model.TradeOffer
import com.ustadmobile.meshrabiya.pokemon.model.TradeOfferStatus
import com.ustadmobile.meshrabiya.pokemon.model.UserPokedex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.Closeable
import java.io.File

class PokemonRepository(
    private val pokedexFile: File,
    private val json: Json,
    val localIp: String,
    private val logger: MNetLogger? = null,
) : Closeable {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _userPokedex = MutableStateFlow(UserPokedex(ownerIp = localIp))
    val userPokedex: StateFlow<UserPokedex> = _userPokedex.asStateFlow()

    private val _incomingOffers = MutableStateFlow(emptyList<TradeOffer>())
    val incomingOffers: StateFlow<List<TradeOffer>> = _incomingOffers.asStateFlow()

    private val _outgoingOffers = MutableStateFlow(emptyList<TradeOffer>())
    val outgoingOffers: StateFlow<List<TradeOffer>> = _outgoingOffers.asStateFlow()

    init {
        if (pokedexFile.exists()) {
            try {
                val loaded = json.decodeFromString<UserPokedex>(pokedexFile.readText())
                _userPokedex.value = loaded.copy(ownerIp = localIp)
            } catch (e: Exception) {
                logger?.invoke(Log.WARN, "[PokemonRepository] Failed to load pokedex", e)
            }
        }
    }

    fun hasAnyPokemon(): Boolean = _userPokedex.value.entries.isNotEmpty()

    fun findById(id: Int): PokemonEntry? = _userPokedex.value.entries.firstOrNull { it.id == id }

    fun addPokemon(entry: PokemonEntry) {
        _userPokedex.update { prev ->
            if (prev.entries.any { it.id == entry.id }) prev
            else prev.copy(
                entries = prev.entries + entry,
                lastUpdatedMs = System.currentTimeMillis(),
            )
        }
        persistPokedex()
    }

    fun removePokemon(id: Int) {
        _userPokedex.update { prev ->
            prev.copy(
                entries = prev.entries.filter { it.id != id },
                lastUpdatedMs = System.currentTimeMillis(),
            )
        }
        persistPokedex()
    }

    fun onIncomingOffer(offer: TradeOffer) {
        _incomingOffers.update { prev ->
            if (prev.any { it.offerId == offer.offerId }) prev
            else prev + offer
        }
    }

    fun addOutgoingOffer(offer: TradeOffer) {
        _outgoingOffers.update { prev ->
            if (prev.any { it.offerId == offer.offerId }) prev
            else prev + offer
        }
    }

    fun updateOfferStatus(offerId: String, newStatus: TradeOfferStatus) {
        _incomingOffers.update { prev ->
            prev.map { if (it.offerId == offerId) it.copy(status = newStatus) else it }
        }
        _outgoingOffers.update { prev ->
            prev.map { if (it.offerId == offerId) it.copy(status = newStatus) else it }
        }
    }

    fun completeTrade(offerId: String, received: PokemonEntry, givenId: Int) {
        _userPokedex.update { prev ->
            val withoutGiven = prev.entries.filter { it.id != givenId }
            val withReceived = if (withoutGiven.any { it.id == received.id }) withoutGiven
                               else withoutGiven + received
            prev.copy(entries = withReceived, lastUpdatedMs = System.currentTimeMillis())
        }
        persistPokedex()
        updateOfferStatus(offerId, TradeOfferStatus.COMPLETED)
    }

    fun buildUserPokedex(): UserPokedex = _userPokedex.value.copy(ownerIp = localIp)

    private fun persistPokedex() {
        val snapshot = _userPokedex.value
        scope.launch {
            try {
                pokedexFile.writeText(json.encodeToString(UserPokedex.serializer(), snapshot))
            } catch (e: Exception) {
                logger?.invoke(Log.ERROR, "[PokemonRepository] Failed to persist pokedex", e)
            }
        }
    }

    override fun close() {
        scope.cancel()
    }
}
