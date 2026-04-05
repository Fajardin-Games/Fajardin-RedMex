package com.ustadmobile.meshrabiya.pokemon.model

import kotlinx.serialization.Serializable

@Serializable
data class PokemonEntry(
    val id: Int,
    val name: String,
    val types: List<String>,
    val description: String = "",
    val evolutionChainUrl: String = "",
    val obtainedFromIp: String? = null,
    val obtainedAtMs: Long = System.currentTimeMillis(),
)
