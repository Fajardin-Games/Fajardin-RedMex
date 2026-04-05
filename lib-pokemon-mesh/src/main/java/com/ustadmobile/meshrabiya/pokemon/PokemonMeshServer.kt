package com.ustadmobile.meshrabiya.pokemon

import android.util.Log
import com.ustadmobile.meshrabiya.log.MNetLogger
import com.ustadmobile.meshrabiya.pokemon.model.PokemonEntry
import com.ustadmobile.meshrabiya.pokemon.model.TradeOffer
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.Closeable
import java.net.InetAddress

class PokemonMeshServer(
    private val repository: PokemonRepository,
    private val client: PokemonMeshClient,
    private val localVirtualAddr: InetAddress,
    private val json: Json,
    private val logger: MNetLogger? = null,
    port: Int = DEFAULT_PORT,
) : NanoHTTPD(port), Closeable {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val logPrefix = "[PokemonMeshServer]"

    override fun serve(session: IHTTPSession): Response {
        val path = session.uri
        logger?.invoke(Log.INFO, "$logPrefix ${session.method} $path")

        return try {
            when {
                path == "/pokedex" && session.method == Method.GET ->
                    handleGetPokedex()

                path == "/trade/offer" && session.method == Method.POST ->
                    handleIncomingOffer(session)

                path.startsWith("/trade/accept/") && session.method == Method.POST ->
                    handleAccept(session, path.removePrefix("/trade/accept/"))

                path.startsWith("/trade/complete/") && session.method == Method.POST ->
                    handleComplete(session, path.removePrefix("/trade/complete/"))

                path.startsWith("/trade/decline/") && session.method == Method.POST ->
                    handleDecline(path.removePrefix("/trade/decline/"))

                else -> newFixedLengthResponse(
                    Response.Status.NOT_FOUND, "text/plain", "not found: $path"
                )
            }
        } catch (e: Exception) {
            logger?.invoke(Log.ERROR, "$logPrefix Error handling $path", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, "text/plain", e.message ?: "Internal error"
            )
        }
    }

    private fun handleGetPokedex(): Response {
        val pokedex = repository.buildUserPokedex()
        val body = json.encodeToString(
            com.ustadmobile.meshrabiya.pokemon.model.UserPokedex.serializer(), pokedex
        )
        return newFixedLengthResponse(Response.Status.OK, "application/json", body)
    }

    private fun handleIncomingOffer(session: IHTTPSession): Response {
        val body = readBody(session)
        val offer = json.decodeFromString<TradeOffer>(body)
        repository.onIncomingOffer(offer)
        logger?.invoke(Log.INFO, "$logPrefix Incoming trade offer ${offer.offerId} from ${offer.fromIp}")
        return newFixedLengthResponse("OK")
    }

    private fun handleAccept(session: IHTTPSession, offerId: String): Response {
        val body = readBody(session)
        val responderEntry = json.decodeFromString<PokemonEntry>(body)

        val offer = repository.outgoingOffers.value.firstOrNull { it.offerId == offerId }
            ?: return newFixedLengthResponse(
                Response.Status.NOT_FOUND, "text/plain", "Offer $offerId not found"
            )

        val ourEntry = repository.findById(offer.offeredPokemonId)
            ?: return newFixedLengthResponse(
                Response.Status.CONFLICT, "text/plain",
                "Pokemon ${offer.offeredPokemonId} no longer owned"
            )

        repository.completeTrade(offerId, responderEntry, offer.offeredPokemonId)
        logger?.invoke(Log.INFO, "$logPrefix Trade $offerId completed (we accepted from ${offer.toIp})")

        // Enviar nuestro Pokemon al respondedor de forma asíncrona
        scope.launch {
            try {
                client.sendComplete(offer.toIp, offerId, ourEntry)
            } catch (e: Exception) {
                logger?.invoke(Log.ERROR, "$logPrefix sendComplete to ${offer.toIp} failed", e)
            }
        }

        return newFixedLengthResponse("OK")
    }

    private fun handleComplete(session: IHTTPSession, offerId: String): Response {
        val body = readBody(session)
        val initiatorEntry = json.decodeFromString<PokemonEntry>(body)

        val offer = repository.incomingOffers.value.firstOrNull { it.offerId == offerId }
            ?: return newFixedLengthResponse(
                Response.Status.NOT_FOUND, "text/plain", "Offer $offerId not found"
            )

        repository.completeTrade(offerId, initiatorEntry, offer.requestedPokemonId)
        logger?.invoke(Log.INFO, "$logPrefix Trade $offerId finalized — received ${initiatorEntry.name}")
        return newFixedLengthResponse("OK")
    }

    private fun handleDecline(offerId: String): Response {
        repository.updateOfferStatus(
            offerId,
            com.ustadmobile.meshrabiya.pokemon.model.TradeOfferStatus.DECLINED
        )
        logger?.invoke(Log.INFO, "$logPrefix Trade offer $offerId was declined")
        return newFixedLengthResponse("OK")
    }

    private fun readBody(session: IHTTPSession): String {
        val len = session.headers["content-length"]?.toInt() ?: 0
        if (len == 0) return ""
        val bytes = ByteArray(len)
        session.inputStream.read(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    override fun close() {
        stop()
        scope.cancel()
    }

    companion object {
        const val DEFAULT_PORT = 4243
    }
}
