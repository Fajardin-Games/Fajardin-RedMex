package com.ustadmobile.meshrabiya.testapp.pokemon

import com.ustadmobile.meshrabiya.pokemon.model.PokemonEntry

object StarterPokemon {
    val BULBASAUR = PokemonEntry(
        id = 1,
        name = "bulbasaur",
        types = listOf("grass", "poison"),
        description = "A strange seed was planted on its back at birth. The plant sprouts and grows with this Pokémon.",
        evolutionChainUrl = "https://pokeapi.co/api/v2/evolution-chain/1/",
    )
    val CHARMANDER = PokemonEntry(
        id = 4,
        name = "charmander",
        types = listOf("fire"),
        description = "The flame on its tail indicates Charmander's life force. If it is healthy, the flame burns brightly.",
        evolutionChainUrl = "https://pokeapi.co/api/v2/evolution-chain/2/",
    )
    val SQUIRTLE = PokemonEntry(
        id = 7,
        name = "squirtle",
        types = listOf("water"),
        description = "After birth, its back swells and hardens into a shell. It sprays a powerful stream of water.",
        evolutionChainUrl = "https://pokeapi.co/api/v2/evolution-chain/3/",
    )
    val ALL = listOf(BULBASAUR, CHARMANDER, SQUIRTLE)
}
