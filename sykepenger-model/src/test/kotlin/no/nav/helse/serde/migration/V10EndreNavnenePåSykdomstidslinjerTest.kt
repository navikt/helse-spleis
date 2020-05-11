package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V10EndreNavnPåSykdomstidslinjerTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `legger til nye sykdomstidslinjer`() {
        val json = objectMapper.readTree(oldJson)
        listOf(V10EndreNavnPåSykdomstidslinjer()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        assertEquals(objectMapper.readTree(expectedJson), migratedJson)
    }
}

@Language("JSON")
private const val oldJson = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
            "sykdomshistorikk": [
                {
                    "tidsstempel": "2020-02-20T00:00:00.000000",
                    "hendelseId": "483c7972-87c9-4f18-8628-489b93da6d3f",
                    "nyHendelseSykdomstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-02-01",
                                "type": "ARBEIDSDAG",
                                "kilde": {
                                  "type": "Søknad"
                                }
                            }
                    ]
                    },
                    "nyBeregnetSykdomstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-02-01",
                                "type": "ARBEIDSDAG",
                                "kilde": {
                                  "type": "Søknad"
                                }
                            }
                      ]
                  }
                }
            ]
        }
      ]
    }
  ]
}
"""

@Language("JSON")
private const val expectedJson = """
{
  "skjemaVersjon": 10,
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
            "sykdomshistorikk": [
                {
                    "tidsstempel": "2020-02-20T00:00:00.000000",
                    "hendelseId": "483c7972-87c9-4f18-8628-489b93da6d3f",
                    "hendelseSykdomstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-02-01",
                                "type": "ARBEIDSDAG",
                                "kilde": {
                                  "type": "Søknad"
                                }
                            }
                    ]
                    },
                    "beregnetSykdomstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-02-01",
                                "type": "ARBEIDSDAG",
                                "kilde": {
                                  "type": "Søknad"
                                }
                            }
                      ]
                  }
                }
            ]
        }
      ]
    }
  ]
}
"""
