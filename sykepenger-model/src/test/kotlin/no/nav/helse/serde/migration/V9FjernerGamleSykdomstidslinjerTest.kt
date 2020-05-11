package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class V9FjernerGamleSykdomstidslinjerTest {
    private val objectMapper = jacksonObjectMapper()
    private val hendelsetidslinjeKey = "hendelseSykdomstidslinje"
    private val beregnetTidslinjeKey = "beregnetSykdomstidslinje"

    @Test
    fun `legger til nye sykdomstidslinjer`() {
        val json = objectMapper.readTree(personJson)
        listOf(V9FjernerGamleSykdomstidslinjer()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())

        migratedJson.path("arbeidsgivere")
            .first()
            .path("vedtaksperioder")
            .forEach { periode ->
                periode.path("sykdomshistorikk")
                    .forEach { element ->
                        assertNull(element[hendelsetidslinjeKey])
                        assertNull(element[beregnetTidslinjeKey])
                    }
            }
    }
}

@Language("JSON")
private const val personJson = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
            "sykdomshistorikk": [
                {
                    "tidsstempel": "2020-02-20T00:00:00.000000",
                    "hendelseId": "483c7972-87c9-4f18-8628-489b93da6d3f",
                    "hendelseSykdomstidslinje": [
                        {
                            "dagen": "2020-02-01",
                            "type": "ARBEIDSDAG_INNTEKTSMELDING"
                        }
                    ],
                    "beregnetSykdomstidslinje": [
                        {
                            "dagen": "2020-02-01",
                            "type": "ARBEIDSDAG_SØKNAD"
                        }
                    ],
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
