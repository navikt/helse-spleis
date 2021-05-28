package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V100SlettEnkeltFeriepengeutbetalingTest {
    @Test
    fun `sletter kun feriepengerutbetaling med riktig id`() {
        assertEquals(toNode(expected), migrer(original))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V100SlettEnkeltFeriepengeutbetaling()).migrate(toNode(json))
}

@Language("JSON")
private val original = """{
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "utbetalingId": "532c9b3b-8987-4065-831b-cfe6cb61a8f4",
          "opptjeningsår": "2018"
        },
        {
          "utbetalingId": "66d6a594-5b5b-41c4-be0c-1841506b594b",
          "opptjeningsår": "2019"
        }
      ]
    }
  ],
  "skjemaVersjon": 99
}
"""

@Language("JSON")
private val expected = """{
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "utbetalingId": "532c9b3b-8987-4065-831b-cfe6cb61a8f4",
          "opptjeningsår": "2018"
        }
      ]
    }
  ],
  "skjemaVersjon": 100
}
"""
