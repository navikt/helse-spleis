package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V30AvviksprosentSomNullableTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `endrer avviksprosent fra NaN til null`() {
        val json = objectMapper.readTree(personJson)
        listOf(V30AvviksprosentSomNullable()).migrate(json)
        val expected = objectMapper.readTree(expectedPersonJson)
        assertEquals(expected, json)
    }
}

@Language("JSON")
private const val personJson = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "dataForVilkårsvurdering": {
            "avviksprosent": "NaN"
          }
        },
        {
          "dataForVilkårsvurdering": null
        }
      ],
      "forkastede": [
        {
          "dataForVilkårsvurdering": {
            "avviksprosent": "NaN"
          }
        },
        {
          "dataForVilkårsvurdering": null
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
          "dataForVilkårsvurdering": {
            "avviksprosent": null
          }
        },
        {
          "dataForVilkårsvurdering": null
        }
      ],
      "forkastede": [
        {
          "dataForVilkårsvurdering": {
            "avviksprosent": null
          }
        },
        {
          "dataForVilkårsvurdering": null
        }
      ]
    }
  ],
  "skjemaVersjon": 30
}
"""
