package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V81ForkastetUtbetalingerTest {
    @Test
    fun `migrerer utbetalingstatus`() {
        val migrated = listOf(V81ForkastetUtbetalinger())
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
          "status": "IKKE_UTBETALT",
          "tidsstempel": "2021-01-31T09:40:55.582101"
        },
        {
          "status": "GODKJENT",
          "tidsstempel": "2021-01-19T09:40:58.582101"
        },
        {
          "status": "IKKE_UTBETALT",
          "tidsstempel": "2020-12-05T14:29:49.124142"
        }
      ]
    }
  ],
  "skjemaVersjon": 80
}
"""
@Language("JSON")
private fun expectedJson() =
    """{
  "arbeidsgivere": [
    {
      "utbetalinger": [
        {
          "status": "IKKE_UTBETALT",
          "tidsstempel": "2021-01-31T09:40:55.582101"
        },
        {
          "status": "GODKJENT",
          "tidsstempel": "2021-01-19T09:40:58.582101"
        },
        {
          "status": "FORKASTET",
          "tidsstempel": "2020-12-05T14:29:49.124142"
        }
      ]
    }
  ],
  "skjemaVersjon": 81
}
"""
