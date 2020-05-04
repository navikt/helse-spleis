package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.*

internal class YtelserRiverTest : RiverTest() {
    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        YtelserRiver(rapidsConnection, mediator)
    }

    @Test
    fun `Kan mappe om message til modell uten feil`() {
        assertNoErrors(json)
    }

    @Test
    fun `håndterer ukjente perioder`() {
        assertNoErrors(ukjentPeriode)
    }

    @Test
    fun `håndterer ugyldig periode`() {
        assertNoErrors(ugyldigPeriode)
    }
}

@Language("JSON")
private val json = """
  {
      "@event_name": "behov",
      "tilstand": "AVVENTER_HISTORIKK",
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
      "fødselsnummer": "08127411111",
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
                "refusjonTom": null,
                "refusjonTilArbeidsgiver": true
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

@Language("JSON")
private val ukjentPeriode = """
    {
      "@event_name": "behov",
      "tilstand": "AVVENTER_HISTORIKK",
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
      "fødselsnummer": "08127411111",
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
                "refusjonTom": null,
                "refusjonTilArbeidsgiver": true
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

@Language("JSON")
private val ugyldigPeriode = """
    {
      "@event_name": "behov",
      "tilstand": "AVVENTER_HISTORIKK",
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
      "fødselsnummer": "08127411111",
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
                "refusjonTom": null,
                "refusjonTilArbeidsgiver": true
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
