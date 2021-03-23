package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V85FjernerAvsluttetUtenUtbetalingMedInntektsmeldingTest {
    @Test
    fun `Endrer ikke historikk for periode som ikke er infotrygdforlengelse`() {
        val result = migrer(enVedtaksperiode)
        assertEquals(toNode(expectedEnVedtaksperiode), result)
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V85FjernerAvsluttetUtenUtbetalingMedInntektsmelding())
        .migrate(toNode(json))

    @Language("JSON")
    private val enVedtaksperiode = """
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
                  "tilstand": "AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING"
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
      "skjemaVersjon": 84
    }
    """

    @Language("JSON")
    private val expectedEnVedtaksperiode = """
    {
      "arbeidsgivere": [
        {
          "vedtaksperioder": [
            {
              "tilstand": "AVSLUTTET"
            },
            {
              "tilstand": "AVSLUTTET_UTEN_UTBETALING"
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
      "skjemaVersjon": 85
    }
    """
}
