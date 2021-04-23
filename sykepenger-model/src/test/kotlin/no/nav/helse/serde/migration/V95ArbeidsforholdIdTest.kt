package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class V95ArbeidsforholdIdTest {

    @Test
    fun `Flytter inntektsmeldingId til Inntektsmelsing`() {
        val result = migrer(personFør)
        assertEquals(toNode(personEtter), result)

    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V95ArbeidsforholdId()).migrate(toNode(json))


    @Language("JSON")
    private val personFør = """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        { "inntektsmeldingId": "3042a85c-4d54-49a5-82f6-4b3118c89fc0" },
        { "inntektsmeldingId": null }
      ],
      "forkastede": [
        { "vedtaksperiode": { "inntektsmeldingId": "8c9688bf-7392-467c-acef-1a62cb9c4f52" } },
        { "vedtaksperiode": { "inntektsmeldingId": null } }
      ]
    }
  ],
  "skjemaVersjon": 94
}
"""

    @Language("JSON")
    private val personEtter = """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "inntektsmeldingInfo": {
            "id": "3042a85c-4d54-49a5-82f6-4b3118c89fc0",
            "arbeidsforholdId": null
          }
        },
        {
          "inntektsmeldingInfo": null
        }
      ],
      "forkastede": [
        {
          "vedtaksperiode": {
            "inntektsmeldingInfo": {
              "id": "8c9688bf-7392-467c-acef-1a62cb9c4f52",
              "arbeidsforholdId": null
            }
          }
        },
        {
          "vedtaksperiode": {
            "inntektsmeldingInfo": null
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 95
}
"""
}
