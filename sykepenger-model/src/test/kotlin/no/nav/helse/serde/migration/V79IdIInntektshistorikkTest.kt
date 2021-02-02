package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class V79IdIInntektshistorikkTest {
    @Test
    fun `legger på id i inntektshistorikk`() {
        val result = listOf(V79IdIInntektshistorikk())
            .migrate(serdeObjectMapper.readTree(before))

        val ids = result["arbeidsgivere"].flatMap { it["inntektshistorikk"] }.map { it["id"]?.asText() }

        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun `legger på id i innslag`() {
        val result = listOf(V79IdIInntektshistorikk())
            .migrate(serdeObjectMapper.readTree(before))

        val idsPerEntry = result["arbeidsgivere"]
            .flatMap { it["inntektshistorikk"] }
            .map { it["inntektsopplysninger"].map { innslag -> innslag["id"]?.asText() } }

        assertEquals(3, idsPerEntry[0].distinct().size)
        assertEquals(2, idsPerEntry[1].distinct().size)
        assertEquals(1, idsPerEntry[2].distinct().size)

        fun filtrerPåHendelseId(hendelseId: String) = result["arbeidsgivere"]
            .flatMap { it["inntektshistorikk"] }
            .map { innslag ->
                innslag["inntektsopplysninger"].filter { it.hendelseId() == hendelseId }
            }

        assertEquals(3, idsPerEntry.flatten().distinct().size)

        val hendelse1 = filtrerPåHendelseId("ebacd9e7-2608-494c-ba58-2b12ed4d808f")
        val hendelse2 = filtrerPåHendelseId("5ed08488-ce13-41d6-a0ff-1a9bbdbf47bf")
        val hendelse3 = filtrerPåHendelseId("94c7dda9-9d5e-41ab-bdd6-5760e9dff465")

        assertEquals(3, hendelse1.count { it == hendelse1.first() })
        assertEquals(2, hendelse2.count { it == hendelse2.first() })
        assertEquals(1, hendelse3.count { it == hendelse3.first() })
    }
}

private fun JsonNode.hendelseId() = (path("skatteopplysninger").firstOrNull() ?: this).get("hendelseId").asText()

@Language("JSON")
private val before = """
{
  "arbeidsgivere": [
    {
      "inntektshistorikk": [
        {
          "inntektsopplysninger": [
            {
              "dato": "2018-01-01",
              "hendelseId": "ebacd9e7-2608-494c-ba58-2b12ed4d808f",
              "kilde": "INNTEKTSMELDING"
            },
            {
              "skatteopplysninger": [
                {
                  "dato": "2018-01-01",
                  "hendelseId": "5ed08488-ce13-41d6-a0ff-1a9bbdbf47bf",
                  "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                },
                {
                  "dato": "2018-01-01",
                  "hendelseId": "5ed08488-ce13-41d6-a0ff-1a9bbdbf47bf",
                  "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                }
              ]
            },
            {
              "dato": "2017-01-01",
              "hendelseId": "94c7dda9-9d5e-41ab-bdd6-5760e9dff465",
              "kilde": "INFOTRYGD"
            }
          ]
        },
        {
          "inntektsopplysninger": [
            {
              "dato": "2018-01-01",
              "hendelseId": "ebacd9e7-2608-494c-ba58-2b12ed4d808f",
              "kilde": "INNTEKTSMELDING"
            },
            {
              "skatteopplysninger": [
                {
                  "dato": "2018-01-01",
                  "hendelseId": "5ed08488-ce13-41d6-a0ff-1a9bbdbf47bf",
                  "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                },
                {
                  "dato": "2018-01-01",
                  "hendelseId": "5ed08488-ce13-41d6-a0ff-1a9bbdbf47bf",
                  "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                }
              ]
            }
          ]
        },
        {
          "inntektsopplysninger": [
            {
              "dato": "2018-01-01",
              "hendelseId": "ebacd9e7-2608-494c-ba58-2b12ed4d808f",
              "kilde": "INNTEKTSMELDING"
            }
          ]
        }
      ]
    }
  ],
  "skjemaVersjon": 73
}"""
