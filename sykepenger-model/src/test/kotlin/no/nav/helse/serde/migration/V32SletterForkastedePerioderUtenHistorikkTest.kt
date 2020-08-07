package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class V32SletterForkastedePerioderUtenHistorikkTest {

    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `fjerner ikke forkastet periode med innhold`() {
        val original = objectMapper.readTree(personMedForkastetPeriode)
        val expected = objectMapper.readTree(expectedpersonMedForkastetPeriode)

        Assertions.assertEquals(expected, listOf(V32SletterForkastedePerioderUtenHistorikk()).migrate(original))
    }

    @Test
    fun `fjerner forkastet periode med tom historikk`() {
        val original = objectMapper.readTree(personMedTomForkastetPeriode)
        val expected = objectMapper.readTree(expectedpersonMedTomForkastetPeriode)

        Assertions.assertEquals(expected, listOf(V32SletterForkastedePerioderUtenHistorikk()).migrate(original))
    }

    @Test
    fun `fjerner bare den forkastede perioden som er tom`() {
        val original = objectMapper.readTree(personForkastetPerioderMedOgUtenHistorikk)
        val expected = objectMapper.readTree(expectedpersonMedForkastetPeriode)

        Assertions.assertEquals(expected, listOf(V32SletterForkastedePerioderUtenHistorikk()).migrate(original))
    }
}

@Language("JSON")
private val personMedTomForkastetPeriode = """
    {
      "aktørId": "aktørId",
      "fødselsnummer": "fnr",
      "arbeidsgivere": [
        {
          "id": "a8d9144d-7911-47b3-ac81-bf49867a5b4d",
          "organisasjonsnummer": "orgnummer",
          "forkastede": [
            {
              "id": "uuid-1",
              "sykdomshistorikk": []
            }
          ]
        }
      ],
      "skjemaVersjon": 31
    }
"""

@Language("JSON")
private val expectedpersonMedTomForkastetPeriode = """
    {
      "aktørId": "aktørId",
      "fødselsnummer": "fnr",
      "arbeidsgivere": [
        {
          "id": "a8d9144d-7911-47b3-ac81-bf49867a5b4d",
          "organisasjonsnummer": "orgnummer",
          "forkastede": []
        }
      ],
      "skjemaVersjon": 32
    }
"""


@Language("JSON")
private val personMedForkastetPeriode = """
    {
      "aktørId": "aktørId",
      "fødselsnummer": "fnr",
      "arbeidsgivere": [
        {
          "id": "a8d9144d-7911-47b3-ac81-bf49867a5b4d",
          "organisasjonsnummer": "orgnummer",
          "forkastede": [
            {
            "id": "uuid-2",
              "sykdomshistorikk": [{
              "beregnetSykdomstidslinje": {
                "dager": [
                  {
                    "arbeidsgiverBetalingProsent": 100,
                    "dato": "2020-04-27",
                    "grad": 100,
                    "kilde": {
                      "id": "30a8f8e6-14ee-4a72-90ad-39a61471afad",
                      "type": "Søknad"
                    },
                    "type": "SYKEDAG"
                  }
                ],
                "låstePerioder": []
              },
              "hendelseId": "30a8f8e6-14ee-4a72-90ad-39a61471afad",
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "arbeidsgiverBetalingProsent": 100,
                    "dato": "2020-04-27",
                    "grad": 100,
                    "kilde": {
                      "id": "30a8f8e6-14ee-4a72-90ad-39a61471afad",
                      "type": "Søknad"
                    },
                    "type": "SYKEDAG"
                  }
                ],
                "låstePerioder": []
              },
              "tidsstempel": "2020-05-12T13:48:10.469258"
            }]
            }
          ]
        }
      ],
      "skjemaVersjon": 31
    }
"""

@Language("JSON")
private val expectedpersonMedForkastetPeriode = """
    {
      "aktørId": "aktørId",
      "fødselsnummer": "fnr",
      "arbeidsgivere": [
        {
          "id": "a8d9144d-7911-47b3-ac81-bf49867a5b4d",
          "organisasjonsnummer": "orgnummer",
          "forkastede": [
            {
            "id": "uuid-2",
              "sykdomshistorikk": [{
              "beregnetSykdomstidslinje": {
                "dager": [
                  {
                    "arbeidsgiverBetalingProsent": 100,
                    "dato": "2020-04-27",
                    "grad": 100,
                    "kilde": {
                      "id": "30a8f8e6-14ee-4a72-90ad-39a61471afad",
                      "type": "Søknad"
                    },
                    "type": "SYKEDAG"
                  }
                ],
                "låstePerioder": []
              },
              "hendelseId": "30a8f8e6-14ee-4a72-90ad-39a61471afad",
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "arbeidsgiverBetalingProsent": 100,
                    "dato": "2020-04-27",
                    "grad": 100,
                    "kilde": {
                      "id": "30a8f8e6-14ee-4a72-90ad-39a61471afad",
                      "type": "Søknad"
                    },
                    "type": "SYKEDAG"
                  }
                ],
                "låstePerioder": []
              },
              "tidsstempel": "2020-05-12T13:48:10.469258"
            }]
            }
          ]
        }
      ],
      "skjemaVersjon": 32
    }
"""


@Language("JSON")
private val personForkastetPerioderMedOgUtenHistorikk = """
    {
      "aktørId": "aktørId",
      "fødselsnummer": "fnr",
      "arbeidsgivere": [
        {
          "id": "a8d9144d-7911-47b3-ac81-bf49867a5b4d",
          "organisasjonsnummer": "orgnummer",
          "forkastede": [
            {
            "id": "uuid-2",
              "sykdomshistorikk": [{
              "beregnetSykdomstidslinje": {
                "dager": [
                  {
                    "arbeidsgiverBetalingProsent": 100,
                    "dato": "2020-04-27",
                    "grad": 100,
                    "kilde": {
                      "id": "30a8f8e6-14ee-4a72-90ad-39a61471afad",
                      "type": "Søknad"
                    },
                    "type": "SYKEDAG"
                  }
                ],
                "låstePerioder": []
              },
              "hendelseId": "30a8f8e6-14ee-4a72-90ad-39a61471afad",
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "arbeidsgiverBetalingProsent": 100,
                    "dato": "2020-04-27",
                    "grad": 100,
                    "kilde": {
                      "id": "30a8f8e6-14ee-4a72-90ad-39a61471afad",
                      "type": "Søknad"
                    },
                    "type": "SYKEDAG"
                  }
                ],
                "låstePerioder": []
              },
              "tidsstempel": "2020-05-12T13:48:10.469258"
            }]
            },
            {
              "id": "uuid-1",
              "sykdomshistorikk": []
            }
          ]
        }
      ],
      "skjemaVersjon": 31
    }
"""
