package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V78VedtaksperiodeListeOverUtbetalingerTest {
    @Test
    fun `migrerer vedtaksperiode med liste over utbetalinger`() {
        val migrated = listOf(V78VedtaksperiodeListeOverUtbetalinger())
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
          "vedtaksperioder": [
              {
                "id": "2555046a-fad9-4c4f-9aba-de1721b2b497",
                "utbetalingId": "c11cdfbc-dcc4-4f29-9ef9-e0db7b197487"
              },
              {
                "id": "ef991e12-b97c-4cf9-9f34-542dba36c582"
              }
            ],
            "forkastede": [
              {
                "vedtaksperiode": {
                  "id": "7c253913-6b90-4435-af88-db2ee1909e6f",
                  "utbetalingId": "9b842ebe-699c-4a57-9265-453ca1ace142"
                }
              },
              {
                "vedtaksperiode": {
                  "id": "be9faa2a-d11e-4d63-926c-bc217692206f"
                }
              }
            ]
        }
    ],
    "skjemaVersjon": 77
}
"""
@Language("JSON")
private fun expectedJson() =
    """{
  "arbeidsgivere": [
        {
          "vedtaksperioder": [
              {
                "id": "2555046a-fad9-4c4f-9aba-de1721b2b497",
                "utbetalingId": "c11cdfbc-dcc4-4f29-9ef9-e0db7b197487",
                "utbetalinger": ["c11cdfbc-dcc4-4f29-9ef9-e0db7b197487"]
              },
              {
                "id": "ef991e12-b97c-4cf9-9f34-542dba36c582",
                "utbetalinger": []
              }
            ],
            "forkastede": [
              {
                "vedtaksperiode": {
                  "id": "7c253913-6b90-4435-af88-db2ee1909e6f",
                  "utbetalingId": "9b842ebe-699c-4a57-9265-453ca1ace142",
                  "utbetalinger": ["9b842ebe-699c-4a57-9265-453ca1ace142"]
                }
              },
              {
                "vedtaksperiode": {
                  "id": "be9faa2a-d11e-4d63-926c-bc217692206f",
                  "utbetalinger": []
                }
              }
            ]
        }
    ],
    "skjemaVersjon": 78
}
"""
