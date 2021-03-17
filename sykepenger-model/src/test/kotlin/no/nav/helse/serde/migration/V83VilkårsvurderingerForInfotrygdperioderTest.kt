package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V83VilkårsvurderingerForInfotrygdperioderTest {
    @Test
    fun `Endrer ikke historikk for periode som ikke er infotrygdforlengelse`() {
        val result = migrer(enVedtaksperiode)
        assertEquals(toNode(expectedEnVedtaksperiode), result)
    }

    @Test
    fun `Legger til infotrygdhistorikk for periode som er infotrygdforlengelse`() {
        val result = migrer(enInfotrygdforlengelse)
        assertEquals(toNode(expectedEnInfotrygdforlengelse), result)
    }

    @Test
    fun `Legger til infotrygdhistorikk i historikken periode som er infotrygdforlengelse`() {
        val result = migrer(førsteGangsOgInfotrygdforlengelse)
        assertEquals(toNode(expectedførsteGangsOgInfotrygdforlengelse), result)
    }

    @Test
    fun `Erstatter historikken på samme skjæringstidspunkt med infotrygdVilkårsgrunnlag om den ikke er vurdertOk`() {
        val result = migrer(vurderingIkkeOk)
        assertEquals(toNode(expectedVurderingIkkeOk), result)
    }

    @Test
    fun `Sorterer og legger inn historikken`() {
        val result = migrer(sortertVilkårsgrunnlagHistorikk)
        assertEquals(toNode(expectedSortertVilkårsgrunnlagHistorikk), result)
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V83VilkårsvurderingerForInfotrygdperioder())
        .migrate(toNode(json))

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
              "forlengelseFraInfotrygd": "NEI"
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
          "type": "Infotrygd"
        }
      ],
      "skjemaVersjon": 82
    }
    """.trimIndent()

    @Language("JSON")
    private val expectedEnVedtaksperiode = """
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
              "forlengelseFraInfotrygd": "NEI"
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
    private val enInfotrygdforlengelse = """
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
              "forlengelseFraInfotrygd": "JA"
            }
          ],
          "forkastede": []
        }
      ],
      "vilkårsgrunnlagHistorikk": [],
      "skjemaVersjon": 82
    }
    """

    @Language("JSON")
    private val expectedEnInfotrygdforlengelse = """
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
              "forlengelseFraInfotrygd": "JA"
            }
          ],
          "forkastede": []
        }
      ],
      "vilkårsgrunnlagHistorikk": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "type": "Infotrygd"
        }
      ],
      "skjemaVersjon": 83
    }
    """

    @Language("JSON")
    private val førsteGangsOgInfotrygdforlengelse = """
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
              "forlengelseFraInfotrygd": "NEI"
            },
            {
              "fom": "2018-02-01",
              "tom": "2018-02-28",
              "skjæringstidspunkt": "2018-02-01",
              "forlengelseFraInfotrygd": "JA"
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
    private val expectedførsteGangsOgInfotrygdforlengelse = """
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
              "forlengelseFraInfotrygd": "NEI"
            },
            {
              "fom": "2018-02-01",
              "tom": "2018-02-28",
              "skjæringstidspunkt": "2018-02-01",
              "forlengelseFraInfotrygd": "JA"
            }
          ],
          "forkastede": []
        }
      ],
      "vilkårsgrunnlagHistorikk": [
        {
          "skjæringstidspunkt": "2018-02-01",
          "type": "Infotrygd"
        },
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
    """

    @Language("JSON")
    private val vurderingIkkeOk = """
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
              "forlengelseFraInfotrygd": "JA"
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
          "vurdertOk": false
        }
      ],
      "skjemaVersjon": 82
    }
    """

    @Language("JSON")
    private val expectedVurderingIkkeOk = """
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
              "forlengelseFraInfotrygd": "JA"
            }
          ],
          "forkastede": []
        }
      ],
      "vilkårsgrunnlagHistorikk": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "type": "Infotrygd"
        }
      ],
      "skjemaVersjon": 83
    }
    """

    @Language("JSON")
    private val sortertVilkårsgrunnlagHistorikk = """
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
              "forlengelseFraInfotrygd": "NEI"
            },
            {
              "fom": "2018-02-01",
              "tom": "2018-02-28",
              "skjæringstidspunkt": "2018-02-01",
              "forlengelseFraInfotrygd": "JA"
            },
            {
              "fom": "2018-03-01",
              "tom": "2018-03-31",
              "skjæringstidspunkt": "2018-03-01",
              "forlengelseFraInfotrygd": "JA"
            },
            {
              "fom": "2018-04-01",
              "tom": "2018-04-30",
              "skjæringstidspunkt": "2018-04-01",
              "forlengelseFraInfotrygd": "JA"
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
          "sammenligningsgrunnlag": 372000.0,
          "harOpptjening": true,
          "medlemskapstatus": "JA",
          "vurdertOk": false
        }
      ],
      "skjemaVersjon": 82
    }
    """

    @Language("JSON")
    private val expectedSortertVilkårsgrunnlagHistorikk = """
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
              "forlengelseFraInfotrygd": "NEI"
            },
            {
              "fom": "2018-02-01",
              "tom": "2018-02-28",
              "skjæringstidspunkt": "2018-02-01",
              "forlengelseFraInfotrygd": "JA"
            },
            {
              "fom": "2018-03-01",
              "tom": "2018-03-31",
              "skjæringstidspunkt": "2018-03-01",
              "forlengelseFraInfotrygd": "JA"
            },
            {
              "fom": "2018-04-01",
              "tom": "2018-04-30",
              "skjæringstidspunkt": "2018-04-01",
              "forlengelseFraInfotrygd": "JA"
            }
          ],
          "forkastede": []
        }
      ],
      "vilkårsgrunnlagHistorikk": [
        {
          "skjæringstidspunkt": "2018-04-01",
          "type": "Infotrygd"
        },
        {
          "skjæringstidspunkt": "2018-03-01",
          "type": "Infotrygd"
        },
        {
          "skjæringstidspunkt": "2018-02-01",
          "type": "Infotrygd"
        },
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
    """
}
