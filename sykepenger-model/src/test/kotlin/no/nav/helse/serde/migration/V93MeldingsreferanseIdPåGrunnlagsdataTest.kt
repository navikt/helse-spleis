package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V93MeldingsreferanseIdPåGrunnlagsdataTest {

    @Test
    fun `grunnlagsdata får samme meldingsreferanseId som sammenligningsgrunnlaget for samme skjæringstidspunkt`() {
        val result = migrer(grunnlagsdataUtenMeldingsreferanseId)
        assertEquals(toNode(expectedGrunnlagsdataMedMeldingsreferanseId), result)
    }

    @Test
    fun `grunnlagsdata får meldingsreferanseId - arbeidsgiver uten inntektshistorikk ligger først i listen`() {
        val result = migrer(arbeidsgiverUtenInntektshistorikk)
        assertEquals(toNode(expectedArbeidsgiverUtenInntektshistorikk), result)
    }

    @Test
    fun `grunnlagsdata får meldingsreferanseId - arbeidsgiver uten inntektshistorikk ligger sist i listen`() {
        val result = migrer(arbeidsgiverUtenInntektshistorikkLiggerSist)
        assertEquals(toNode(expectedArbeidsgiverUtenInntektshistorikkLiggerSist), result)
    }

    @Test
    fun `grunnlagsdata får meldingsreferanseId lik null når det ikke finnes inntektshistorikk med SKATT_SAMMENLIGNINGSGRUNNLAG`() {
        val result = migrer(utenSkattSammenligningsgrunnlag)
        assertEquals(toNode(expectedUtenSkattSammenligningsgrunnlag), result)
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V93MeldingsreferanseIdPåGrunnlagsdata())
        .migrate(toNode(json))

    @Language("JSON")
    private val grunnlagsdataUtenMeldingsreferanseId = """
    {
      "arbeidsgivere": [
    {
      "inntektshistorikk": [
        {
          "inntektsopplysninger": [
            {
              "dato": "2018-01-01",
              "hendelseId": "ebacd9e7-2608-494c-ba58-2b12ed4d808f",
              "kilde": "INNTEKTSMELDING"
            },
            {
              "skatteopplysninger": [
                {
                  "dato": "2018-01-01",
                  "hendelseId": "0d2825d8-f5eb-45d3-9a84-15d42ad878e1",
                  "beløp": 416.67,
                  "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                  "tidsstempel": "2020-11-30T11:34:55.953759",
                  "måned": "2017-11",
                  "type": "LØNNSINNTEKT",
                  "fordel": "kontantytelse",
                  "beskrivelse": "fastTillegg"
                },
                {
                  "dato": "2018-01-01",
                  "hendelseId": "0d2825d8-f5eb-45d3-9a84-15d42ad878e1",
                  "beløp": 416.67,
                  "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                  "tidsstempel": "2020-11-30T11:34:55.953759",
                  "måned": "2017-12",
                  "type": "LØNNSINNTEKT",
                  "fordel": "kontantytelse",
                  "beskrivelse": "fastTillegg"
                }
              ]
            }
          ]
        }
      ]
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
      "skjemaVersjon": 92
    }
    """

    @Language("JSON")
    private val expectedGrunnlagsdataMedMeldingsreferanseId = """
    {
      "arbeidsgivere": [
    {
      "inntektshistorikk": [
        {
          "inntektsopplysninger": [
            {
              "dato": "2018-01-01",
              "hendelseId": "ebacd9e7-2608-494c-ba58-2b12ed4d808f",
              "kilde": "INNTEKTSMELDING"
            },
            {
              "skatteopplysninger": [
                {
                  "dato": "2018-01-01",
                  "hendelseId": "0d2825d8-f5eb-45d3-9a84-15d42ad878e1",
                  "beløp": 416.67,
                  "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                  "tidsstempel": "2020-11-30T11:34:55.953759",
                  "måned": "2017-11",
                  "type": "LØNNSINNTEKT",
                  "fordel": "kontantytelse",
                  "beskrivelse": "fastTillegg"
                },
                {
                  "dato": "2018-01-01",
                  "hendelseId": "0d2825d8-f5eb-45d3-9a84-15d42ad878e1",
                  "beløp": 416.67,
                  "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                  "tidsstempel": "2020-11-30T11:34:55.953759",
                  "måned": "2017-12",
                  "type": "LØNNSINNTEKT",
                  "fordel": "kontantytelse",
                  "beskrivelse": "fastTillegg"
                }
              ]
            }
          ]
        }
      ]
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
          "vurdertOk": true,
          "meldingsreferanseId": "0d2825d8-f5eb-45d3-9a84-15d42ad878e1"
        }
      ],
      "skjemaVersjon": 93
    }
    """

    @Language("JSON")
    private val arbeidsgiverUtenInntektshistorikk = """
    {
      "arbeidsgivere": [
        {
          "inntektshistorikk": []
        },
        {
          "inntektshistorikk": [
            {
              "inntektsopplysninger": [
                {
                  "dato": "2018-01-01",
                  "hendelseId": "ebacd9e7-2608-494c-ba58-2b12ed4d808f",
                  "kilde": "INNTEKTSMELDING"
                },
                {
                  "skatteopplysninger": [
                    {
                      "dato": "2018-01-01",
                      "hendelseId": "0d2825d8-f5eb-45d3-9a84-15d42ad878e1",
                      "beløp": 416.67,
                      "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                      "tidsstempel": "2020-11-30T11:34:55.953759",
                      "måned": "2017-11",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastTillegg"
                    },
                    {
                      "dato": "2018-01-01",
                      "hendelseId": "0d2825d8-f5eb-45d3-9a84-15d42ad878e1",
                      "beløp": 416.67,
                      "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                      "tidsstempel": "2020-11-30T11:34:55.953759",
                      "måned": "2017-12",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastTillegg"
                    }
                  ]
                }
              ]
            }
          ]
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
      "skjemaVersjon": 92
    }
    """

    @Language("JSON")
    private val expectedArbeidsgiverUtenInntektshistorikk = """
    {
      "arbeidsgivere": [
        {
          "inntektshistorikk": []
        },
        {
          "inntektshistorikk": [
            {
              "inntektsopplysninger": [
                {
                  "dato": "2018-01-01",
                  "hendelseId": "ebacd9e7-2608-494c-ba58-2b12ed4d808f",
                  "kilde": "INNTEKTSMELDING"
                },
                {
                  "skatteopplysninger": [
                    {
                      "dato": "2018-01-01",
                      "hendelseId": "0d2825d8-f5eb-45d3-9a84-15d42ad878e1",
                      "beløp": 416.67,
                      "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                      "tidsstempel": "2020-11-30T11:34:55.953759",
                      "måned": "2017-11",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastTillegg"
                    },
                    {
                      "dato": "2018-01-01",
                      "hendelseId": "0d2825d8-f5eb-45d3-9a84-15d42ad878e1",
                      "beløp": 416.67,
                      "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                      "tidsstempel": "2020-11-30T11:34:55.953759",
                      "måned": "2017-12",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastTillegg"
                    }
                  ]
                }
              ]
            }
          ]
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
          "vurdertOk": true,
          "meldingsreferanseId": "0d2825d8-f5eb-45d3-9a84-15d42ad878e1"
        }
      ],
      "skjemaVersjon": 93
    }
    """

    @Language("JSON")
    private val arbeidsgiverUtenInntektshistorikkLiggerSist = """
    {
      "arbeidsgivere": [
        {
          "inntektshistorikk": [
            {
              "inntektsopplysninger": [
                {
                  "dato": "2018-01-01",
                  "hendelseId": "ebacd9e7-2608-494c-ba58-2b12ed4d808f",
                  "kilde": "INNTEKTSMELDING"
                },
                {
                  "skatteopplysninger": [
                    {
                      "dato": "2018-01-01",
                      "hendelseId": "0d2825d8-f5eb-45d3-9a84-15d42ad878e1",
                      "beløp": 416.67,
                      "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                      "tidsstempel": "2020-11-30T11:34:55.953759",
                      "måned": "2017-11",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastTillegg"
                    },
                    {
                      "dato": "2018-01-01",
                      "hendelseId": "0d2825d8-f5eb-45d3-9a84-15d42ad878e1",
                      "beløp": 416.67,
                      "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                      "tidsstempel": "2020-11-30T11:34:55.953759",
                      "måned": "2017-12",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastTillegg"
                    }
                  ]
                }
              ]
            }
          ]
        },
        {
          "inntektshistorikk": []
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
      "skjemaVersjon": 92
    }
    """

    @Language("JSON")
    private val expectedArbeidsgiverUtenInntektshistorikkLiggerSist = """
    {
      "arbeidsgivere": [
        {
          "inntektshistorikk": [
            {
              "inntektsopplysninger": [
                {
                  "dato": "2018-01-01",
                  "hendelseId": "ebacd9e7-2608-494c-ba58-2b12ed4d808f",
                  "kilde": "INNTEKTSMELDING"
                },
                {
                  "skatteopplysninger": [
                    {
                      "dato": "2018-01-01",
                      "hendelseId": "0d2825d8-f5eb-45d3-9a84-15d42ad878e1",
                      "beløp": 416.67,
                      "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                      "tidsstempel": "2020-11-30T11:34:55.953759",
                      "måned": "2017-11",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastTillegg"
                    },
                    {
                      "dato": "2018-01-01",
                      "hendelseId": "0d2825d8-f5eb-45d3-9a84-15d42ad878e1",
                      "beløp": 416.67,
                      "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                      "tidsstempel": "2020-11-30T11:34:55.953759",
                      "måned": "2017-12",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastTillegg"
                    }
                  ]
                }
              ]
            }
          ]
        },
        {
          "inntektshistorikk": []
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
          "vurdertOk": true,
          "meldingsreferanseId": "0d2825d8-f5eb-45d3-9a84-15d42ad878e1"
        }
      ],
      "skjemaVersjon": 93
    }
    """

    @Language("JSON")
    private val utenSkattSammenligningsgrunnlag = """
    {
      "arbeidsgivere": [
    {
      "inntektshistorikk": [
        {
          "inntektsopplysninger": [
            {
              "dato": "2018-01-01",
              "hendelseId": "ebacd9e7-2608-494c-ba58-2b12ed4d808f",
              "kilde": "INNTEKTSMELDING"
            }
          ]
        }
      ]
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
      "skjemaVersjon": 92
    }
    """

    @Language("JSON")
    private val expectedUtenSkattSammenligningsgrunnlag = """
    {
      "arbeidsgivere": [
    {
      "inntektshistorikk": [
        {
          "inntektsopplysninger": [
            {
              "dato": "2018-01-01",
              "hendelseId": "ebacd9e7-2608-494c-ba58-2b12ed4d808f",
              "kilde": "INNTEKTSMELDING"
            }
          ]
        }
      ]
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
          "vurdertOk": true,
          "meldingsreferanseId": null
        }
      ],
      "skjemaVersjon": 93
    }
    """
}
