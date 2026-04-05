package com.ustadmobile.meshrabiya.testapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ustadmobile.meshrabiya.pokemon.model.PokemonEntry
import com.ustadmobile.meshrabiya.testapp.pokemon.StarterPokemon
import com.ustadmobile.meshrabiya.testapp.theme.MeshAmber
import com.ustadmobile.meshrabiya.testapp.theme.MeshBackground

private val starterTypeColors = mapOf(
    "grass"  to Color(0xFF7AC74C),
    "poison" to Color(0xFFA33EA1),
    "fire"   to Color(0xFFEE8130),
    "water"  to Color(0xFF6390F0),
)

@Composable
fun StarterPickerScreen(onSelected: (PokemonEntry) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeshBackground)
            .verticalScroll(rememberScrollState())  // scrollable in case of very large fonts
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "¡Elige tu Pokémon inicial!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MeshAmber,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Solo se hace una vez. Después podrás conseguir más intercambiando con otros entrenadores.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))

        StarterPokemon.ALL.forEach { starter ->
            StarterCard(pokemon = starter, onClick = { onSelected(starter) })
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StarterCard(pokemon: PokemonEntry, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp),   // grows with font size, never clips
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp, vertical = 12.dp,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = pokemonSpriteUrl(pokemon.id),
                contentDescription = pokemon.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .sizeIn(minWidth = 56.dp, minHeight = 56.dp, maxWidth = 80.dp, maxHeight = 80.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pokemon.name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = pokemon.types.joinToString(" · ") { it.replaceFirstChar { c -> c.uppercase() } },
                    style = MaterialTheme.typography.labelMedium,
                    color = starterTypeColors[pokemon.types.firstOrNull()] ?: MeshAmber,
                )
            }
        }
    }
}
