package no.nav.helse.spleis.rettighetskategori

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

class RettighetskategoriApiTest : AbstractApiTest() {
    private val FNR = nyttFødselsnummer()

    @Test
    fun `hent rettighetskategorier for arbeidstaker med vilkårsgrunnlag fra Infotrygd og Spleis`() =
        blackboxTestApplication({ it.ds.lagrePerson(FNR, arbeidstakerMedInfotrygdforlengelse()) }) {
            "/api/rettighetskategorier".httpPost(HttpStatusCode.OK, mapOf("fødselsnummer" to FNR)) {
                @Language("JSON")
                val expected = """
            {
              "rettighetskategorier": [
                {
                  "rettighetskategoriId": "22fa796c-aa49-351a-9d95-6be904d95777",
                  "skjæringstidspunkt": "2018-01-01",
                  "opprettet": "2022-08-31T09:52:08.494020",
                  "kilde": "INFOTRYGD",
                  "harSelvstendigInntekt": false,
                  "yrkesaktivitetstyper": {
                    "aktive": ["ARBEIDSTAKER"],
                    "forkastede": []
                  }
                },
                {
                  "rettighetskategoriId": "<rettighetskategoriId>",
                  "skjæringstidspunkt": "2018-04-01",
                  "opprettet": "<opprettet>",
                  "kilde": "SPLEIS",
                  "harSelvstendigInntekt": false,
                  "yrkesaktivitetstyper": {
                    "aktive": ["ARBEIDSTAKER"],
                    "forkastede": []
                  }
                }
              ]
            }
            """
                JSONAssert.assertEquals(expected, nullifiserSpleisfelter(this), true)
            }
        }

    @Test
    fun `hent rettighetskategorier for selvstendig`() =
        blackboxTestApplication({ it.ds.lagrePerson(FNR, selvstendig(FNR)) }) {
            "/api/rettighetskategorier".httpPost(HttpStatusCode.OK, mapOf("fødselsnummer" to FNR)) {
                @Language("JSON")
                val expected = """
            {
              "rettighetskategorier": [
                {
                  "rettighetskategoriId": "<rettighetskategoriId>",
                  "skjæringstidspunkt": "2018-04-01",
                  "opprettet": "<opprettet>",
                  "kilde": "SPLEIS",
                  "harSelvstendigInntekt": true,
                  "yrkesaktivitetstyper": {
                    "aktive": ["SELVSTENDIG"],
                    "forkastede": []
                  }
                }
              ]
            }
            """
                JSONAssert.assertEquals(expected, nullifiserSpleisfelter(this), true)
            }
        }

    @Test
    fun `hent rettighetskategorier for person som ikke finnes`() =
        blackboxTestApplication({}) {
            "/api/rettighetskategorier".httpPost(HttpStatusCode.OK, mapOf("fødselsnummer" to "11111111111")) {
                JSONAssert.assertEquals("""{ "rettighetskategorier": [] }""", this, true)
            }
        }

    private fun nullifiserSpleisfelter(response: String) =
        objectMapper
            .readTree(response)
            .apply {
                path("rettighetskategorier")
                    .filter { it.path("kilde").asText() == "SPLEIS" }
                    .forEach { rettighetskategori ->
                        rettighetskategori as ObjectNode
                        assertDoesNotThrow { UUID.fromString(rettighetskategori.path("rettighetskategoriId").asText()) }
                        rettighetskategori.put("rettighetskategoriId", "<rettighetskategoriId>")
                        assertDoesNotThrow { LocalDateTime.parse(rettighetskategori.path("opprettet").asText()) }
                        rettighetskategori.put("opprettet", "<opprettet>")
                    }
            }.toString()
}
