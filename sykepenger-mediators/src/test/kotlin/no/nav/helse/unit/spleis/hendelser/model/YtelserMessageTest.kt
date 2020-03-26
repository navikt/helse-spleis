package no.nav.helse.unit.spleis.hendelser.model

import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.spleis.hendelser.model.YtelserMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class YtelserMessageTest {

    @Test
    fun `Kan mappe om message til modell uten feil`() {
        YtelserMessage(json, MessageProblems("{}")).asYtelser().also {
            it.valider()
            assertFalse(it.hasErrors())
            assertEquals(0, it.aktivitetsteller())
        }
    }

    @Test
    fun `ukjente perioder gir feil i mapping`() {
        YtelserMessage(ukjentPeriode, MessageProblems("{}")).asYtelser().also {
            it.valider()
            assertTrue(it.hasErrors())
            assertEquals(1, it.aktivitetsteller())
        }
    }

    @Test
    fun `ugyldig periode gir feil i mapping`() {
        YtelserMessage(ugyldigPeriode, MessageProblems("{}")).asYtelser().also {
            it.valider()
            assertTrue(it.hasErrors())
            assertEquals(2, it.aktivitetsteller())
        }
    }
}

private val json = """
    {
      "historikkFom": "2014-12-08",
      "historikkTom": "2019-12-08",
      "@behov": [
        "Sykepengehistorikk",
        "Foreldrepenger"
      ],
      "@id": "${UUID.randomUUID()}",
      "@opprettet": "2020-01-24T11:25:00",
      "hendelse": "Ytelser",
      "aktørId": "aktørId",
      "fødselsnummer": "fnr",
      "organisasjonsnummer": "orgnummer",
      "vedtaksperiodeId": "${UUID.randomUUID()}",
      "@løsning": {
        "Foreldrepenger": {
          "Foreldrepengeytelse": null,
          "Svangerskapsytelse": null
        },
        "Sykepengehistorikk": [
          {
            "fom": "2019-03-11",
            "tom": "2019-04-12",
            "grad": "100",
            "inntektsopplysninger": [
              {
                "sykepengerFom": "2019-03-27",
                "inntekt": 36000,
                "orgnummer": "orgnummer",
                "refusjonTom": null
              }
            ],
            "utbetalteSykeperioder": [
              {
                "fom": "2019-03-28",
                "tom": "2019-04-12",
                "utbetalingsGrad": "100",
                "oppgjorsType": "",
                "utbetalt": "2019-04-23",
                "dagsats": 1400.0,
                "typeKode": "5",
                "typeTekst": "ArbRef",
                "orgnummer": "orgnummer",
                "inntektPerMåned": 36000
              }
            ]
          }
        ]
      },
      "@final": true,
      "@besvart": "2020-01-24T11:25:00"
    }
""".trimIndent()

private val ukjentPeriode = """
    {
      "historikkFom": "2015-12-08",
      "historikkTom": "2019-12-08",
      "@behov": [
        "Sykepengehistorikk",
        "Foreldrepenger"
      ],
      "@id": "${UUID.randomUUID()}",
      "@opprettet": "2020-01-24T11:25:00",
      "hendelse": "Ytelser",
      "aktørId": "aktørId",
      "fødselsnummer": "fnr",
      "organisasjonsnummer": "orgnummer",
      "vedtaksperiodeId": "${UUID.randomUUID()}",
      "@løsning": {
        "Foreldrepenger": {
          "Foreldrepengeytelse": null,
          "Svangerskapsytelse": null
        },
        "Sykepengehistorikk": [
          {
            "fom": "2019-03-28",
            "tom": "2019-04-12",
            "grad": "100",
            "inntektsopplysninger": [
              {
                "sykepengerFom": "2019-03-27",
                "inntekt": 36000,
                "orgnummer": "orgnummer",
                "refusjonTom": null
              }
            ],
            "utbetalteSykeperioder": [
              {
                "fom": "2019-03-28",
                "tom": "2019-04-12",
                "utbetalingsGrad": "100",
                "oppgjorsType": "",
                "utbetalt": "2019-04-23",
                "dagsats": 1400.0,
                "typeKode": "5",
                "typeTekst": "ArbRef",
                "orgnummer": "orgnummer",
                "inntektPerMåned": 36000
              },
              {
                "fom": "2019-03-28",
                "tom": "2019-04-12",
                "utbetalingsGrad": "100",
                "oppgjorsType": "",
                "utbetalt": "2019-04-23",
                "dagsats": 1400.0,
                "typeKode": "",
                "typeTekst": "Ukjent..",
                "orgnummer": "orgnummer",
                "inntektPerMåned": 36000
              }
            ]
          }
        ]
      },
      "@final": true,
      "@besvart": "2020-01-24T11:25:00"
    }
""".trimIndent()

private val ugyldigPeriode = """
    {
      "historikkFom": "2015-12-08",
      "historikkTom": "2019-12-08",
      "@behov": [
        "Sykepengehistorikk",
        "Foreldrepenger"
      ],
      "@id": "${UUID.randomUUID()}",
      "@opprettet": "2020-01-24T11:25:00",
      "hendelse": "Ytelser",
      "aktørId": "aktørId",
      "fødselsnummer": "fnr",
      "organisasjonsnummer": "orgnummer",
      "vedtaksperiodeId": "${UUID.randomUUID()}",
      "@løsning": {
        "Foreldrepenger": {
          "Foreldrepengeytelse": null,
          "Svangerskapsytelse": null
        },
        "Sykepengehistorikk": [
          {
            "fom": "2019-03-28",
            "tom": "2019-04-12",
            "grad": "100",
            "inntektsopplysninger": [
              {
                "sykepengerFom": "2019-03-27",
                "inntekt": 36000,
                "orgnummer": "orgnummer",
                "refusjonTom": null
              }
            ],
            "utbetalteSykeperioder": [
              {
                "fom": null,
                "tom": "2019-04-12",
                "utbetalingsGrad": "100",
                "oppgjorsType": "",
                "utbetalt": "2019-04-23",
                "dagsats": 1400.0,
                "typeKode": "5",
                "typeTekst": "ArbRef",
                "orgnummer": "orgnummer",
                "inntektPerMåned": 36000
              },
              {
                "fom": "2019-03-28",
                "tom": null,
                "utbetalingsGrad": "100",
                "oppgjorsType": "",
                "utbetalt": "2019-04-23",
                "dagsats": 1400.0,
                "typeKode": "5",
                "typeTekst": "ArbRef",
                "orgnummer": "orgnummer",
                "inntektPerMåned": 36000
              }
            ]
          }
        ]
      },
      "@final": true,
      "@besvart": "2020-01-24T11:25:00"
    }
""".trimIndent()
