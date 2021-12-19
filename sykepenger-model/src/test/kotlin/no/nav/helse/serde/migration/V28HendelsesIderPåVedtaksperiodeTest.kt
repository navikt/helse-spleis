package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class V28HendelsesIderPåVedtaksperiodeTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `kopier hendelsesIder fra sykdomshistorikk til vedtaksperiode`() {
        val migrated = listOf(V28HendelsesIderPåVedtaksperiode()).migrate(objectMapper.readTree(originalJson))
        val expected = objectMapper.readTree(expectedJson)

        assertEquals(expected, migrated)
    }
}


@Language("JSON")
private val originalJson = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "sykdomshistorikk": [
            {
              "hendelseId": "c3aeddd6-39b2-47cd-9eb9-b948a370dda2"
            },
            {
              "hendelseId": "15ea33b9-2d6e-4517-a0b3-2bc4f0f5a1fd"
            },
            {
              "hendelseId": "e229a575-8c62-49f5-90e1-a8e027844135"
            }
          ]
        },
        {
          "sykdomshistorikk": [
            {
              "hendelseId": "9897347e-a1fe-4f24-8144-d5ce9fe07da0"
            }
          ]
        }
      ],
      "forkastede": [
        {
          "sykdomshistorikk": [
            {
              "hendelseId": "fdc14c73-7a6f-4f96-9972-c2e9dafd1026"
            },
            {
              "hendelseId": "c1ceaafd-81ae-4bbe-8319-5f8d6919cc79"
            }
          ]
        }
      ]
    }
  ],
  "skjemaVersjon": 27
}
"""

private val expectedJson = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "hendelseIder": [
            "e229a575-8c62-49f5-90e1-a8e027844135",
            "15ea33b9-2d6e-4517-a0b3-2bc4f0f5a1fd",
            "c3aeddd6-39b2-47cd-9eb9-b948a370dda2"
          ],
          "sykdomshistorikk": [
            {
              "hendelseId": "c3aeddd6-39b2-47cd-9eb9-b948a370dda2"
            },
            {
              "hendelseId": "15ea33b9-2d6e-4517-a0b3-2bc4f0f5a1fd"
            },
            {
              "hendelseId": "e229a575-8c62-49f5-90e1-a8e027844135"
            }
          ]
        },
        {
          "hendelseIder": [
            "9897347e-a1fe-4f24-8144-d5ce9fe07da0"
          ],
          "sykdomshistorikk": [
            {
              "hendelseId": "9897347e-a1fe-4f24-8144-d5ce9fe07da0"
            }
          ]
        }
      ],
      "forkastede": [
        {
          "hendelseIder": [
            "c1ceaafd-81ae-4bbe-8319-5f8d6919cc79",
            "fdc14c73-7a6f-4f96-9972-c2e9dafd1026"
          ],
          "sykdomshistorikk": [
            {
              "hendelseId": "fdc14c73-7a6f-4f96-9972-c2e9dafd1026"
            },
            {
              "hendelseId": "c1ceaafd-81ae-4bbe-8319-5f8d6919cc79"
            }
          ]
        }
      ]
    }
  ],
  "skjemaVersjon": 28
}
"""
