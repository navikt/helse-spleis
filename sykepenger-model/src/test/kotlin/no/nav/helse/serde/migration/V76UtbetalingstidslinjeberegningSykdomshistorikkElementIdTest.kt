package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V76UtbetalingstidslinjeberegningSykdomshistorikkElementIdTest {
    @Test
    fun `migrerer utbetalingstidslinjeberegning`() {
        val migrated = listOf(V76UtbetalingstidslinjeberegningSykdomshistorikkElementId())
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
          "organisasjonsnummer": "123456789",
          "utbetalingstidslinje": {
            "dager": []
          },
          "tidsstempel": "2020-11-02T13:09:14.712441"
        },
        {
          "organisasjonsnummer": "123456789",
          "utbetalingstidslinje": {
            "dager": []
          },
          "tidsstempel": "2020-12-09T14:29:49.124142"
        },
        {
          "organisasjonsnummer": "123456789",
          "utbetalingstidslinje": {
            "dager": []
          },
          "tidsstempel": "2020-12-22T13:16:47.682093"
        },
        {
          "organisasjonsnummer": "123456789",
          "utbetalingstidslinje": {
            "dager": []
          },
          "tidsstempel": "2021-01-19T09:40:58.424818"
        }
      ],
      "sykdomshistorikk": [
        {
          "id": "36ac7821-5d6d-4a89-9e3d-9a1f0959566e",
          "tidsstempel": "2021-01-31T09:40:55.582101",
          "hendelseSykdomstidslinje": {},
          "beregnetSykdomstidslinje": {}
        },
        {
          "id": "cbafc668-654e-4d16-9f99-e5a95c31b2f1",
          "tidsstempel": "2021-01-19T09:40:55.582101",
          "hendelseSykdomstidslinje": {},
          "beregnetSykdomstidslinje": {}
        },
        {
          "id": "d547cc5f-8001-4675-90fa-12347bf737c5",
          "tidsstempel": "2020-12-05T14:29:49.124142",
          "hendelseSykdomstidslinje": {},
          "beregnetSykdomstidslinje": {}
        },
        {
          "id": "c68b81a2-7e1f-4dcf-babd-99f019ce0b81",
          "tidsstempel": "2020-12-01T14:29:49.124142",
          "hendelseSykdomstidslinje": {},
          "beregnetSykdomstidslinje": {}
        },
        {
          "id": "120b35a4-55a0-4275-8df8-81ae724883ca",
          "tidsstempel": "2020-11-02T13:09:00.712441",
          "hendelseSykdomstidslinje": {},
          "beregnetSykdomstidslinje": {}
        },
        {
          "id": "aaff53ad-3bbb-46b8-807f-1c0f00f861da",
          "tidsstempel": "2020-10-01T23:59:59.712441",
          "hendelseSykdomstidslinje": {},
          "beregnetSykdomstidslinje": {}
        }
      ]
    }
  ],
  "skjemaVersjon": 75
}
"""
@Language("JSON")
private fun expectedJson() =
    """{
  "arbeidsgivere": [
    {
      "beregnetUtbetalingstidslinjer": [
        {
          "sykdomshistorikkElementId": "120b35a4-55a0-4275-8df8-81ae724883ca",
          "organisasjonsnummer": "123456789",
          "utbetalingstidslinje": {
            "dager": []
          },
          "tidsstempel": "2020-11-02T13:09:14.712441"
        },
        {
          "sykdomshistorikkElementId": "d547cc5f-8001-4675-90fa-12347bf737c5",
          "organisasjonsnummer": "123456789",
          "utbetalingstidslinje": {
            "dager": []
          },
          "tidsstempel": "2020-12-09T14:29:49.124142"
        },
        {
          "sykdomshistorikkElementId": "d547cc5f-8001-4675-90fa-12347bf737c5",
          "organisasjonsnummer": "123456789",
          "utbetalingstidslinje": {
            "dager": []
          },
          "tidsstempel": "2020-12-22T13:16:47.682093"
        },
        {
          "sykdomshistorikkElementId": "cbafc668-654e-4d16-9f99-e5a95c31b2f1",
          "organisasjonsnummer": "123456789",
          "utbetalingstidslinje": {
            "dager": []
          },
          "tidsstempel": "2021-01-19T09:40:58.424818"
        }
      ],
      "sykdomshistorikk": [
        {
          "id": "36ac7821-5d6d-4a89-9e3d-9a1f0959566e",
          "tidsstempel": "2021-01-31T09:40:55.582101",
          "hendelseSykdomstidslinje": {},
          "beregnetSykdomstidslinje": {}
        },
        {
          "id": "cbafc668-654e-4d16-9f99-e5a95c31b2f1",
          "tidsstempel": "2021-01-19T09:40:55.582101",
          "hendelseSykdomstidslinje": {},
          "beregnetSykdomstidslinje": {}
        },
        {
          "id": "d547cc5f-8001-4675-90fa-12347bf737c5",
          "tidsstempel": "2020-12-05T14:29:49.124142",
          "hendelseSykdomstidslinje": {},
          "beregnetSykdomstidslinje": {}
        },
        {
          "id": "c68b81a2-7e1f-4dcf-babd-99f019ce0b81",
          "tidsstempel": "2020-12-01T14:29:49.124142",
          "hendelseSykdomstidslinje": {},
          "beregnetSykdomstidslinje": {}
        },
        {
          "id": "120b35a4-55a0-4275-8df8-81ae724883ca",
          "tidsstempel": "2020-11-02T13:09:00.712441",
          "hendelseSykdomstidslinje": {},
          "beregnetSykdomstidslinje": {}
        },
        {
          "id": "aaff53ad-3bbb-46b8-807f-1c0f00f861da",
          "tidsstempel": "2020-10-01T23:59:59.712441",
          "hendelseSykdomstidslinje": {},
          "beregnetSykdomstidslinje": {}
        }
      ]
    }
  ],
  "skjemaVersjon": 76
}
"""

