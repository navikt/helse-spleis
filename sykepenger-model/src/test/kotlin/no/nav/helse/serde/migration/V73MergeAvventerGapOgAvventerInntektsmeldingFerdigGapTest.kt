package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class V73MergeAvventerGapOgAvventerInntektsmeldingFerdigGapTest {
    @Test
    fun `migrerer perioder i AVVENTER_GAP`() {
        val result = listOf(V73MergeAvventerGapOgAvventerInntektsmeldingFerdigGap())
            .migrate(serdeObjectMapper.readTree(avventerGap))

        assertEquals(serdeObjectMapper.readTree(expected), result)
    }
    @Test
    fun `migrerer forkastede perioder i AVVENTER_GAP`() {
        val result = listOf(V73MergeAvventerGapOgAvventerInntektsmeldingFerdigGap())
            .migrate(serdeObjectMapper.readTree(avventerGapForkastet))

        assertEquals(serdeObjectMapper.readTree(expectedForkastet), result)
    }

    @Test
    fun `migrerer perioder i AVVENTER_INNTEKTSMELDING_FERDIG_GAP`() {
        val result = listOf(V73MergeAvventerGapOgAvventerInntektsmeldingFerdigGap())
            .migrate(serdeObjectMapper.readTree(avventerInntektsmeldingFerdigGap))

        assertEquals(serdeObjectMapper.readTree(expected), result)
    }

    @Test
    fun `migrerer forkastede perioder i AVVENTER_INNTEKTSMELDING_FERDIG_GAP`() {
        val result = listOf(V73MergeAvventerGapOgAvventerInntektsmeldingFerdigGap())
            .migrate(serdeObjectMapper.readTree(avventerInntektsmeldingFerdigGapForkastet))

        assertEquals(serdeObjectMapper.readTree(expectedForkastet), result)
    }

    @Language("JSON")
    val avventerGap = """
{
  "aktørId": "42",
  "fødselsnummer": "12020052345",
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "tilstand": "AVVENTER_GAP"
        }
      ],
      "forkastede": []
    }
  ],
  "skjemaVersjon": 71
}
    """

    @Language("JSON")
    val avventerInntektsmeldingFerdigGap = """
{
  "aktørId": "42",
  "fødselsnummer": "12020052345",
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "tilstand": "AVVENTER_INNTEKTSMELDING_FERDIG_GAP"
        }
      ],
      "forkastede": []
    }
  ],
  "skjemaVersjon": 71
}
"""

    @Language("JSON")
    val expected = """
{
  "aktørId": "42",
  "fødselsnummer": "12020052345",
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "tilstand": "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP"
        }
      ],
      "forkastede": []
    }
  ],
  "skjemaVersjon": 73
}
"""

    @Language("JSON")
    val avventerGapForkastet = """
{
  "aktørId": "42",
  "fødselsnummer": "12020052345",
  "arbeidsgivere": [
    {
      "vedtaksperioder": [],
      "forkastede": [
        {
          "vedtaksperiode": {
            "tilstand": "AVVENTER_GAP"
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 71
}
    """

    @Language("JSON")
    val avventerInntektsmeldingFerdigGapForkastet = """
{
  "aktørId": "42",
  "fødselsnummer": "12020052345",
  "arbeidsgivere": [
    {
      "vedtaksperioder": [],
      "forkastede": [
        {
          "vedtaksperiode": {
            "tilstand": "AVVENTER_INNTEKTSMELDING_FERDIG_GAP"
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 71
}
"""

    @Language("JSON")
    val expectedForkastet = """
{
  "aktørId": "42",
  "fødselsnummer": "12020052345",
  "arbeidsgivere": [
    {
      "vedtaksperioder": [],
      "forkastede": [
        {
          "vedtaksperiode": {
            "tilstand": "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP"
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 73
}
"""
}
