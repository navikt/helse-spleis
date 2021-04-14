package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class V90AvvistDagerBegrunnelserTest {
    @Test
    fun `Flytter enkel begrunnelse til liste av begrunnelser for alle avviste dager`() {
        val result = migrer(enVedtaksperiodeMedAvvisteDager)
        Assertions.assertEquals(toNode(expectedEnVedtaksperiodeListeAvBegrunnelser), result)
    }

    @Test
    fun `Flytter enkel begrunnelse til liste av begrunnelser for alle avviste dager i forkastede perioder`() {
        val result = migrer(enForkastetVedtaksperiodeMedAvvisteDager)
        Assertions.assertEquals(toNode(expectedEnForkastetVedtaksperiodeListeAvBegrunnelser), result)
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V90AvvistDagerBegrunnelser())
        .migrate(toNode(json))

    @Language("JSON")
    private val enVedtaksperiodeMedAvvisteDager = """
    {
      "arbeidsgivere": [
        {
          "organisasjonsnummer": "987654321",
          "id": "66c896ba-047c-4cf3-9714-cf7773ef4703",
          "vedtaksperioder": [
            {
              "fom": "2018-01-01",
              "tom": "2018-01-31",
              "skjæringstidspunkt": "2018-01-01",
              "forlengelseFraInfotrygd": "NEI",
              "utbetalingstidslinje": {
                "dager": [
                {
                    "type": "ArbeidsgiverperiodeDag",
                    "dato": "2018-01-01",
                    "grad": 0.0,
                    "arbeidsgiverBetalingProsent": 100.0,
                    "skjæringstidspunkt": "2018-01-01",
                    "totalGrad": 0.0,
                    "dekningsgrunnlag": 92.3076923076923,
                    "aktuellDagsinntekt": 92.3076923076923,
                    "arbeidsgiverbeløp": 0,
                    "personbeløp": 0,
                    "er6GBegrenset": false
                  },
                  {
                    "type": "AvvistDag",
                    "dato": "2018-01-02",
                    "begrunnelse": "MinimumInntekt",
                    "skjæringstidspunkt": "2018-01-01"
                  },
                  {
                    "type": "AvvistDag",
                    "dato": "2018-01-03",
                    "begrunnelse": "MinimumInntekt",
                    "skjæringstidspunkt": "2018-01-01"
                  }
                ]
               }
            }
          ],
          "forkastede": []
        }
      ],
      "skjemaVersjon": 89
    }
    """.trimIndent()

    @Language("JSON")
    private val expectedEnVedtaksperiodeListeAvBegrunnelser = """
    {
      "arbeidsgivere": [
        {
          "organisasjonsnummer": "987654321",
          "id": "66c896ba-047c-4cf3-9714-cf7773ef4703",
          "vedtaksperioder": [
            {
              "fom": "2018-01-01",
              "tom": "2018-01-31",
              "skjæringstidspunkt": "2018-01-01",
              "forlengelseFraInfotrygd": "NEI",
              "utbetalingstidslinje": {
                "dager": [
                {
                    "type": "ArbeidsgiverperiodeDag",
                    "dato": "2018-01-01",
                    "grad": 0.0,
                    "arbeidsgiverBetalingProsent": 100.0,
                    "skjæringstidspunkt": "2018-01-01",
                    "totalGrad": 0.0,
                    "dekningsgrunnlag": 92.3076923076923,
                    "aktuellDagsinntekt": 92.3076923076923,
                    "arbeidsgiverbeløp": 0,
                    "personbeløp": 0,
                    "er6GBegrenset": false
                  },
                  {
                    "type": "AvvistDag",
                    "dato": "2018-01-02",
                    "begrunnelser": ["MinimumInntekt"],
                    "skjæringstidspunkt": "2018-01-01"
                  },
                  {
                    "type": "AvvistDag",
                    "dato": "2018-01-03",
                    "begrunnelser": ["MinimumInntekt"],
                    "skjæringstidspunkt": "2018-01-01"
                  }
                ]
               }
            }
          ],
          "forkastede": []
        }
      ],
      "skjemaVersjon": 90
    }
    """.trimIndent()

    @Language("JSON")
    private val enForkastetVedtaksperiodeMedAvvisteDager = """
    {
      "arbeidsgivere": [
        {
          "organisasjonsnummer": "987654321",
          "id": "66c896ba-047c-4cf3-9714-cf7773ef4703",
          "forkastede": [
            {
            "vedtaksperiode": {
              "fom": "2018-01-01",
              "tom": "2018-01-31",
              "skjæringstidspunkt": "2018-01-01",
              "forlengelseFraInfotrygd": "NEI",
              "utbetalingstidslinje": {
                "dager": [
                {
                    "type": "ArbeidsgiverperiodeDag",
                    "dato": "2018-01-01",
                    "grad": 0.0,
                    "arbeidsgiverBetalingProsent": 100.0,
                    "skjæringstidspunkt": "2018-01-01",
                    "totalGrad": 0.0,
                    "dekningsgrunnlag": 92.3076923076923,
                    "aktuellDagsinntekt": 92.3076923076923,
                    "arbeidsgiverbeløp": 0,
                    "personbeløp": 0,
                    "er6GBegrenset": false
                  },
                  {
                    "type": "AvvistDag",
                    "dato": "2018-01-02",
                    "begrunnelse": "MinimumInntekt",
                    "skjæringstidspunkt": "2018-01-01"
                  },
                  {
                    "type": "AvvistDag",
                    "dato": "2018-01-03",
                    "begrunnelse": "MinimumInntekt",
                    "skjæringstidspunkt": "2018-01-01"
                  }
                ]
               }
            }
            }
          ],
          "vedtaksperioder": []
        }
      ],
      "skjemaVersjon": 89
    }
    """.trimIndent()

    @Language("JSON")
    private val expectedEnForkastetVedtaksperiodeListeAvBegrunnelser = """
    {
      "arbeidsgivere": [
        {
          "organisasjonsnummer": "987654321",
          "id": "66c896ba-047c-4cf3-9714-cf7773ef4703",
          "forkastede": [
            {
            "vedtaksperiode": {
              "fom": "2018-01-01",
              "tom": "2018-01-31",
              "skjæringstidspunkt": "2018-01-01",
              "forlengelseFraInfotrygd": "NEI",
              "utbetalingstidslinje": {
                "dager": [
                {
                    "type": "ArbeidsgiverperiodeDag",
                    "dato": "2018-01-01",
                    "grad": 0.0,
                    "arbeidsgiverBetalingProsent": 100.0,
                    "skjæringstidspunkt": "2018-01-01",
                    "totalGrad": 0.0,
                    "dekningsgrunnlag": 92.3076923076923,
                    "aktuellDagsinntekt": 92.3076923076923,
                    "arbeidsgiverbeløp": 0,
                    "personbeløp": 0,
                    "er6GBegrenset": false
                  },
                  {
                    "type": "AvvistDag",
                    "dato": "2018-01-02",
                    "begrunnelser": ["MinimumInntekt"],
                    "skjæringstidspunkt": "2018-01-01"
                  },
                  {
                    "type": "AvvistDag",
                    "dato": "2018-01-03",
                    "begrunnelser": ["MinimumInntekt"],
                    "skjæringstidspunkt": "2018-01-01"
                  }
                ]
               }
            }
            }
          ],
          "vedtaksperioder": []
        }
      ],
      "skjemaVersjon": 90
    }
    """.trimIndent()

}
