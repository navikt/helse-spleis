package no.nav.helse.spleis.rest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.naisful.test.TestContext
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.every
import io.mockk.mockk
import java.net.URI
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.Personidentifikator
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.person.EventBus
import no.nav.helse.person.Person
import no.nav.helse.spleis.AbstractSpleisApiTest
import no.nav.helse.spleis.SpekematClient
import no.nav.helse.spleis.graphql.Spekemat
import no.nav.helse.spleis.objectMapper
import no.nav.helse.spleis.testhelpers.TestObservatør
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT

internal class PersonApiTest : AbstractSpleisApiTest() {

    @Test
    fun `ugyldig fødselsnummer gir 400`() = spleisApiTestApplication {
        hentPerson("""{"fødselsnummer":"tullball"}""", HttpStatusCode.BadRequest)
        hentPerson("""{}""", HttpStatusCode.BadRequest)
    }

    @Test
    fun `person som ikke finnes gir 404`() = spleisApiTestApplication {
        val body = hentPerson("""{"fødselsnummer":"40440440440"}""", HttpStatusCode.NotFound)
        val problem = objectMapper.readTree(body)
        assertEquals(404, problem.path("status").asInt())
    }

    @Test
    fun `REST-payloaden er identisk med GraphQL-payloaden`() {
        val spekemat = Spekemat()
        observatør = TestObservatør()
        val eventBus = EventBus().apply {
            register(spekemat)
            register(observatør)
        }
        person = Person(Personidentifikator(UNG_PERSON_FNR), UNG_PERSON_FØDSELSDATO.alder, EmptyLog)

        val spekematClient = mockk<SpekematClient>()
        spleisApiTestApplication(spekematClient = spekematClient, testdata = opprettTestdata(eventBus)) {
            every { spekematClient.hentSpekemat(UNG_PERSON_FNR, any()) } returns spekemat.resultat()

            val restBody = hentPerson("""{"fødselsnummer":"$UNG_PERSON_FNR"}""", HttpStatusCode.OK)
            val graphqlBody = hentPersonViaGraphQL()

            val forventet = objectMapper.readTree(graphqlBody.utenVariableVerdier)
                .path("data")
                .path("person")
                .somRestKontrakt()

            JSONAssert.assertEquals(
                objectMapper.writeValueAsString(forventet),
                restBody.utenVariableVerdier,
                STRICT
            )
        }
    }

    private suspend fun TestContext.hentPerson(body: String, forventetStatus: HttpStatusCode): String {
        val response = client.post("/api/person") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val responseBody = response.bodyAsText()
        assertEquals(forventetStatus, response.status, responseBody)
        return responseBody
    }

    private suspend fun TestContext.hentPersonViaGraphQL(): String {
        val query = URI("https://raw.githubusercontent.com/navikt/helse-spesialist/main/clients/spesialist-client-spleis/src/main/resources/graphql/hentSnapshot.graphql")
            .toURL()
            .readText()

        @Language("JSON")
        val requestBody = """
            {
                "query": "$query",
                "variables": {
                  "fnr": "$UNG_PERSON_FNR"
                },
                "operationName": "HentSnapshot"
            }
        """
        return client.post("/graphql") { setBody(requestBody) }
            .also { assertEquals(HttpStatusCode.OK, it.status) }
            .bodyAsText()
    }

    private companion object {
        private val UUIDRegex = "\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b".toRegex()
        private const val NullUUID = "00000000-0000-0000-0000-000000000000"
        private val LocalDateTimeRegex = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}".toRegex()
        private val LocalDateTimePrecisionRegex = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+".toRegex()
        private const val LocalDateTimeMandagsfrø = "2018-01-01T00:00:00"
        private val TidsstempelRegex = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}".toRegex()
        private const val TidsstempelMandagsfrø = "2018-01-01 00:00:00.000"
        private val FagsystemIdRegex = "[A-Z,2-7]{26}".toRegex()
        private const val FagsystemId = "ZZZZZZZZZZZZZZZZZZZZZZZZZZ"

        private val String.utenVariableVerdier
            get() = replace(UUIDRegex, NullUUID)
                .replace(LocalDateTimeRegex, LocalDateTimeMandagsfrø)
                .replace(LocalDateTimePrecisionRegex, LocalDateTimeMandagsfrø)
                .replace(TidsstempelRegex, TidsstempelMandagsfrø)
                .replace(FagsystemIdRegex, FagsystemId)

        /**
         * Felter GraphQL-varianten svarer med, men som REST-varianten bevisst utelater fordi
         * spesialist verken ber om dem eller har dem i klientmodellen sin. Nøkkelen er
         * "<klassenavn i GraphQL>.<felt>".
         */
        private val bevisstUtelatt = setOf(
            // spesialists `grunnlag`-fragment ber kun om skjonnsmessigFastsatt { belop, manedsbelop },
            // og GraphQLArbeidsgiverinntekt i klienten deres har ikke feltet i det hele tatt.
            // NB: SpleisVilkarsgrunnlag.skjonnsmessigFastsattAarlig er derimot i bruk.
            "inntekter.skjonnsmessigFastsattAarlig"
        )

        /**
         * Oversetter GraphQL-payloaden til det REST-APIet skal svare med: diskriminatoren `__typename`
         * heter `type` og har ikke `GraphQL`-prefiks. Hendelsene har allerede et `type`-felt som brukes
         * som diskriminator i REST-varianten, og der forsvinner `__typename` helt.
         */
        private fun JsonNode.somRestKontrakt(forelder: String = ""): JsonNode = when (this) {
            is ObjectNode -> {
                val typenavn = get("__typename")?.asText()
                objectMapper.createObjectNode().also { kopi ->
                    fields().forEach { (navn, verdi) ->
                        if (navn == "__typename") return@forEach
                        if ("$forelder.$navn" in bevisstUtelatt) return@forEach
                        kopi.set<JsonNode>(navn, verdi.somRestKontrakt(navn))
                    }
                    if (typenavn != null && !has("type")) kopi.put("type", typenavn.removePrefix("GraphQL"))
                }
            }

            is ArrayNode -> objectMapper.createArrayNode().also { kopi ->
                forEach { kopi.add(it.somRestKontrakt(forelder)) }
            }

            else -> this
        }
    }
}
