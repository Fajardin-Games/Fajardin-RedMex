package com.ustadmobile.meshrabiya.pokemon.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPokedex(
    val ownerIp: String = "",
    val entries: List<PokemonEntry> = emptyList(),
    val lastUpdatedMs: Long = System.currentTimeMillis(),
)
