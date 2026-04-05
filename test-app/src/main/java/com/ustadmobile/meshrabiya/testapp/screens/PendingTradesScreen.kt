package com.ustadmobile.meshrabiya.testapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ustadmobile.meshrabiya.pokemon.model.PokemonEntry
import com.ustadmobile.meshrabiya.pokemon.model.TradeOffer
import com.ustadmobile.meshrabiya.pokemon.model.TradeOfferStatus
import com.ustadmobile.meshrabiya.testapp.ViewModelFactory
import com.ustadmobile.meshrabiya.testapp.appstate.AppUiState
import com.ustadmobile.meshrabiya.testapp.theme.MeshAmber
import com.ustadmobile.meshrabiya.testapp.theme.MeshBackground
import com.ustadmobile.meshrabiya.testapp.theme.MeshSurface
import com.ustadmobile.meshrabiya.testapp.viewmodel.PendingTradesUiState
import com.ustadmobile.meshrabiya.testapp.viewmodel.PendingTradesViewModel
import org.kodein.di.compose.localDI

@Composable
fun PendingTradesScreen(
    onSetAppUiState: (AppUiState) -> Unit,
    viewModel: PendingTradesViewModel = viewModel(
        factory = ViewModelFactory(
            di = localDI(),
            owner = LocalSavedStateRegistryOwner.current,
            vmFactory = { PendingTradesViewModel(it) },
            defaultArgs = null,
        )
    ),
) {
    val uiState by viewModel.uiState.collectAsState(PendingTradesUiState())
    LaunchedEffect(uiState.appUiState) { onSetAppUiState(uiState.appUiState) }

    PendingTradesScreen(
        uiState = uiState,
        onAccept = { offer, myId -> viewModel.acceptOffer(offer, myId) },
        onDecline = { offer -> viewModel.declineOffer(offer) },
        onDismissError = { viewModel.clearError() },
    )
}

