package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class V80InntektskildePåVedtaksperiodeTest {
    @Test
    fun `legger på inntektskilde på vedtaksperiode`() {
        val result = listOf(V80InntektskildePåVedtaksperiode())
            .migrate(serdeObjectMapper.readTree(before))

        assertEquals(serdeObjectMapper.readTree(after), result)
    }
}

@Language("JSON")
private val before = """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {},
        {}
      ],
      "forkastede": [
        {
          "vedtaksperiode": {}
        },
        {
          "vedtaksperiode": {}
        }
      ]
    }
  ],
  "skjemaVersjon": 79
}"""

@Language("JSON")
private val after = """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "inntektskilde": "EN_ARBEIDSGIVER"
        },
        {
          "inntektskilde": "EN_ARBEIDSGIVER"
        }
      ],
      "forkastede": [
        {
          "vedtaksperiode": {
            "inntektskilde": "EN_ARBEIDSGIVER"
          }
        },
        {
          "vedtaksperiode": {
            "inntektskilde": "EN_ARBEIDSGIVER"
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 80
}"""
