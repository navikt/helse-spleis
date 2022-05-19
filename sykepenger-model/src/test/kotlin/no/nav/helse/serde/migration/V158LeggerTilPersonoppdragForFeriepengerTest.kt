package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class V158LeggerTilPersonoppdragForFeriepengerTest {
    @Test
    fun `Legger til personoppdrag og sendt-flagg`() {
        val fiksetMigrert = migrer(original).also { jsonNode ->
            jsonNode["arbeidsgivere"]
            .map { it["feriepengeutbetalinger"] }
            .filterIsInstance<ArrayNode>()
            .forEach { feriepengeutbetalinger ->
                feriepengeutbetalinger.forEach { feriepengeutbetaling ->
                    feriepengeutbetaling.path("personoppdrag").let {
                        it as ObjectNode
                        it.remove(listOf("fagsystemId", "tidsstempel"))
                    }
                }
            }
        }
        Assertions.assertEquals(toNode(expected), fiksetMigrert)
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V158LeggerTilPersonoppdragForFeriepenger()).migrate(toNode(json))
}


@Language("JSON")
private val original = """{
  "fødselsnummer": "42",
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "utbetalingId": "532c9b3b-8987-4065-831b-cfe6cb61a8f4",
          "opptjeningsår": "2018",
          "oppdrag": {
            "nettoBeløp": 1606
          },
          "sendTilOppdrag": true
        }
      ]
    }
  ],
  "skjemaVersjon": 157
}
"""

@Language("JSON")
private val expected = """{
  "fødselsnummer": "42",
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "utbetalingId": "532c9b3b-8987-4065-831b-cfe6cb61a8f4",
          "opptjeningsår": "2018",
          "oppdrag": {
            "nettoBeløp": 1606
          },
          "sendTilOppdrag": true,
          "sendPersonoppdragTilOS": false,
          "personoppdrag": {
            "mottaker": "42",
            "fagområde": "SP",
            "linjer": [],
            "endringskode": "NY",
            "sisteArbeidsgiverdag": null,
            "nettoBeløp": 0,
            "stønadsdager": 0,
            "avstemmingsnøkkel": null,
            "status": null,
            "overføringstidspunkt": null,
            "fom": "-999999999-01-01",
            "tom": "-999999999-01-01",
            "erSimulert": false,
            "simuleringsResultat": null
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 158
}
"""
