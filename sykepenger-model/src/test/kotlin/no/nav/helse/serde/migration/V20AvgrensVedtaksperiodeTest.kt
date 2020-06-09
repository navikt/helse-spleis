package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V20AvgrensVedtaksperiodeTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `legger på fom og tom på vedtaksperioder`() {
        val json = objectMapper.readTree(personJson)
        listOf(V20AvgrensVedtaksperiode()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        val expected = objectMapper.readTree(expectedPersonJson)
        assertEquals(expected, migratedJson)
    }
}

@Language("JSON")
private const val personJson = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "sykdomshistorikk": [
            {
              "beregnetSykdomstidslinje": {
                "dager": [
                  {
                    "dato": "2018-01-01"
                  },
                  {
                    "dato": "2018-01-30"
                  },
                  {
                    "dato": "2018-01-31"
                  }
                ]
              }
            }
          ],
          "tilstand": "AVSLUTTET"
        }
      ],
      "forkastede": [
        {
          "sykdomshistorikk": [
            {
              "beregnetSykdomstidslinje": {
                "dager": [
                  {
                    "dato": "2017-12-01"
                  },
                  {
                    "dato": "2017-12-22"
                  },
                  {
                    "dato": "2017-12-23"
                  }
                ]
              }
            }
          ],
          "tilstand": "AVSLUTTET"
        }
      ]
    }
  ]
}
"""

@Language("JSON")
private const val expectedPersonJson = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "fom": "2018-01-01",
          "tom": "2018-01-31",
          "sykdomshistorikk": [
            {
              "beregnetSykdomstidslinje": {
                "dager": [
                  {
                    "dato": "2018-01-01"
                  },
                  {
                    "dato": "2018-01-30"
                  },
                  {
                    "dato": "2018-01-31"
                  }
                ]
              }
            }
          ],
          "tilstand": "AVSLUTTET"
        }
      ],
      "forkastede": [
        {
          "fom": "2017-12-01",
          "tom": "2017-12-23",
          "sykdomshistorikk": [
            {
              "beregnetSykdomstidslinje": {
                "dager": [
                  {
                    "dato": "2017-12-01"
                  },
                  {
                    "dato": "2017-12-22"
                  },
                  {
                    "dato": "2017-12-23"
                  }
                ]
              }
            }
          ],
          "tilstand": "AVSLUTTET"
        }
      ]
    }
  ],
  "skjemaVersjon": 20
}
"""
