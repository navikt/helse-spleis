package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class V96RetteOppFeilUtbetalingPekerTest {

    private val vedtaksperiodeId = "a6558358-f2fd-4d33-aa37-d85907fcf05e"
    private val utbetalingIdSomSkalErstattes = "a4b86e02-4621-42d8-a2a4-17c226e92d3a"
    private val utbetalingIdSomErstatter = "67dd0668-5dd6-417b-a149-8b9576ae0301"

    @Test
    fun `Erstatter utbetalingId`() {
        val result = migrer(personFør)
        assertEquals(toNode(personEtter), result)
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V96RetteOppFeilUtbetalingPeker()).migrate(toNode(json))


    @Language("JSON")
    private val personFør = """{
  "arbeidsgivere": [
    {
      "forkastede": [
        {
          "vedtaksperiode": {
            "id": "$vedtaksperiodeId",
            "utbetalinger": [
              "$utbetalingIdSomSkalErstattes"
            ]
          }
        }
      ],
      "utbetalinger": [
        {
          "id": "$utbetalingIdSomSkalErstattes"
        },
        {
          "id": "$utbetalingIdSomErstatter"
        }
      ]
    }
  ],
  "skjemaVersjon": 95
}
"""

    @Language("JSON")
    private val personEtter = """{
  "arbeidsgivere": [
    {
      "forkastede": [
        {
          "vedtaksperiode": {
            "id": "$vedtaksperiodeId",
            "utbetalinger": [
              "$utbetalingIdSomErstatter"
            ]
          }
        }
      ],
      "utbetalinger": [
        {
          "id": "$utbetalingIdSomSkalErstattes"
        },
        {
          "id": "$utbetalingIdSomErstatter"
        }
      ]
    }
  ],
  "skjemaVersjon": 96
}
"""
}
