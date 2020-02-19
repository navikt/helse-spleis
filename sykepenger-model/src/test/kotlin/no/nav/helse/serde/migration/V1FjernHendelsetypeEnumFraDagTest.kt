package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class V1FjernHendelsetypeEnumFraDagTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    internal fun `oversetter til nye dagtyper`() {
        val json = objectMapper.readTree(personJson)
        listOf(V1FjernHendelsetypeEnumFraDag()).migrate(json)
        val migratedJson = json.toString()

        assertNotContains(migratedJson, "\"hendelseType\"")
        assertNotContains(migratedJson, "\"ARBEIDSDAG\"")
        assertNotContains(migratedJson, "\"EGENMELDINGSDAG\"")
        assertNotContains(migratedJson, "\"FERIEDAG\"")
        assertNotContains(migratedJson, "\"SYKEDAG\"")
        assertNotContains(migratedJson, "\"PERMISJONSDAG\"")
        assertContains(migratedJson, "\"ARBEIDSDAG_INNTEKTSMELDING\"")
        assertContains(migratedJson, "\"ARBEIDSDAG_SØKNAD\"")
        assertContains(migratedJson, "\"EGENMELDINGSDAG_INNTEKTSMELDING\"")
        assertContains(migratedJson, "\"EGENMELDINGSDAG_SØKNAD\"")
        assertContains(migratedJson, "\"FERIEDAG_INNTEKTSMELDING\"")
        assertContains(migratedJson, "\"FERIEDAG_SØKNAD\"")
        assertContains(migratedJson, "\"SYKEDAG_SYKMELDING\"")
        assertContains(migratedJson, "\"SYKEDAG_SØKNAD\"")
        assertContains(migratedJson, "\"PERMISJONSDAG_SØKNAD\"")
    }

    private fun assertNotContains(json: String, value: String) {
        assertFalse(json.contains(value)) { "Did not expect to find $value in $json" }
    }

    private fun assertContains(json: String, value: String) {
        assertTrue(json.contains(value)) { "Expected to find $value in $json" }
    }
}

private const val personJson = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "sykdomshistorikk": [
            {
              "tidsstempel": "2020-02-07T15:43:10.350016",
              "hendelseId": "483c7972-87c9-4f18-8628-489b93da6d3e",
              "hendelseSykdomstidslinje": [
                {
                  "dagen": "2018-01-03",
                  "hendelseType": "Inntektsmelding",
                  "type": "ARBEIDSDAG"
                },
                {
                  "dagen": "2018-01-04",
                  "hendelseType": "Inntektsmelding",
                  "type": "EGENMELDINGSDAG"
                },
                {
                  "dagen": "2018-01-05",
                  "hendelseType": "Inntektsmelding",
                  "type": "FERIEDAG"
                }
              ],
              "beregnetSykdomstidslinje": [
                {
                  "dagen": "2018-01-03",
                  "hendelseType": "Sykmelding",
                  "type": "SYKEDAG"
                },
                {
                  "dagen": "2018-01-04",
                  "hendelseType": "Søknad",
                  "type": "SYKEDAG"
                },
                {
                  "dagen": "2018-01-05",
                  "hendelseType": "Søknad",
                  "type": "ARBEIDSDAG"
                },
                {
                  "dagen": "2018-01-06",
                  "hendelseType": "Søknad",
                  "type": "SYK_HELGEDAG"
                },
                {
                  "dagen": "2018-01-07",
                  "hendelseType": "Søknad",
                  "type": "EGENMELDINGSDAG"
                },
                {
                  "dagen": "2018-01-08",
                  "hendelseType": "Søknad",
                  "type": "FERIEDAG"
                },
                {
                  "dagen": "2018-01-08",
                  "hendelseType": "Søknad",
                  "type": "PERMISJONSDAG"
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
"""
