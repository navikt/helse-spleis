package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V77UtbetalingBeregningIdTest {
    @Test
    fun `migrerer utbetalingstidslinjeberegning`() {
        val migrated = listOf(V77UtbetalingBeregningId())
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
      "beregnetUtbetalingstidslinjer": [
        {
          "id": "36ac7821-5d6d-4a89-9e3d-9a1f0959566e",
          "tidsstempel": "2020-11-02T13:09:14.712441"
        },
        {
          "id": "cbafc668-654e-4d16-9f99-e5a95c31b2f1",
          "tidsstempel": "2020-12-09T14:29:49.124142"
        },
        {
          "id": "d547cc5f-8001-4675-90fa-12347bf737c5",
          "tidsstempel": "2020-12-22T13:16:47.682093"
        },
        {
          "id": "c68b81a2-7e1f-4dcf-babd-99f019ce0b81",
          "tidsstempel": "2021-01-19T09:40:55.424818"
        },
        {
          "id": "cea8bfe7-6847-4105-bb4e-0d2aebd81b35",
          "tidsstempel": "2021-02-01T09:40:58.424818"
        }
      ],
      "utbetalinger": [
        {
          "tidsstempel": "2021-01-31T09:40:55.582101"
        },
        {
          "tidsstempel": "2021-01-19T09:40:58.582101"
        },
        {
          "tidsstempel": "2020-12-05T14:29:49.124142"
        },
        {
          "tidsstempel": "2020-12-01T14:29:49.124142"
        },
        {
          "tidsstempel": "2020-11-02T13:09:00.712441"
        },
        {
          "tidsstempel": "2020-10-01T23:59:59.712441"
        }
      ]
    }
  ],
  "skjemaVersjon": 76
}
"""
@Language("JSON")
private fun expectedJson() =
    """{
  "arbeidsgivere": [
    {
      "beregnetUtbetalingstidslinjer": [
        {
          "id": "36ac7821-5d6d-4a89-9e3d-9a1f0959566e",
          "tidsstempel": "2020-11-02T13:09:14.712441"
        },
        {
          "id": "cbafc668-654e-4d16-9f99-e5a95c31b2f1",
          "tidsstempel": "2020-12-09T14:29:49.124142"
        },
        {
          "id": "d547cc5f-8001-4675-90fa-12347bf737c5",
          "tidsstempel": "2020-12-22T13:16:47.682093"
        },
        {
          "id": "c68b81a2-7e1f-4dcf-babd-99f019ce0b81",
          "tidsstempel": "2021-01-19T09:40:55.424818"
        },
        {
          "id": "cea8bfe7-6847-4105-bb4e-0d2aebd81b35",
          "tidsstempel": "2021-02-01T09:40:58.424818"
        }
      ],
      "utbetalinger": [
        {
          "tidsstempel": "2021-01-31T09:40:55.582101",
          "beregningId": "c68b81a2-7e1f-4dcf-babd-99f019ce0b81"
        },
        {
          "tidsstempel": "2021-01-19T09:40:58.582101",
          "beregningId": "c68b81a2-7e1f-4dcf-babd-99f019ce0b81"
        },
        {
          "tidsstempel": "2020-12-05T14:29:49.124142",
          "beregningId": "36ac7821-5d6d-4a89-9e3d-9a1f0959566e"
        },
        {
          "tidsstempel": "2020-12-01T14:29:49.124142",
          "beregningId": "36ac7821-5d6d-4a89-9e3d-9a1f0959566e"
        },
        {
          "tidsstempel": "2020-11-02T13:09:00.712441",
          "beregningId": "00000000-0000-0000-0000-000000000000"
        },
        {
          "tidsstempel": "2020-10-01T23:59:59.712441",
          "beregningId": "00000000-0000-0000-0000-000000000000"
        }
      ]
    }
  ],
  "skjemaVersjon": 77
}
"""
