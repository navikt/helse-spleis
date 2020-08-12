package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V33BehovtypeAktivitetsloggTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `legger til forlengelseFraInfotrygd`() {
        val json = objectMapper.readTree(json)
        listOf(V33BehovtypeAktivitetslogg()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        val expected = objectMapper.readTree(expectedJson)
        assertEquals(expected, migratedJson)
    }
}

@Language("JSON")
private const val json = """{
  "aktivitetslogg": {
    "aktiviteter": [
      {
        "alvorlighetsgrad": "BEHOV",
        "behovtype": "Inntektsberegning",
        "melding": "Trenger inntektsberegning",
        "detaljer": {
          "beregningStart": "2018-09",
          "beregningSlutt": "2019-08"
        },
        "tidsstempel": "2020-06-15 17:31:08.155"
      },
      {
        "alvorlighetsgrad": "BEHOV",
        "behovtype": "Sykepengehistorikk",
        "melding": "Trenger sykepengehistorikk fra Infotrygd",
        "detaljer": {
          "historikkFom": "2016-05-29",
          "historikkTom": "2020-06-18"
        },
        "tidsstempel": "2020-07-03 17:49:58.511"
      },
      {
        "alvorlighetsgrad": "BEHOV",
        "behovtype": "Inntektsberegning",
        "melding": "Trenger inntektsberegning",
        "detaljer": {
          "beregningStart": "2018-09",
          "beregningSlutt": "2019-08"
        },
        "tidsstempel": "2020-07-21 13:03:31.496"
      }
    ]
  },
  "skjemaVersjon": 32
}"""

@Language("JSON")
private const val expectedJson = """{
  "aktivitetslogg": {
    "aktiviteter": [
      {
        "alvorlighetsgrad": "BEHOV",
        "behovtype": "InntekterForSammenligningsgrunnlag",
        "melding": "Trenger inntektsberegning",
        "detaljer": {
          "beregningStart": "2018-09",
          "beregningSlutt": "2019-08"
        },
        "tidsstempel": "2020-06-15 17:31:08.155"
      },
      {
        "alvorlighetsgrad": "BEHOV",
        "behovtype": "Sykepengehistorikk",
        "melding": "Trenger sykepengehistorikk fra Infotrygd",
        "detaljer": {
          "historikkFom": "2016-05-29",
          "historikkTom": "2020-06-18"
        },
        "tidsstempel": "2020-07-03 17:49:58.511"
      },
      {
        "alvorlighetsgrad": "BEHOV",
        "behovtype": "InntekterForSammenligningsgrunnlag",
        "melding": "Trenger inntektsberegning",
        "detaljer": {
          "beregningStart": "2018-09",
          "beregningSlutt": "2019-08"
        },
        "tidsstempel": "2020-07-21 13:03:31.496"
      }
    ]
  },
  "skjemaVersjon": 33
}"""
