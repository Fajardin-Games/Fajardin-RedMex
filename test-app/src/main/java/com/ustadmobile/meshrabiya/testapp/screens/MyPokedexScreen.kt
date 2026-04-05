package com.ustadmobile.meshrabiya.testapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ustadmobile.meshrabiya.pokemon.model.PokemonEntry
import com.ustadmobile.meshrabiya.testapp.ViewModelFactory
import com.ustadmobile.meshrabiya.testapp.appstate.AppUiState
import com.ustadmobile.meshrabiya.testapp.appstate.FabState
import com.ustadmobile.meshrabiya.testapp.theme.MeshAmber
import com.ustadmobile.meshrabiya.testapp.theme.MeshBackground
import com.ustadmobile.meshrabiya.testapp.theme.MeshSurface
import com.ustadmobile.meshrabiya.testapp.viewmodel.MyPokedexUiState
import com.ustadmobile.meshrabiya.testapp.viewmodel.MyPokedexViewModel
import org.kodein.di.compose.localDI

val pokemonTypeColors = mapOf(
    "grass"    to Color(0xFF7AC74C),
    "poison"   to Color(0xFFA33EA1),
    "fire"     to Color(0xFFEE8130),
    "water"    to Color(0xFF6390F0),
    "electric" to Color(0xFFF7D02C),
    "ice"      to Color(0xFF96D9D6),
    "fighting" to Color(0xFFC22E28),
    "ground"   to Color(0xFFE2BF65),
    "flying"   to Color(0xFFA98FF3),
    "psychic"  to Color(0xFFF95587),
    "bug"      to Color(0xFFA6B91A),
    "rock"     to Color(0xFFB6A136),
    "ghost"    to Color(0xFF735797),
    "dragon"   to Color(0xFF6F35FC),
    "dark"     to Color(0xFF705746),
    "steel"    to Color(0xFFB7B7CE),
    "fairy"    to Color(0xFFD685AD),
    "normal"   to Color(0xFFA8A77A),
)

fun pokemonSpriteUrl(id: Int) =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"

@Composable
fun MyPokedexScreen(
    onSetAppUiState: (AppUiState) -> Unit,
    onNavigateToTrades: () -> Unit = {},
    viewModel: MyPokedexViewModel = viewModel(
        factory = ViewModelFactory(
            di = localDI(),
            owner = LocalSavedStateRegistryOwner.current,
            vmFactory = { MyPokedexViewModel(it) },
            defaultArgs = null,
        )
    ),
) {
    val uiState by viewModel.uiState.collectAsState(MyPokedexUiState())

    LaunchedEffect(uiState.appUiState) {
        onSetAppUiState(
            uiState.appUiState.copy(
                fabState = FabState(
                    visible = true,
                    label = "Intercambios",
                    icon = Icons.Default.SwapHoriz,
                    onClick = onNavigateToTrades,
                )
            )
        )
    }

    MyPokedexScreen(uiState = uiState)
}

@Composable
fun MyPokedexScreen(uiState: MyPokedexUiState) {
    if (uiState.entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(MeshBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Tu Pokédex está vacía.\nIntercambia con otros entrenadores.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MeshBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = uiState.entries, key = { it.id }) { pokemon ->
            PokemonCard(pokemon = pokemon)
        }
        item { Spacer(modifier = Modifier.sizeIn(minHeight = 80.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PokemonCard(pokemon: PokemonEntry, trailingContent: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MeshSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Sprite — size scales with font but is capped
        AsyncImage(
            model = pokemonSpriteUrl(pokemon.id),
            contentDescription = pokemon.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp, maxWidth = 72.dp, maxHeight = 72.dp)
                .clip(RoundedCornerShape(8.dp)),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "#${pokemon.id.toString().padStart(3, '0')}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = pokemon.name.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            // FlowRow lets chips wrap to the next line with large fonts
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                pokemon.types.forEach { type ->
                    val color = pokemonTypeColors[type] ?: MeshAmber
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = type.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = color.copy(alpha = 0.2f),
                            labelColor = color,
                        ),
                    )
                }
            }
            if (pokemon.obtainedFromIp != null) {
                Text(
                    text = "De: ${pokemon.obtainedFromIp}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        trailingContent?.invoke()
    }
}
