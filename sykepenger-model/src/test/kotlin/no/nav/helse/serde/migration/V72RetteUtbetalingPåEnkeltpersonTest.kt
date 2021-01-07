package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V72RetteUtbetalingPåEnkeltpersonTest {

    @Test
    fun `legger på opprettet-tidspunkt`() {
        val migrated = listOf(V72RetteUtbetalingPåEnkeltperson())
            .migrate(serdeObjectMapper.readTree(originalJson()))
        val expected = serdeObjectMapper.readTree(expectedJson())

        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private fun originalJson() =
    """{
  "arbeidsgivere": [
    {
      "utbetalinger": [
        {
          "id": "5d1dc0b9-0e2e-4c6a-a7c2-f50cae9a54a4",
          "status": "GODKJENT_UTEN_UTBETALING",
          "type": "ANNULLERING"
        },
        {
          "id": "231b36a9-0e2e-4c6a-a7c2-353f96c23552",
          "status": "UTBETALT",
          "type": "ANNULLERING"
        },
        {
          "id": "a62701f1-e8cc-4a8e-b000-35589020715b",
          "status": "IKKE_GODKJENT",
          "type": "UTBETALING"
        }
      ]
    }
  ],
  "skjemaVersjon": 71
}
"""

@Language("JSON")
private fun expectedJson() =
    """{
  "arbeidsgivere": [
    {
      "utbetalinger": [
        {
          "id": "5d1dc0b9-0e2e-4c6a-a7c2-f50cae9a54a4",
          "status": "GODKJENT_UTEN_UTBETALING",
          "type": "ANNULLERING"
        },
        {
          "id": "231b36a9-0e2e-4c6a-a7c2-353f96c23552",
          "status": "UTBETALT",
          "type": "UTBETALING"
        },
        {
          "id": "a62701f1-e8cc-4a8e-b000-35589020715b",
          "status": "IKKE_GODKJENT",
          "type": "UTBETALING"
        }
      ]
    }
  ],
  "skjemaVersjon": 72
}
"""
