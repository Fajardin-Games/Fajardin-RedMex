package com.ustadmobile.meshrabiya.testapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ustadmobile.meshrabiya.pokemon.model.PokemonEntry
import com.ustadmobile.meshrabiya.testapp.ViewModelFactory
import com.ustadmobile.meshrabiya.testapp.appstate.AppUiState
import com.ustadmobile.meshrabiya.testapp.theme.MeshAmber
import com.ustadmobile.meshrabiya.testapp.theme.MeshBackground
import com.ustadmobile.meshrabiya.testapp.viewmodel.NeighborPokedexUiState
import com.ustadmobile.meshrabiya.testapp.viewmodel.NeighborPokedexViewModel
import org.kodein.di.compose.localDI

@Composable
fun NeighborPokedexScreen(
    targetIp: String,
    onSetAppUiState: (AppUiState) -> Unit,
    viewModel: NeighborPokedexViewModel = viewModel(
        key = "neighborpokedex_$targetIp",
        factory = ViewModelFactory(
            di = localDI(),
            owner = LocalSavedStateRegistryOwner.current,
            vmFactory = { NeighborPokedexViewModel(it, targetIp) },
            defaultArgs = null,
        )
    ),
) {
    val uiState by viewModel.uiState.collectAsState(NeighborPokedexUiState())
    LaunchedEffect(uiState.appUiState) { onSetAppUiState(uiState.appUiState) }

    NeighborPokedexScreen(
        uiState = uiState,
        onProposeTrade = { requested, offeredId -> viewModel.proposeTrade(requested, offeredId) },
        onDismissMessage = { viewModel.clearMessages() },
    )
}

@Composable
fun NeighborPokedexScreen(
    uiState: NeighborPokedexUiState,
    onProposeTrade: (PokemonEntry, Int) -> Unit = { _, _ -> },
    onDismissMessage: () -> Unit = {},
) {
    // Dialog state: which neighbor pokemon was tapped
    var selectedNeighborPokemon by remember { mutableStateOf<PokemonEntry?>(null) }
    var selectedMyPokemonId by remember { mutableStateOf(-1) }

    // Feedback dialogs
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissMessage,
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = onDismissMessage) { Text("OK") }
            }
        )
    }
    uiState.successMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = onDismissMessage,
            title = { Text("¡Listo!") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = onDismissMessage) { Text("OK") }
            }
        )
    }

    // Trade proposal dialog
    selectedNeighborPokemon?.let { requested ->
        TradeProposalDialog(
            requestedPokemon = requested,
            myPokemon = uiState.myEntries,
            selectedId = selectedMyPokemonId,
            onSelectMyPokemon = { selectedMyPokemonId = it },
            onConfirm = {
                if (selectedMyPokemonId != -1) {
                    onProposeTrade(requested, selectedMyPokemonId)
                    selectedNeighborPokemon = null
                    selectedMyPokemonId = -1
                }
            },
            onDismiss = {
                selectedNeighborPokemon = null
                selectedMyPokemonId = -1
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MeshBackground)) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MeshAmber,
                )
            }
            uiState.entries.isEmpty() -> {
                Text(
                    text = "Este entrenador no tiene Pokémon aún.",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = uiState.entries, key = { it.id }) { pokemon ->
                        PokemonCard(
                            pokemon = pokemon,
                            trailingContent = {
                                OutlinedButton(
                                    onClick = {
                                        selectedNeighborPokemon = pokemon
                                        selectedMyPokemonId = -1
                                    },
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .widthIn(max = 110.dp),
                                ) {
                                    Text(
                                        "Proponer",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                    )
                                }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TradeProposalDialog(
    requestedPokemon: PokemonEntry,
    myPokemon: List<PokemonEntry>,
    selectedId: Int,
    onSelectMyPokemon: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Ofrecer intercambio")
        },
        text = {
            Column {
                Text(
                    text = "Quieres: ${requestedPokemon.name.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "¿Qué ofreces a cambio?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (myPokemon.isEmpty()) {
                    Text(
                        text = "No tienes Pokémon para ofrecer.",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    myPokemon.forEach { mine ->
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selectedId == mine.id)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(8.dp),
                                )
                                .padding(4.dp)
                        ) {
                            RadioButton(
                                selected = selectedId == mine.id,
                                onClick = { onSelectMyPokemon(mine.id) },
                            )
                            Text(
                                text = mine.name.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = selectedId != -1 && myPokemon.isNotEmpty(),
            ) {
                Text("Enviar propuesta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
