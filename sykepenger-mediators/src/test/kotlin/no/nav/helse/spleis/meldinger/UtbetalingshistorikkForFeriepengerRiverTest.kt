package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class UtbetalingshistorikkForFeriepengerRiverTest : RiverTest() {
    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        UtbetalingshistorikkForFeriepengerRiver(rapidsConnection, mediator)
    }

    @Test
    fun `Kan mappe om message til modell uten feil`() {
        assertNoErrors(json)
    }

    @Language("JSON")
    private val json = """
    {
      "@event_name": "behov",
      "@id": "4FA6DAC0-7EB1-42F8-A208-E89DB61D6A79",
      "@opprettet": "2021-05-19T15:41:40.438193",
      "@behov": [
        "SykepengehistorikkForFeriepenger"
      ],
      "SykepengehistorikkForFeriepenger": {
        "historikkFom": "2020-01-01",
        "historikkTom": "2020-12-31"
      },
      "aktørId": "aktørId",
      "fødselsnummer": "14123456789",
      "system_read_count": 0,
      "@løsning": {
        "SykepengehistorikkForFeriepenger": {
          "utbetalinger": [
            {
              "fom": "2020-09-05",
              "tom": "2020-09-25",
              "utbetalingsGrad": "100",
              "oppgjorsType": "",
              "utbetalt": "2020-09-25",
              "dagsats": 2176.0,
              "typeKode": "5",
              "typeTekst": "ArbRef",
              "orgnummer": "80000000"
            },
            {
              "fom": "2020-09-04",
              "tom": "2020-09-04",
              "utbetalingsGrad": "100",
              "oppgjorsType": "",
              "utbetalt": "2020-09-04",
              "dagsats": 1000.0,
              "typeKode": "5",
              "typeTekst": "ArbRef",
              "orgnummer": "80000000"
            },
            {
              "fom": "2019-06-02",
              "tom": "2019-06-20",
              "utbetalingsGrad": "100",
              "oppgjorsType": "",
              "utbetalt": "2019-06-20",
              "dagsats": 1000.0,
              "typeKode": "5",
              "typeTekst": "ArbRef",
              "orgnummer": "80000000"
            },
            {
              "fom": "2019-05-17",
              "tom": "2019-06-01",
              "utbetalingsGrad": "100",
              "oppgjorsType": "",
              "utbetalt": "2019-06-01",
              "dagsats": 1000.0,
              "typeKode": "5",
              "typeTekst": "ArbRef",
              "orgnummer": "80000000"
            },
            {
              "fom": "2019-05-01",
              "tom": "2019-05-16",
              "utbetalingsGrad": "100",
              "oppgjorsType": "",
              "utbetalt": "2019-05-16",
              "dagsats": 1000.0,
              "typeKode": "5",
              "typeTekst": "ArbRef",
              "orgnummer": "80000000"
            },
            {
              "fom": "2019-04-15",
              "tom": "2019-04-30",
              "utbetalingsGrad": "100",
              "oppgjorsType": "",
              "utbetalt": "2019-04-30",
              "dagsats": 1000.0,
              "typeKode": "5",
              "typeTekst": "ArbRef",
              "orgnummer": "80000000"
            },
            {
              "fom": "2019-03-30",
              "tom": "2019-04-14",
              "utbetalingsGrad": "100",
              "oppgjorsType": "",
              "utbetalt": "2019-04-14",
              "dagsats": 1000.0,
              "typeKode": "5",
              "typeTekst": "ArbRef",
              "orgnummer": "80000000"
            },
            {
              "fom": "2019-02-04",
              "tom": "2019-03-29",
              "utbetalingsGrad": "100",
              "oppgjorsType": "",
              "utbetalt": "2019-03-29",
              "dagsats": 1000.0,
              "typeKode": "5",
              "typeTekst": "ArbRef",
              "orgnummer": "80000000"
            }
          ],
          "feriepengehistorikk": [
            {
              "orgnummer": "80000000",
              "beløp": 1000.0,
              "fom": "2020-05-01",
              "tom": "2020-05-31"
            }
          ],
          "inntektshistorikk": [
            {
              "sykepengerFom": "2020-09-04",
              "inntekt": 47141.666666666664,
              "orgnummer": "80000000",
              "refusjonTom": null,
              "refusjonTilArbeidsgiver": true
            },
            {
              "sykepengerFom": "2019-02-04",
              "inntekt": 42306.666666666664,
              "orgnummer": "80000000",
              "refusjonTom": null,
              "refusjonTilArbeidsgiver": true
            }
          ],
          "harStatslønn": false,
          "arbeidskategorikoder": {
            "01": "2020-09-25"
          }
        }
      },
      "@final": true,
      "@besvart": "${LocalDateTime.now()}"
    }
    """

}
