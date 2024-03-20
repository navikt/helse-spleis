package no.nav.helse.spleis

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.azure.AzureTokenProvider
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.spleis.speil.SpekematDTO
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

class SpekematClient(
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val tokenProvider: AzureTokenProvider,
    private val scope: String,
    private val objectMapper: ObjectMapper,
    baseUrl: String? = null
) {
    private val baseUrl = baseUrl ?: "http://spekemat"
    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private const val CALL_ID_HEADER = "callId"
    }

    fun hentSpekemat(fnr: String, callId: String): SpekematDTO {
        "Henter data fra spekemat med {}".also {
            logg.info(it, kv("callId", callId))
            sikkerlogg.info(it, kv("callId", callId), kv("fødselsnummer", fnr))
        }
        val request = lagRequest(fnr, callId)
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        val responseBody = response.body()
        val statusCode = response.statusCode()
        sikkerlogg.info("mottok $statusCode:\n$responseBody", kv("fødselsnummer", fnr), kv("callId", callId))
        if (statusCode != 200) throw SpekematClientException("Fikk uventet http statuskode fra spekemat: $statusCode. Forventet HTTP 200 OK")
        return parsePølsefabrikker(responseBody)
    }

    private fun parsePølsefabrikker(body: String): SpekematDTO {
        return try {
            val response = objectMapper.readValue<PølserResponse>(body)
            SpekematDTO(
                pakker = response.yrkesaktiviteter.map { pakke ->
                    SpekematDTO.PølsepakkeDTO(
                        yrkesaktivitetidentifikator = pakke.yrkesaktivitetidentifikator,
                        rader = pakke.rader.map { rad ->
                            SpekematDTO.PølsepakkeDTO.PølseradDTO(
                                kildeTilRad = rad.kildeTilRad,
                                pølser = rad.pølser.map { pølse ->
                                    SpekematDTO.PølsepakkeDTO.PølseradDTO.PølseDTO(
                                        vedtaksperiodeId = pølse.vedtaksperiodeId,
                                        behandlingId = pølse.behandlingId,
                                        kilde = pølse.kilde,
                                        status = when (pølse.status) {
                                            Pølsestatus.ÅPEN -> SpekematDTO.PølsepakkeDTO.PølseradDTO.PølseDTO.PølsestatusDTO.ÅPEN
                                            Pølsestatus.LUKKET -> SpekematDTO.PølsepakkeDTO.PølseradDTO.PølseDTO.PølsestatusDTO.LUKKET
                                            Pølsestatus.FORKASTET -> SpekematDTO.PølsepakkeDTO.PølseradDTO.PølseDTO.PølsestatusDTO.FORKASTET
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
        } catch (err: Exception) {
            throw SpekematClientException("Feil ved deserialisering av spekemat-responsen", err)
        }
    }

    private fun lagRequest(fnr: String, callId: String): HttpRequest {
        @Language("JSON")
        val requestBody = """{ "fnr": "$fnr" }"""
        return HttpRequest.newBuilder(URI("$baseUrl/api/pølser"))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header(CALL_ID_HEADER, callId)
            .header("Authorization", "Bearer ${tokenProvider.bearerToken(scope).token}")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()
    }
}
class SpekematClientException(override val message: String, override val cause: Throwable? = null) : RuntimeException()

@JsonIgnoreProperties(ignoreUnknown = true)
private data class PølserResponse(val yrkesaktiviteter: List<YrkesaktivitetDto>)
private data class YrkesaktivitetDto(
    val yrkesaktivitetidentifikator: String,
    val rader: List<PølseradDto>
)
private data class PølseradDto(
    val pølser: List<PølseDto>,
    val kildeTilRad: UUID
)
private data class PølseDto(
    val vedtaksperiodeId: UUID,
    val behandlingId: UUID,
    val status: Pølsestatus,
    // tingen som gjorde at generasjonen ble opprettet
    val kilde: UUID
)
private enum class Pølsestatus { ÅPEN, LUKKET, FORKASTET }