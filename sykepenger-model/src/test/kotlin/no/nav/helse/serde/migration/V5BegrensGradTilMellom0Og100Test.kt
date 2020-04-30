package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V5BegrensGradTilMellom0Og100Test {
    private val objectMapper = jacksonObjectMapper()
    private val hendelsetidslinjeKey = "hendelseSykdomstidslinje"
    private val beregnetTidslinjeKey = "beregnetSykdomstidslinje"

    @Test
    fun `endre grad -400 til 0`() {
        val json = objectMapper.readTree(personJson)
        listOf(V1EndreKunArbeidsgiverSykedagEnum(), V4LeggTilNySykdomstidslinje(), V5BegrensGradTilMellom0Og100()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())

        migratedJson.path("arbeidsgivere")
            .first()
            .path("vedtaksperioder")
            .forEach { periode ->
                periode.path("sykdomshistorikk")
                    .forEach { element ->
                        assertEquals(0.0, element[hendelsetidslinjeKey].first()["grad"].asDouble())
                        assertEquals(0.0, element[beregnetTidslinjeKey].first()["grad"].asDouble())
                    }
            }
    }

    @Test
    fun `endre grad 300 til 100`() {
        val json = objectMapper.readTree(personJson)
        listOf(V1EndreKunArbeidsgiverSykedagEnum(), V4LeggTilNySykdomstidslinje(), V5BegrensGradTilMellom0Og100()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())

        migratedJson.path("arbeidsgivere")
            .first()
            .path("vedtaksperioder")
            .forEach { periode ->
                periode.path("sykdomshistorikk")
                    .forEach { element ->
                        assertEquals(100.0, element[hendelsetidslinjeKey][1]["grad"].asDouble())
                    }
            }
    }

    @Test
    fun `grad mellom 0 og 100 endres ikke`() {
        val json = objectMapper.readTree(personJson)
        listOf(V1EndreKunArbeidsgiverSykedagEnum(), V4LeggTilNySykdomstidslinje(), V5BegrensGradTilMellom0Og100()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())

        migratedJson.path("arbeidsgivere")
            .first()
            .path("vedtaksperioder")
            .forEach { periode ->
                periode.path("sykdomshistorikk")
                    .forEach { element ->
                        assertEquals(80.0, element[hendelsetidslinjeKey][2]["grad"].asDouble())
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
                    "hendelseId": "uuid",
                    "hendelseSykdomstidslinje": [
                        {
                            "dagen": "2020-02-01",
                            "type": "FORELDET_SYKEDAG",
                            "grad": -400
                        },
                        {
                            "dagen": "2020-02-01",
                            "type": "FORELDET_SYKEDAG",
                            "grad": 300
                        },
                        {
                            "dagen": "2020-02-01",
                            "type": "SYKEDAG_SYKMELDING",
                            "grad": 80
                        }
                    ],
                    "beregnetSykdomstidslinje": [
                        {
                            "dagen": "2020-02-01",
                            "type": "FORELDET_SYKEDAG",
                            "grad": -400
                        }
                    ],
                    "nyHendelseSykdomstidslinje": [
                        {
                            "dagen": "2020-02-01",
                            "type": "FORELDET_SYKEDAG",
                            "grad": -400
                        }
                    ],
                    "nyBeregnetSykdomstidslinje": [
                        {
                            "dagen": "2020-02-01",
                            "type": "FORELDET_SYKEDAG",
                            "grad": -400
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
