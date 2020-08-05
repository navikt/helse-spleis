package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class V31LeggerTilInntektendringTidsstempelTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `legger til inntektskildetype til inntekter`() {
        val migrated = listOf(V31LeggerTilInntektendringTidsstempel()).migrate(objectMapper.readTree(originalJson))
        val expected = objectMapper.readTree(expectedJson)

        assertEquals(expected, migrated)
    }
}


@Language("JSON")
private val originalJson = """
{
  "arbeidsgivere": [
    {
        "inntekter": [
          {
            "fom": "2017-12-31",
            "hendelseId": "259645c2-4465-4c58-b466-505d9a87dfb1",
            "beløp": 31000.0,
            "kilde": "INNTEKTSMELDING"
          },
          {
            "fom": "2018-02-02",
            "hendelseId": "259645c2-4465-4c58-b466-505d9a87dfb1",
            "beløp": 1000.0,
            "kilde": "INNTEKTSMELDING"
          },
          {
            "fom": "2019-01-31",
            "hendelseId": "259645c2-4465-4c58-b466-505d9a87dfb1",
            "beløp": 2000.0,
            "kilde": "INNTEKTSMELDING"
          }
        ]
    },
    {
        "inntekter": [
          {
            "fom": "2017-12-31",
            "hendelseId": "259645c2-4465-4c58-b466-505d9a87dfb1",
            "beløp": 31000.0,
            "kilde": "INNTEKTSMELDING"
          },
          {
            "fom": "2018-02-02",
            "hendelseId": "259645c2-4465-4c58-b466-505d9a87dfb1",
            "beløp": 1000.0,
            "kilde": "INNTEKTSMELDING"
          },
          {
            "fom": "2019-01-31",
            "hendelseId": "259645c2-4465-4c58-b466-505d9a87dfb1",
            "beløp": 2000.0,
            "kilde": "INNTEKTSMELDING"
          }
        ]
    }
  ],
  "skjemaVersjon": 29
}
"""
private val expectedTidsstempel = "\"${LocalDate.now().atStartOfDay()}\""

@Language("JSON")
private val expectedJson = """
{
  "arbeidsgivere": [
    {
        "inntekter": [
          {
            "fom": "2017-12-31",
            "hendelseId": "259645c2-4465-4c58-b466-505d9a87dfb1",
            "beløp": 31000.0,
            "kilde": "INNTEKTSMELDING",
            "tidsstempel": $expectedTidsstempel
          },
          {
            "fom": "2018-02-02",
            "hendelseId": "259645c2-4465-4c58-b466-505d9a87dfb1",
            "beløp": 1000.0,
            "kilde": "INNTEKTSMELDING",
            "tidsstempel": $expectedTidsstempel
          },
          {
            "fom": "2019-01-31",
            "hendelseId": "259645c2-4465-4c58-b466-505d9a87dfb1",
            "beløp": 2000.0,
            "kilde": "INNTEKTSMELDING",
            "tidsstempel": $expectedTidsstempel
          }
        ]
    },
    {
        "inntekter": [
          {
            "fom": "2017-12-31",
            "hendelseId": "259645c2-4465-4c58-b466-505d9a87dfb1",
            "beløp": 31000.0,
            "kilde": "INNTEKTSMELDING",
            "tidsstempel": $expectedTidsstempel
          },
          {
            "fom": "2018-02-02",
            "hendelseId": "259645c2-4465-4c58-b466-505d9a87dfb1",
            "beløp": 1000.0,
            "kilde": "INNTEKTSMELDING",
            "tidsstempel": $expectedTidsstempel
          },
          {
            "fom": "2019-01-31",
            "hendelseId": "259645c2-4465-4c58-b466-505d9a87dfb1",
            "beløp": 2000.0,
            "kilde": "INNTEKTSMELDING",
            "tidsstempel": $expectedTidsstempel
          }
        ]
    }
  ],
  "skjemaVersjon": 31
}
"""
