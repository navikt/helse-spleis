package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class V35ÅrsakTilForkastingTest {

    @Test
    fun `legger til årsak for forkastede vedtaksperioder`() {
        val migrated = listOf(V35ÅrsakTilForkasting())
            .migrate(serdeObjectMapper.readTree(originalJson()))
        val expected = serdeObjectMapper.readTree(expectedJson())

        assertEquals(expected, migrated)
    }

    @Test
    fun `gjør ingenting hvis det ikke fins noen forkastede perioder`() {
        val migrated = listOf(V35ÅrsakTilForkasting())
            .migrate(serdeObjectMapper.readTree(originalUtenForkastedeJson()))
        val expected = serdeObjectMapper.readTree(expectedUtenForkastedeJson())

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
          "id": 123
        }
      ],
      "forkastede": [
        {
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
        {
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
        }
      ]
    }
  ],
  "skjemaVersjon": 34
}
"""

@Language("JSON")
private fun expectedJson() = """{
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
  "skjemaVersjon": 35
}
"""

@Language("JSON")
private fun originalUtenForkastedeJson() =
    """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "id": 111
        }
      ],
      "forkastede": []    }
  ],
  "skjemaVersjon": 34
}
"""

@Language("JSON")
private fun expectedUtenForkastedeJson() = """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "id": 111
        }
      ],
      "forkastede": []
    }
  ],
  "skjemaVersjon": 35
}
"""
