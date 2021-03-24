package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V86ForkastetPeriodeIFjernetStateTest {
    @Test
    fun `Flytter forkastede perioder i AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD til AVSLUTTET_UTEN_UTBETALING`() {
        assertEquals(migrer(person), toNode(expectedPerson))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V86ForkastetPeriodeIFjernetState())
        .migrate(toNode(json))

    @Language("JSON")
    private val person   = """
    {
      "arbeidsgivere": [
        {
          "vedtaksperioder": [
            {
              "tilstand": "AVSLUTTET"
            },
            {
              "tilstand": "AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING"
            },
            {
              "tilstand": "AVSLUTTET_UTEN_UTBETALING"
            }
          ],
          "forkastede": [
            {
                "vedtaksperiode": {
                  "tilstand": "AVSLUTTET"
                }
            },
            {
                "vedtaksperiode": {
                  "tilstand": "AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD"
                }
            },
            {
                "vedtaksperiode": {
                  "tilstand": "AVSLUTTET_UTEN_UTBETALING"
                }
            }
          ]
        }
      ],
      "skjemaVersjon": 85
    }
    """

    val expectedPerson = """
    {
      "arbeidsgivere": [
        {
          "vedtaksperioder": [
            {
              "tilstand": "AVSLUTTET"
            },
            {
              "tilstand": "AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING"
            },
            {
              "tilstand": "AVSLUTTET_UTEN_UTBETALING"
            }
          ],
          "forkastede": [
            {
                "vedtaksperiode": {
                  "tilstand": "AVSLUTTET"
                }
            },
            {
                "vedtaksperiode": {
                  "tilstand": "AVSLUTTET_UTEN_UTBETALING"
                }
            },
            {
                "vedtaksperiode": {
                  "tilstand": "AVSLUTTET_UTEN_UTBETALING"
                }
            }
          ]
        }
      ],
      "skjemaVersjon": 86
    }
    """
}
