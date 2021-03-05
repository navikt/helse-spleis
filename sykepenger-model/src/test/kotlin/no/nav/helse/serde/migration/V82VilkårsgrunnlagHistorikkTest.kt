package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class V82VilkårsgrunnlagHistorikkTest {
    @Test
    fun `migrering av person med en vedtaksperiode`() {
        val result = migrer(enVedtaksperiode)
        assertEquals(toNode(expectedEnVedtaksperiode), result)
    }

    @Test
    fun `migrering av person med en forkastet vedtaksperiode`() {
        val result = migrer(enForkastet)
        assertEquals(toNode(expectedEnForkastetVedtaksperiode), result)
    }

    @Test
    fun `migrering av person med to vedtaksperioder som har samme skjæringstidspunkt`() {
        val result = migrer(toVedtaksperiodeMedEtSkjæringstidspunkt)
        assertEquals(toNode(exptectedToVedtaksperiodeMedDataForVilkårsvurderingEtSted), result)
    }

    @Test
    fun `migrering av person med to vedtaksperioder som har forskjellige skjæringstidspunkt`() {
        val result = migrer(toVedtaksperiodeMedToSkjæringstidspunkt)
        assertEquals(toNode(expectedToVedtaksperiodeToSkjæringstidspunkt), result)
    }

    @Test
    fun `migrering av person med to vedtaksperioder hvor kun en har dataForVilkårsprøving`() {
        val result = migrer(toVedtaksperiodeMedDataForVilkårsvurderingEtSted)
        assertEquals(toNode(expectedToVedtaksperiodeMedDataForVilkårsvurderingEtSted), result)
    }

    @Test
    fun `migrering av person med forkastet vedtaksperiode som er i TIL_INFOTRYGD`() {
        val result = migrer(forkastetPeriodeIkkeAvsluttet)
        assertEquals(toNode(expectedForkastetPeriodeIkkeAvsluttet), result)
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V82VilkårsgrunnlagHistorikk())
        .migrate(toNode(json))
}

@Language("JSON")
private val enVedtaksperiode = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "tilstand": "AVSLUTTET",
          "dataForVilkårsvurdering": {
            "beregnetÅrsinntektFraInntektskomponenten": 372000.0,
            "avviksprosent": 0.0,
            "antallOpptjeningsdagerErMinst": 365,
            "harOpptjening": true,
            "medlemskapstatus": "JA"
          }
        }
      ],
      "forkastede": []
    }
  ],
  "skjemaVersjon": 81
}
"""



@Language("JSON")
private val toVedtaksperiodeMedToSkjæringstidspunkt = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "tilstand": "AVSLUTTET",
          "dataForVilkårsvurdering": {
            "beregnetÅrsinntektFraInntektskomponenten": 372000.0,
            "avviksprosent": 0.0,
            "antallOpptjeningsdagerErMinst": 365,
            "harOpptjening": true,
            "medlemskapstatus": "JA"
          }
        },
        {
          "skjæringstidspunkt": "2018-03-01",
          "tilstand": "AVSLUTTET",
          "dataForVilkårsvurdering": {
            "beregnetÅrsinntektFraInntektskomponenten": 420000.0,
            "avviksprosent": 0.0,
            "antallOpptjeningsdagerErMinst": 365,
            "harOpptjening": true,
            "medlemskapstatus": "JA"
          }
        }
      ],
      "forkastede": []
    }
  ],
  "skjemaVersjon": 81
}
"""

@Language("JSON")
private val toVedtaksperiodeMedEtSkjæringstidspunkt = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "tilstand": "AVSLUTTET",
          "dataForVilkårsvurdering": {
            "beregnetÅrsinntektFraInntektskomponenten": 420000.0,
            "avviksprosent": 0.0,
            "antallOpptjeningsdagerErMinst": 365,
            "harOpptjening": true,
            "medlemskapstatus": "JA"
          }
        },
        {
          "skjæringstidspunkt": "2018-01-01",
          "tilstand": "AVSLUTTET",
          "dataForVilkårsvurdering": {
            "beregnetÅrsinntektFraInntektskomponenten": 372000.0,
            "avviksprosent": 0.0,
            "antallOpptjeningsdagerErMinst": 365,
            "harOpptjening": true,
            "medlemskapstatus": "JA"
          }
        }
      ],
      "forkastede": []
    }
  ],
  "skjemaVersjon": 81
}
"""

@Language("JSON")
private val toVedtaksperiodeMedDataForVilkårsvurderingEtSted = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "tilstand": "AVSLUTTET",
          "dataForVilkårsvurdering": {
            "beregnetÅrsinntektFraInntektskomponenten": 372000.0,
            "avviksprosent": 0.0,
            "antallOpptjeningsdagerErMinst": 365,
            "harOpptjening": true,
            "medlemskapstatus": "JA"
          }
        },
        {
          "skjæringstidspunkt": "2018-03-01",
          "tilstand": "AVVENTER_HISTORIKK",
          "dataForVilkårsvurdering": null
        }
      ],
      "forkastede": []
    }
  ],
  "skjemaVersjon": 81
}
"""

