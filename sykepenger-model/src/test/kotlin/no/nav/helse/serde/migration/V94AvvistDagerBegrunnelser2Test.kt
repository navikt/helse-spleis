package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class V94AvvistDagerBegrunnelser2Test {

    @Test
    fun `Flytter enkel begrunnelse til liste av begrunnelser for alle avviste dager i beregnetUtbetalingstidslinjer`() {
        val result = migrer(beregnetUtbetalingstidslinjeMedAvvisteDager)
        Assertions.assertEquals(toNode(expectedBeregnetUtbetalingstidslinjeMedAvvisteDager), result)
    }

    @Test
    fun `Flytter enkel begrunnelse til liste av begrunnelser for alle avviste dager i utbetalinger`() {
        val result = migrer(utbetalingerMedAvvisteDager)
        Assertions.assertEquals(toNode(expectedUtbetalingerMedAvvisteDager), result)
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V94AvvistDagerBegrunnelser2()).migrate(toNode(json))


    @Language("JSON")
    private val beregnetUtbetalingstidslinjeMedAvvisteDager = """
    {
      "arbeidsgivere": [
        {
          "organisasjonsnummer": "987654321",
          "id": "66c896ba-047c-4cf3-9714-cf7773ef4703",
          "beregnetUtbetalingstidslinjer": [
            {
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
          "vedtaksperioder": [],
          "forkastede": []
        }
      ],
      "skjemaVersjon": 93
    }
    """

    @Language("JSON")
    private val expectedBeregnetUtbetalingstidslinjeMedAvvisteDager = """
    {
      "arbeidsgivere": [
        {
          "organisasjonsnummer": "987654321",
          "id": "66c896ba-047c-4cf3-9714-cf7773ef4703",
          "beregnetUtbetalingstidslinjer": [
            {
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
          "vedtaksperioder": [],
          "forkastede": []
        }
      ],
      "skjemaVersjon": 94
    }
    """

    @Language("JSON")
    private val utbetalingerMedAvvisteDager = """
    {
      "arbeidsgivere": [
        {
          "organisasjonsnummer": "987654321",
          "id": "66c896ba-047c-4cf3-9714-cf7773ef4703",
          "utbetalinger": [
            {
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
          "vedtaksperioder": [],
          "forkastede": []
        }
      ],
      "skjemaVersjon": 93
    }
    """

    @Language("JSON")
    private val expectedUtbetalingerMedAvvisteDager = """
    {
      "arbeidsgivere": [
        {
          "organisasjonsnummer": "987654321",
          "id": "66c896ba-047c-4cf3-9714-cf7773ef4703",
          "utbetalinger": [
            {
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
          "vedtaksperioder": [],
          "forkastede": []
        }
      ],
      "skjemaVersjon": 94
    }
    """

}
