package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class V84VilkårsvurderingMinimumInntektTest {
    @Test
    fun `Endrer vilkårsvurdering for skjæringstidspunkt til periode med avviste dager grunnet MinimumInntekt-filter`() {
        val result = migrer(enVedtaksperiodeUtenMinimumInntekt)
        Assertions.assertEquals(toNode(expectedEnVedtaksperiodeVurderingIkkeOk), result)
    }

    @Test
    fun `Legger til harMinimumInntekt = null på vilkårsvurdering der skjæringstidspunkt ikke har avviste dager grunnet minimum inntekt eller NavDager`() {
        val result = migrer(enVedtaksperiode)
        Assertions.assertEquals(toNode(enVedtaksperiodeExpected), result)
    }

    @Test
    fun `Legger til harMinimumInntekt = true på vilkårsvurdering der skjæringstidspunkt har NavDager`() {
        val result = migrer(enVedtaksperiodeMedNavdager)
        Assertions.assertEquals(toNode(enVedtaksperiodeMedNavdagerExpected), result)
    }

    @Test
    fun `Tomme utbetalingstidslinjer gir harMinimumInntekt = null`() {
        val result = migrer(vedtaksperiodeUtenUtbetaling)
        Assertions.assertEquals(toNode(expectedVedtaksperiodeUtenUtbetaling), result)
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V84VilkårsvurderingMinimumInntekt())
        .migrate(toNode(json))

    @Language("JSON")
    private val enVedtaksperiodeUtenMinimumInntekt = """
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
                    "begrunnelse": "MinimumInntekt"
                  }
                ]
               }
            }
          ],
          "forkastede": []
        }
      ],
      "vilkårsgrunnlagHistorikk": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "type": "Vilkårsprøving",
          "antallOpptjeningsdagerErMinst": 365,
          "avviksprosent": 0.0,
          "sammenligningsgrunnlag": 372000.0,
          "harOpptjening": true,
          "medlemskapstatus": "JA",
          "vurdertOk": true
        }
      ],
      "skjemaVersjon": 83
    }
    """.trimIndent()

    @Language("JSON")
    private val expectedEnVedtaksperiodeVurderingIkkeOk = """
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
                    "begrunnelse": "MinimumInntekt"
                  }
                ]
               }
            }
          ],
          "forkastede": []
        }
      ],
      "vilkårsgrunnlagHistorikk": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "type": "Vilkårsprøving",
          "antallOpptjeningsdagerErMinst": 365,
          "avviksprosent": 0.0,
          "sammenligningsgrunnlag": 372000.0,
          "harOpptjening": true,
          "medlemskapstatus": "JA",
          "harMinimumInntekt": false,
          "vurdertOk": false
        }
      ],
      "skjemaVersjon": 84
    }
    """.trimIndent()

    @Language("JSON")
    private val enVedtaksperiode = """
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
                    "begrunnelse": "SykepengedagerOppbrukt"
                  }
                ]
               }
            }
          ],
          "forkastede": []
        }
      ],
      "vilkårsgrunnlagHistorikk": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "type": "Vilkårsprøving",
          "antallOpptjeningsdagerErMinst": 365,
          "avviksprosent": 0.0,
          "sammenligningsgrunnlag": 372000.0,
          "harOpptjening": true,
          "medlemskapstatus": "JA",
          "vurdertOk": true
        }
      ],
      "skjemaVersjon": 83
    }"""

    @Language("JSON")
    private val enVedtaksperiodeExpected = """
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
                    "begrunnelse": "SykepengedagerOppbrukt"
                  }
                ]
               }
            }
          ],
          "forkastede": []
        }
      ],
      "vilkårsgrunnlagHistorikk": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "type": "Vilkårsprøving",
          "antallOpptjeningsdagerErMinst": 365,
          "avviksprosent": 0.0,
          "sammenligningsgrunnlag": 372000.0,
          "harOpptjening": true,
          "medlemskapstatus": "JA",
          "harMinimumInntekt": null,
          "vurdertOk": true
        }
      ],
      "skjemaVersjon": 84
    }
    """

@Language("JSON")
    private val enVedtaksperiodeMedNavdager = """
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
                    "type": "NavDag",
                    "dato": "2018-01-02",
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0,
                    "skjæringstidspunkt": "2018-01-01",
                    "totalGrad": 100.0,
                    "dekningsgrunnlag": 461.53846153846155,
                    "aktuellDagsinntekt": 461.53846153846155,
                    "arbeidsgiverbeløp": 462,
                    "personbeløp": 0,
                    "er6GBegrenset": false
                }
                ]
               }
            }
          ],
          "forkastede": []
        }
      ],
      "vilkårsgrunnlagHistorikk": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "type": "Vilkårsprøving",
          "antallOpptjeningsdagerErMinst": 365,
          "avviksprosent": 0.0,
          "sammenligningsgrunnlag": 372000.0,
          "harOpptjening": true,
          "medlemskapstatus": "JA",
          "vurdertOk": true
        }
      ],
      "skjemaVersjon": 83
    }"""

    @Language("JSON")
    private val enVedtaksperiodeMedNavdagerExpected = """
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
                    "type": "NavDag",
                    "dato": "2018-01-02",
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0,
                    "skjæringstidspunkt": "2018-01-01",
                    "totalGrad": 100.0,
                    "dekningsgrunnlag": 461.53846153846155,
                    "aktuellDagsinntekt": 461.53846153846155,
                    "arbeidsgiverbeløp": 462,
                    "personbeløp": 0,
                    "er6GBegrenset": false
                }
              ]
            }
            }
          ],
          "forkastede": []
        }
      ],
      "vilkårsgrunnlagHistorikk": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "type": "Vilkårsprøving",
          "antallOpptjeningsdagerErMinst": 365,
          "avviksprosent": 0.0,
          "sammenligningsgrunnlag": 372000.0,
          "harOpptjening": true,
          "medlemskapstatus": "JA",
          "harMinimumInntekt": true,
          "vurdertOk": true
        }
      ],
      "skjemaVersjon": 84
    }
    """


@Language("JSON")
    private val vedtaksperiodeUtenUtbetaling = """
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
              "utbetalingstidslinje": {
                "dager": []
               }
            }
          ],
          "forkastede": []
        }
      ],
      "vilkårsgrunnlagHistorikk": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "type": "Vilkårsprøving",
          "antallOpptjeningsdagerErMinst": 365,
          "avviksprosent": 0.0,
          "sammenligningsgrunnlag": 372000.0,
          "harOpptjening": true,
          "medlemskapstatus": "JA",
          "vurdertOk": true
        }
      ],
      "skjemaVersjon": 83
    }"""

    @Language("JSON")
    private val expectedVedtaksperiodeUtenUtbetaling = """
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
              "utbetalingstidslinje": {
                "dager": []
               }
            }
          ],
          "forkastede": []
        }
      ],
      "vilkårsgrunnlagHistorikk": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "type": "Vilkårsprøving",
          "antallOpptjeningsdagerErMinst": 365,
          "avviksprosent": 0.0,
          "sammenligningsgrunnlag": 372000.0,
          "harOpptjening": true,
          "medlemskapstatus": "JA",
          "harMinimumInntekt": null,
          "vurdertOk": true
        }
      ],
      "skjemaVersjon": 84
    }
    """


}
