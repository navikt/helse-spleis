package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V68FikseØdelagteUtbetalingerTest {
    @Test
    fun `legger på ny linje på ødelagt utbetaling`() {
        val expected = serdeObjectMapper.readTree(expectedJson)
        val migrated = listOf(V68FikseØdelagteUtbetalinger())
            .migrate(serdeObjectMapper.readTree(originalJson))
        assertEquals(expected, migrated)
    }

}



@Language("JSON")
private val originalJson =
    """{
  "arbeidsgivere": [
    {
      "utbetalinger": [
        {
          "id": "53132067-543c-49d2-883b-04113bdb06cd",
          "arbeidsgiverOppdrag": {
            "linjer": [
              {
                "fom": "2020-09-01",
                "tom": "2020-09-10"
              }
            ],
            "fagsystemId": "AKWJDAKWDJAWKDJAA"
          },
          "status": "UTBETALT",
          "type": "UTBETALING"
        },
        {
          "id": "adasdsad-asd-asd-asd-asdasdsad",
          "arbeidsgiverOppdrag": {
            "linjer": [
              {
                "fom": "2020-09-01",
                "tom": "2020-09-10"
              }
            ],
            "fagsystemId": "sdasdasdsada"
          },
          "status": "UTBETALT",
          "type": "UTBETALING"
        },
        {
          "id": "0a00d06b-60bd-42d9-9e59-b070bfb96ef7",
          "arbeidsgiverOppdrag": {
            "linjer": [
              {
                "fom": "2020-09-01",
                "tom": "2020-09-10"
              }
            ],
            "fagsystemId": "sdkhdksjhgdkjgh"
          },
          "status": "UTBETALT",
          "type": "UTBETALING"
        }
      ]
    }
  ],
  "skjemaVersjon": 66
}
"""

@Language("JSON")
private val expectedJson =
    """{
  "arbeidsgivere": [
    {
      "utbetalinger": [
        {
          "id": "53132067-543c-49d2-883b-04113bdb06cd",
          "arbeidsgiverOppdrag": {
            "linjer": [
              {
                "fom": "2020-09-01",
                "tom": "2020-09-10"
              },
              {
                "fom": "2020-09-19",
                "tom": "2020-09-21",
                "dagsats": 2339,
                "lønn": 2850,
                "grad": 100,
                "refFagsystemId": "AKWJDAKWDJAWKDJAA",
                "delytelseId": 2,
                "datoStatusFom": null,
                "statuskode": null,
                "refDelytelseId": 1,
                "endringskode": "NY",
                "klassekode": "SPREFAG-IOP"
              }
            ],
            "fagsystemId": "AKWJDAKWDJAWKDJAA"
          },
          "status": "UTBETALT",
          "type": "UTBETALING"
        },
        {
          "id": "adasdsad-asd-asd-asd-asdasdsad",
          "arbeidsgiverOppdrag": {
            "linjer": [
              {
                "fom": "2020-09-01",
                "tom": "2020-09-10"
              }
            ],
            "fagsystemId": "sdasdasdsada"
          },
          "status": "UTBETALT",
          "type": "UTBETALING"
        },
        {
          "id": "0a00d06b-60bd-42d9-9e59-b070bfb96ef7",
          "arbeidsgiverOppdrag": {
            "linjer": [
              {
                "fom": "2020-09-01",
                "tom": "2020-09-10"
              },
              {
                "fom": "2020-09-19",
                "tom": "2020-09-22",
                "dagsats": 1871,
                "lønn": 2595,
                "grad": 80,
                "refFagsystemId": "sdkhdksjhgdkjgh",
                "delytelseId": 2,
                "datoStatusFom": null,
                "statuskode": null,
                "refDelytelseId": 1,
                "endringskode": "NY",
                "klassekode": "SPREFAG-IOP"
              }
            ],
            "fagsystemId": "sdkhdksjhgdkjgh"
          },
          "status": "UTBETALT",
          "type": "UTBETALING"
        }
      ]
    }
  ],
  "skjemaVersjon": 68
}
"""
