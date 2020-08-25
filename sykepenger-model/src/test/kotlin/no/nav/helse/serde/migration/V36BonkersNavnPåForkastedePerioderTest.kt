package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class V36BonkersNavnPåForkastedePerioderTest {

    @Test
    fun `first og second skal være vedtaksperiode og årsak`() {
        val migrated = listOf(V36BonkersNavnPåForkastedePerioder())
            .migrate(serdeObjectMapper.readTree(originalJson))
        val expected = serdeObjectMapper.readTree(expectedJson)

        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private val originalJson =
    """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "id": 123
        }
      ],
      "forkastede": [
        {
          "first": {
              "id": 456,
              "sykdomshistorikk": [
                {
                  "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03217",
                  "tidsstempel": "2020-06-26T10:43:04.197284",
                  "hendelseSykdomstidslinje": {},
                  "beregnetSykdomstidslinje": {}
                }, {
                  "beregnetSykdomstidslinje": {}
                }
              ]
          },
          "second" : "UKJENT"
        },
        {
          "first": {
              "id": 789,
              "sykdomshistorikk": [
                {
                  "hendelseId": "aaaaaaaa-f5e1-4b66-9afc-b0ef0ef03217",
                  "tidsstempel": "2019-06-26T10:43:04.197284",
                  "hendelseSykdomstidslinje": {},
                  "beregnetSykdomstidslinje": {}
                }, {
                  "beregnetSykdomstidslinje": {}
                }
              ]
          },
          "second" : "UKJENT"
        }
      ]
    }
  ],
  "skjemaVersjon": 35
}
"""

@Language("JSON")
private val expectedJson =
    """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "id": 123
        }
      ],
      "forkastede": [
        {
          "vedtaksperiode": {
              "id": 456,
              "sykdomshistorikk": [
                {
                  "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03217",
                  "tidsstempel": "2020-06-26T10:43:04.197284",
                  "hendelseSykdomstidslinje": {},
                  "beregnetSykdomstidslinje": {}
                }, {
                  "beregnetSykdomstidslinje": {}
                }
              ]
          },
          "årsak": "UKJENT"
        },
        {
          "vedtaksperiode": {
              "id": 789,
              "sykdomshistorikk": [
                {
                  "hendelseId": "aaaaaaaa-f5e1-4b66-9afc-b0ef0ef03217",
                  "tidsstempel": "2019-06-26T10:43:04.197284",
                  "hendelseSykdomstidslinje": {},
                  "beregnetSykdomstidslinje": {}
                }, {
                  "beregnetSykdomstidslinje": {}
                }
              ]
          },
          "årsak": "UKJENT"
        }
      ]
    }
  ],
  "skjemaVersjon": 36
}
"""
