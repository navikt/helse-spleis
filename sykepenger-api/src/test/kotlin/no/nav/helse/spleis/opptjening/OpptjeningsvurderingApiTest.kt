package no.nav.helse.spleis.opptjening

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.http.HttpStatusCode
import no.nav.helse.nyttFødselsnummer
import no.nav.helse.spleis.AbstractApiTest
import no.nav.helse.spleis.objectMapper
import no.nav.helse.spleis.testhelpers.arbeidstakerMedInfotrygdforlengelse
import no.nav.helse.spleis.testhelpers.selvstendig
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.skyscreamer.jsonassert.JSONAssert
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

class OpptjeningsvurderingApiTest : AbstractApiTest() {
    // unikt per testmetode (ny instans per @Test) — deles av `sendt.person`/`person_alias`, som har `fnr` som unik kolonne i den delte databasen
    private val FNR = nyttFødselsnummer()

    @Test
    fun `hent opptjeningsvurderinger for person som finnes`() =
        blackboxTestApplication({ it.ds.lagrePerson(FNR, arbeidstakerMedInfotrygdforlengelse()) }) {
            "/api/opptjeningsvurderinger".httpPost(HttpStatusCode.OK, mapOf("fødselsnummer" to FNR)) {
                val actual = assertOgNullifiserSpleisOpptjeningsvurderingIder(this)

                @Language("JSON")
                val expected = """
            {
              "opptjeningsvurderinger": [
                {
                  "opptjeningsvurderingId": "b89e2ae5-59e3-388e-98cd-42a8e7350773",
                  "opprettet": "2022-08-31T09:52:08.494020",
                  "type": "ARBEIDSTAKER",
                  "skjæringstidspunkt": "2018-01-01",
                  "kilde": "INFOTRYGD"
                },
                {
                  "opptjeningsvurderingId": "00000000-0000-0000-0000-000000000000",
                  "opprettet": "<opprettet>",
                  "type": "ARBEIDSTAKER",
                  "skjæringstidspunkt": "2018-04-01",
                  "kilde": "SPLEIS",
                  "arbeidsforhold": [
                    {
                      "organisasjonsnummer": "987654322",
                      "ansettelsesperioder": [
                        {
                          "fom": "2017-04-01",
                          "tom": null
                        }
                      ]
                    }
                  ],
                  "opptjeningsperiode": {
                    "fom": "2017-04-01",
                    "tom": "2018-03-31"
                  },
                  "oppfylt": true,
                  "antallDager": 365
                }
              ]
            }
            """
                JSONAssert.assertEquals(expected, actual, true)
            }
        }

    @Test
    fun `hent opptjeningsvurderinger for person som ikke finnes`() =
        blackboxTestApplication({}) {
            "/api/opptjeningsvurderinger".httpPost(HttpStatusCode.OK, mapOf("fødselsnummer" to "11111111111")) {
                @Language("JSON")
                val expected = """
            {
              "opptjeningsvurderinger": []
            }
            """
                JSONAssert.assertEquals(expected, this, true)
            }
        }

    @Test
    fun `hent opptjeningsvurderinger for selvstendig som finnes`() =
        blackboxTestApplication({ it.ds.lagrePerson(FNR, selvstendig(FNR)) }) {
            "/api/opptjeningsvurderinger".httpPost(HttpStatusCode.OK, mapOf("fødselsnummer" to FNR)) {
                val actual = assertOgNullifiserSpleisOpptjeningsvurderingIder(this)

                @Language("JSON")
                val expected = """
            {
              "opptjeningsvurderinger": [
                {
                  "opptjeningsvurderingId": "00000000-0000-0000-0000-000000000000",
                  "opprettet": "<opprettet>",
                  "skjæringstidspunkt": "2018-04-01",
                  "kilde": "SPLEIS",
                  "type": "SELVSTENDIG"
                }
              ]
            }
            """
                JSONAssert.assertEquals(expected, actual, true)
            }
        }

    private fun assertOgNullifiserSpleisOpptjeningsvurderingIder(response: String): String {
        val spleisOpptjeningsvurderingIder = mutableListOf<UUID>()
        return objectMapper
            .readTree(response)
            .apply {
                val forventetAntall = path("opptjeningsvurderinger").count { it.path("kilde").asText() == "SPLEIS" }
                path("opptjeningsvurderinger").forEach { opptjeningsvurdering ->
                    if (opptjeningsvurdering.path("kilde").asText() == "INFOTRYGD") return@forEach
                    opptjeningsvurdering as ObjectNode
                    val opptjeningsvurderingId =
                        assertDoesNotThrow { UUID.fromString(opptjeningsvurdering.path("opptjeningsvurderingId").asText()) }
                    spleisOpptjeningsvurderingIder.add(opptjeningsvurderingId)
                    opptjeningsvurdering.put("opptjeningsvurderingId", "00000000-0000-0000-0000-000000000000")
                    assertDoesNotThrow { LocalDateTime.parse(opptjeningsvurdering.path("opprettet").asText()) }
                    opptjeningsvurdering.put("opprettet", "<opprettet>")
                }
                assertEquals(forventetAntall, spleisOpptjeningsvurderingIder.toSet().size)
            }.toString()
    }
}