@Composable
fun PendingTradesScreen(
    uiState: PendingTradesUiState,
    onAccept: (TradeOffer, Int) -> Unit = { _, _ -> },
    onDecline: (TradeOffer) -> Unit = {},
    onDismissError: () -> Unit = {},
) {
    var acceptTarget by remember { mutableStateOf<TradeOffer?>(null) }
    var selectedMyPokemonId by remember { mutableStateOf(-1) }

    uiState.actionError?.let { err ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("Error") },
            text = { Text(err) },
            confirmButton = { TextButton(onClick = onDismissError) { Text("OK") } },
        )
    }

    acceptTarget?.let { offer ->
        AcceptOfferDialog(
            offer = offer,
            myPokemon = uiState.myPokedex,
            selectedId = selectedMyPokemonId,
            onSelectMyPokemon = { selectedMyPokemonId = it },
            onConfirm = {
                if (selectedMyPokemonId != -1) {
                    onAccept(offer, selectedMyPokemonId)
                    acceptTarget = null
                    selectedMyPokemonId = -1
                }
            },
            onDismiss = {
                acceptTarget = null
                selectedMyPokemonId = -1
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MeshBackground),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "incoming_header") {
            SectionHeader(title = "Recibidos (${uiState.incomingOffers.size})")
        }

        if (uiState.incomingOffers.isEmpty()) {
            item(key = "no_incoming") {
                Text(
                    text = "Sin propuestas entrantes.",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            items(items = uiState.incomingOffers, key = { it.offerId }) { offer ->
                IncomingOfferCard(
                    offer = offer,
                    myPokedex = uiState.myPokedex,
                    onAccept = {
                        val requested = uiState.myPokedex.firstOrNull { p -> p.id == offer.requestedPokemonId }
                        if (requested != null) {
                            acceptTarget = offer
                            selectedMyPokemonId = requested.id
                        } else {
                            acceptTarget = offer
                            selectedMyPokemonId = -1
                        }
                    },
                    onDecline = { onDecline(offer) },
                )
            }
        }

        item(key = "outgoing_header") {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = "Enviados (${uiState.outgoingOffers.size})")
        }

        if (uiState.outgoingOffers.isEmpty()) {
            item(key = "no_outgoing") {
                Text(
                    text = "Sin propuestas enviadas.",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            items(items = uiState.outgoingOffers, key = { it.offerId }) { offer ->
                OutgoingOfferCard(offer = offer)
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MeshAmber,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

@Composable
private fun TradeSpriteRow(offeredId: Int, requestedId: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AsyncImage(
            model = pokemonSpriteUrl(offeredId),
            contentDescription = "Pokémon #$offeredId",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp, maxWidth = 64.dp, maxHeight = 64.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        AsyncImage(
            model = pokemonSpriteUrl(requestedId),
            contentDescription = "Pokémon #$requestedId",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp, maxWidth = 64.dp, maxHeight = 64.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
    }
}

@Composable
private fun IncomingOfferCard(
    offer: TradeOffer,
    myPokedex: List<PokemonEntry>,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val ownsRequested = myPokedex.any { it.id == offer.requestedPokemonId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MeshSurface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Sender info
        Text(
            text = "De: ${offer.fromIp}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // Sprites — offered → requested
        TradeSpriteRow(offeredId = offer.offeredPokemonId, requestedId = offer.requestedPokemonId)

        // Trade labels
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Ofrecen: #${offer.offeredPokemonId}   Quieren: #${offer.requestedPokemonId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (!ownsRequested) {
                Text(
                    text = "No tienes el #${offer.requestedPokemonId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        // Action buttons — wrap if screen is narrow / font is large
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = onAccept,
                enabled = ownsRequested || myPokedex.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text("Aceptar", maxLines = 1)
            }
            OutlinedButton(
                onClick = onDecline,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.weight(1f),
            ) {
                Text("Declinar", maxLines = 1)
            }
        }
    }
}

@Composable
private fun OutgoingOfferCard(offer: TradeOffer) {
    val statusColor = when (offer.status) {
        TradeOfferStatus.COMPLETED -> androidx.compose.ui.graphics.Color(0xFF7AC74C)
        TradeOfferStatus.DECLINED, TradeOfferStatus.FAILED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusLabel = when (offer.status) {
        TradeOfferStatus.COMPLETED -> "Completado"
        TradeOfferStatus.DECLINED  -> "Rechazado"
        TradeOfferStatus.FAILED    -> "Fallido"
        TradeOfferStatus.ACCEPTED  -> "Aceptado"
        TradeOfferStatus.PENDING   -> "Pendiente"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MeshSurface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Sprites row + status badge on the right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TradeSpriteRow(
                offeredId = offer.offeredPokemonId,
                requestedId = offer.requestedPokemonId,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }

        // Info row
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Para: ${offer.toIp}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "#${offer.offeredPokemonId} → #${offer.requestedPokemonId}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun AcceptOfferDialog(
    offer: TradeOffer,
    myPokemon: List<PokemonEntry>,
    selectedId: Int,
    onSelectMyPokemon: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aceptar intercambio") },
        text = {
            Column {
                // Show sprites of the trade
                TradeSpriteRow(
                    offeredId = offer.offeredPokemonId,
                    requestedId = offer.requestedPokemonId,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Te ofrecen: #${offer.offeredPokemonId}  •  Quieren: #${offer.requestedPokemonId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (myPokemon.isEmpty()) {
                    Text(
                        text = "No tienes Pokémon para intercambiar.",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        text = "¿Cuál Pokémon entregas?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    myPokemon.forEach { mine ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedId == mine.id)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .padding(4.dp),
                        ) {
                            RadioButton(
                                selected = selectedId == mine.id,
                                onClick = { onSelectMyPokemon(mine.id) },
                            )
                            Text(
                                text = mine.name.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
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
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
