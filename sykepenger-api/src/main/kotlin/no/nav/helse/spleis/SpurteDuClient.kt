package no.nav.helse.spleis

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration


class SpurteDuClient(
    private val objectMapper: ObjectMapper
) {

    fun utveksleSpurteDu(token: String, id: String): String? {
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()

        val request = HttpRequest.newBuilder()
            .uri(URI("http://spurtedu/vis_meg/$id"))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $token")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        return try {
            objectMapper.readTree(response.body()).path("text").asText()
        } catch (err: JsonParseException) {
            null
        }
    }
}