@Language("JSON")
private val enForkastet = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [],
      "forkastede": [
        {
          "vedtaksperiode": {
            "skjæringstidspunkt": "2018-01-01",
            "tilstand": "AVSLUTTET",
            "dataForVilkårsvurdering": {
              "beregnetÅrsinntektFraInntektskomponenten": 372000.0,
              "avviksprosent": 0.0,
              "antallOpptjeningsdagerErMinst": 365,
              "harOpptjening": true,
              "medlemskapstatus": "JA"
            }
          },
          "årsak": "IKKE_STØTTET"
        }
      ]
    }
  ],
  "skjemaVersjon": 81
}
"""

@Language("JSON")
private val forkastetPeriodeIkkeAvsluttet = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [],
      "forkastede": [
        {
          "vedtaksperiode": {
            "skjæringstidspunkt": "2018-01-01",
            "tilstand": "TIL_INFOTRYGD",
            "dataForVilkårsvurdering": {
              "beregnetÅrsinntektFraInntektskomponenten": 372000.0,
              "avviksprosent": 0.0,
              "antallOpptjeningsdagerErMinst": 365,
              "harOpptjening": true,
              "medlemskapstatus": "JA"
            }
          },
          "årsak": "IKKE_STØTTET"
        }
      ]
    }
  ],
  "skjemaVersjon": 81
}
"""

@Language("JSON")
private val expectedEnVedtaksperiode = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "tilstand": "AVSLUTTET"
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
  "skjemaVersjon": 82
}
"""

@Language("JSON")
private val expectedEnForkastetVedtaksperiode = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [],
      "forkastede": [
        {
          "vedtaksperiode": {
            "skjæringstidspunkt": "2018-01-01",
            "tilstand": "AVSLUTTET"
          },
          "årsak": "IKKE_STØTTET"
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
      "vurdertOk": true
    }
  ],
  "skjemaVersjon": 82
}
"""

@Language("JSON")
private val exptectedToVedtaksperiodeMedDataForVilkårsvurderingEtSted = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "tilstand": "AVSLUTTET"
        },
        {
          "skjæringstidspunkt": "2018-01-01",
          "tilstand": "AVSLUTTET"
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
  "skjemaVersjon": 82
}
"""

@Language("JSON")
private val expectedToVedtaksperiodeMedDataForVilkårsvurderingEtSted = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "tilstand": "AVSLUTTET"
        },
        {
          "skjæringstidspunkt": "2018-03-01",
          "tilstand": "AVVENTER_HISTORIKK"
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
  "skjemaVersjon": 82
}
"""

@Language("JSON")
private val expectedToVedtaksperiodeToSkjæringstidspunkt = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "tilstand": "AVSLUTTET"
        },
        {
          "skjæringstidspunkt": "2018-03-01",
          "tilstand": "AVSLUTTET"
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
    },

    {
      "skjæringstidspunkt": "2018-03-01",
      "type": "Vilkårsprøving",
      "antallOpptjeningsdagerErMinst": 365,
      "avviksprosent": 0.0,
      "sammenligningsgrunnlag": 420000.0,
      "harOpptjening": true,
      "medlemskapstatus": "JA",
      "vurdertOk": true
    }
  ],
  "skjemaVersjon": 82
}
"""

private val expectedForkastetPeriodeIkkeAvsluttet = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [],
      "forkastede": [
        {
          "vedtaksperiode": {
            "skjæringstidspunkt": "2018-01-01",
            "tilstand": "TIL_INFOTRYGD"
          },
          "årsak": "IKKE_STØTTET"
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
      "vurdertOk": false
    }
  ],
  "skjemaVersjon": 82
}
"""
