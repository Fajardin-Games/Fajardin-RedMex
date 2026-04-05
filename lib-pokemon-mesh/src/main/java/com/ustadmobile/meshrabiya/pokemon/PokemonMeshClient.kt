package com.ustadmobile.meshrabiya.pokemon

import android.util.Log
import com.ustadmobile.meshrabiya.log.MNetLogger
import com.ustadmobile.meshrabiya.pokemon.model.PokemonEntry
import com.ustadmobile.meshrabiya.pokemon.model.TradeOffer
import com.ustadmobile.meshrabiya.pokemon.model.UserPokedex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class PokemonMeshClient(
    private val httpClient: OkHttpClient,
    private val json: Json,
    private val logger: MNetLogger? = null,
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchPokedex(targetIp: String, port: Int = DEFAULT_PORT): UserPokedex {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("http://$targetIp:$port/pokedex")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response from $targetIp")
            json.decodeFromString<UserPokedex>(body)
        }
    }

    suspend fun sendTradeOffer(targetIp: String, offer: TradeOffer, port: Int = DEFAULT_PORT) {
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(TradeOffer.serializer(), offer).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("http://$targetIp:$port/trade/offer")
                .post(body)
                .build()
            val response = httpClient.newCall(request).execute()
            logger?.invoke(Log.INFO, "[PokemonMeshClient] sendTradeOffer to $targetIp: ${response.code}")
        }
    }

    suspend fun sendAccept(
        initiatorIp: String,
        offerId: String,
        myEntry: PokemonEntry,
        port: Int = DEFAULT_PORT,
    ) {
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(PokemonEntry.serializer(), myEntry).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("http://$initiatorIp:$port/trade/accept/$offerId")
                .post(body)
                .build()
            val response = httpClient.newCall(request).execute()
            logger?.invoke(Log.INFO, "[PokemonMeshClient] sendAccept to $initiatorIp: ${response.code}")
        }
    }

    suspend fun sendComplete(
        targetIp: String,
        offerId: String,
        myEntry: PokemonEntry,
        port: Int = DEFAULT_PORT,
    ) {
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(PokemonEntry.serializer(), myEntry).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("http://$targetIp:$port/trade/complete/$offerId")
                .post(body)
                .build()
            val response = httpClient.newCall(request).execute()
            logger?.invoke(Log.INFO, "[PokemonMeshClient] sendComplete to $targetIp: ${response.code}")
        }
    }

    suspend fun sendDecline(initiatorIp: String, offerId: String, port: Int = DEFAULT_PORT) {
        withContext(Dispatchers.IO) {
            val body = "".toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("http://$initiatorIp:$port/trade/decline/$offerId")
                .post(body)
                .build()
            val response = httpClient.newCall(request).execute()
            logger?.invoke(Log.INFO, "[PokemonMeshClient] sendDecline to $initiatorIp: ${response.code}")
        }
    }

    companion object {
        const val DEFAULT_PORT = 4243
    }
}
