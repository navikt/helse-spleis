package no.nav.helse.unit.spleis.hendelser.model

import io.mockk.ConstantAnswer
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.hendelser.Ytelser
import no.nav.helse.unit.spleis.hendelser.TestRapid
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class YtelserMessageTest {

    @Test
    fun `Kan mappe om message til modell uten feil`() {
        rapid.sendTestMessage(json)
        assertTrue(recognizedMessage)
    }

    @Test
    fun `håndterer ukjente perioder`() {
        rapid.sendTestMessage(ukjentPeriode)
        assertTrue(recognizedMessage)
    }

    @Test
    fun `håndterer ugyldig periode`() {
        rapid.sendTestMessage(ugyldigPeriode)
        assertTrue(recognizedMessage)
    }

    private var riverError = false
    private var riverSevere = false
    private var recognizedMessage = false
    @BeforeEach
    fun reset() {
        recognizedMessage = false
        riverError = false
        riverSevere = false
        rapid.reset()
    }

    private val messageMediator = mockk<MessageMediator>()
    private val rapid = TestRapid().apply {
        Ytelser(this, messageMediator)
    }
    init {
        every {
            messageMediator.onRecognizedMessage(any(), any())
        } answers {
            recognizedMessage = true
            ConstantAnswer(Unit)
        }
        every {
            messageMediator.onRiverError(any(), any(), any())
        } answers {
            riverError = true
            ConstantAnswer(Unit)
        }
        every {
            messageMediator.onRiverSevere(any(), any(), any())
        } answers {
            riverSevere = true
            ConstantAnswer(Unit)
        }
    }
}

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
