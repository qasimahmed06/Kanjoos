package com.example.kanjoos.data

import com.example.kanjoos.model.Transaction
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

class TransactionRepository(private val client: io.ktor.client.HttpClient = SupabaseClient.httpClient) {

    private val baseUrl = SupabaseClient.baseUrl.removeSuffix("/")
    private val endpoint = "$baseUrl/rest/v1/transactions"

    suspend fun getAllTransactions(): List<Transaction> {
        val response = client.get {
            url(endpoint)
            header("apikey", SupabaseClient.apiKey)
            header("Authorization", "Bearer ${SupabaseClient.apiKey}")
            header("Accept", "application/json")
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Supabase fetch failed (${response.status.value}): ${response.bodyAsText()}")
        }

        val bodyText = response.bodyAsText()
        val json = Json { ignoreUnknownKeys = true }
        val parsed = json.parseToJsonElement(bodyText)
        if (parsed !is JsonArray) {
            throw IllegalStateException("Supabase fetch returned an error payload instead of a list: $bodyText")
        }

        return json.decodeFromString(ListSerializer(Transaction.serializer()), bodyText)
    }

    suspend fun insertTransaction(transaction: Transaction): Unit {
        client.post {
            url(endpoint)
            header("apikey", SupabaseClient.apiKey)
            header("Authorization", "Bearer ${SupabaseClient.apiKey}")
            contentType(ContentType.Application.Json)
            header("Prefer", "return=minimal")
            setBody(transaction)
        }
    }
}
