package com.ustadmobile.meshrabiya.pokemon.model

import kotlinx.serialization.Serializable

@Serializable
data class TradeOffer(
    val offerId: String,
    val fromIp: String,
    val toIp: String,
    val offeredPokemonId: Int,
    val requestedPokemonId: Int,
    val status: TradeOfferStatus = TradeOfferStatus.PENDING,
    val createdAtMs: Long = System.currentTimeMillis(),
)
