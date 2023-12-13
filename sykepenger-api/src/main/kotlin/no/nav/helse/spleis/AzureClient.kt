package no.nav.helse.spleis

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.parameters
import kotlinx.coroutines.runBlocking

class AzureClient(
    private val httpClient: HttpClient,
    private val tokenEndpoint: String,
    private val clientId: String,
    private val clientSecret: String,
    private val objectMapper: ObjectMapper
) {

    suspend fun veksleTilOnBehalfOf(token: String, scope: String): String {
        return hentTokenFraAzure(token, scope)
    }

    private suspend fun hentTokenFraAzure(token: String, scope: String): String {
        val response = httpClient.post(tokenEndpoint) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(FormDataContent(parameters {
                append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                append("client_id", clientId)
                append("client_secret", clientSecret)
                append("assertion", token)
                append("scope", scope)
                append("requested_token_use", "on_behalf_of")
            }))
        }
        val body = response.bodyAsText()
        val json = objectMapper.readTree(body)
        return json.path("access_token").asText()
    }
}