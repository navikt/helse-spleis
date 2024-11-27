package no.nav.helse.spleis.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.UtbetalingshistorikkRiver
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class UtbetalingshistorikkRiverTest : RiverTest() {
    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        UtbetalingshistorikkRiver(rapidsConnection, mediator)
    }

    @Test
    fun `Kan mappe om message til modell uten feil`() {
        assertNoErrors(json)
    }

    @Test
    fun `håndterer ugyldig periode`() {
        assertNoErrors(ugyldigPeriode)
    }

    @Test
    fun `ignorerer gammel historikk`() {
        assertIgnored(gammelHistorikk)
    }

    @Test
    fun `ignorerer behov med flere løsninger enn bare Sykepengehistorikk`() {
        assertIgnored(komposittbehov)
    }

    @Test
    fun `ignorerer behov med flere fagsystemId`() {
        assertIgnored(medFagsystemId)
    }

    @Language("JSON")
    private val json = """
      {
          "@event_name": "behov",
          "tilstand": "AVVENTER_HISTORIKK",
          "historikkFom": "2014-12-08",
          "historikkTom": "2019-12-08",
          "@behov": [
            "Sykepengehistorikk"
          ],
          "@id": "${UUID.randomUUID()}",
          "@opprettet": "2020-01-24T11:25:00",
          "fødselsnummer": "08127411111",
          "organisasjonsnummer": "orgnummer",
          "vedtaksperiodeId": "${UUID.randomUUID()}",
          "@løsning": {
            "Sykepengehistorikk": [
              {
                "statslønn":  false,
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
                ],
                "arbeidsKategoriKode": "01"
              }
            ]
          },
          "@final": true,
          "@besvart": "${LocalDateTime.now()}"
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
            "Sykepengehistorikk"
          ],
          "@id": "${UUID.randomUUID()}",
          "@opprettet": "2020-01-24T11:25:00",
          "fødselsnummer": "08127411111",
          "organisasjonsnummer": "orgnummer",
          "vedtaksperiodeId": "${UUID.randomUUID()}",
          "@løsning": {
            "Sykepengehistorikk": [
              {
                "statslønn": false,
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
                ],
                "arbeidsKategoriKode": "01"
              }
            ]
          },
          "@final": true,
          "@besvart": "${LocalDateTime.now()}"
        }
    """

    @Language("JSON")
    private val gammelHistorikk = """
        {
          "@event_name": "behov",
          "tilstand": "AVVENTER_HISTORIKK",
          "historikkFom": "2015-12-08",
          "historikkTom": "2019-12-08",
          "@behov": [
            "Sykepengehistorikk"
          ],
          "@id": "${UUID.randomUUID()}",
          "@opprettet": "2020-01-24T11:25:00",
          "fødselsnummer": "08127411111",
          "organisasjonsnummer": "orgnummer",
          "vedtaksperiodeId": "${UUID.randomUUID()}",
          "@løsning": {
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
          "@besvart": "${LocalDateTime.now().minusHours(2)}"
        }
    """

    @Language("JSON")
    private val komposittbehov = """
        {
          "@event_name": "behov",
          "tilstand": "AVVENTER_HISTORIKK",
          "historikkFom": "2015-12-08",
          "historikkTom": "2019-12-08",
          "@behov": [
            "Sykepengehistorikk", "Dagpenger"
          ],
          "@id": "${UUID.randomUUID()}",
          "@opprettet": "2020-01-24T11:25:00",
          "fødselsnummer": "08127411111",
          "organisasjonsnummer": "orgnummer",
          "vedtaksperiodeId": "${UUID.randomUUID()}",
          "@løsning": {
            "Sykepengehistorikk": []
          },
          "@final": true,
          "@besvart": "${LocalDateTime.now()}"
        }
    """

    // hva betyr det at den har fagsystemId, da?
    @Language("JSON")
    private val medFagsystemId = """
        {
          "@event_name": "behov",
          "tilstand": "AVVENTER_HISTORIKK",
          "historikkFom": "2014-12-08",
          "historikkTom": "2019-12-08",
          "@behov": [
            "Sykepengehistorikk"
          ],
          "@id": "${UUID.randomUUID()}",
          "@opprettet": "2020-01-24T11:25:00",
          "fødselsnummer": "08127411111",
          "organisasjonsnummer": "orgnummer",
          "vedtaksperiodeId": "${UUID.randomUUID()}",
          "@løsning": {
            "Sykepengehistorikk": [
              {
                "statslønn":  false,
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
                ],
                "arbeidsKategoriKode": "01"
              }
            ]
          },
          "@final": true,
          "@besvart": "${LocalDateTime.now()}",
          "fagsystemId": "EnFagsystemID"
        }
    """
}
