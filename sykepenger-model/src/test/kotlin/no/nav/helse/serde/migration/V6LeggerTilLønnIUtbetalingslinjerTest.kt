package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V6LeggerTilLønnIUtbetalingslinjerTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `setter lønn basert på dagsats og grad`() {
        val json = objectMapper.readTree(personJson)
        listOf(
            V6LeggerTilLønnIUtbetalingslinjer()
        ).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())

        val lønn = migratedJson["arbeidsgivere"]
            .first()["utbetalinger"]
            .first()["arbeidsgiverOppdrag"]["linjer"]
            .first()["lønn"].asInt()
        assertEquals(1789, lønn)
    }
}

@Language("JSON")
private const val personJson = """
{
  "arbeidsgivere": [
    {
      "utbetalinger": [
        {
          "arbeidsgiverOppdrag": {
            "mottaker": "987654321",
            "fagområde": "SPREF",
            "linjer": [
              {
                "fom": "2018-01-17",
                "tom": "2018-01-31",
                "dagsats": 1431,
                "grad": 80.0,
                "refFagsystemId": null,
                "delytelseId": 1,
                "datoStatusFom": null,
                "statuskode": null,
                "refDelytelseId": null,
                "endringskode": "NY",
                "klassekode": "SPREFAG-IOP"
              }
            ],
            "fagsystemId": "7YKKTYCKPNAJJKTTBADQXUC7ZA",
            "endringskode": "NY",
            "sisteArbeidsgiverdag": null,
            "sjekksum": -873851477
          }
        }
      ]
    }
  ]
}
"""
