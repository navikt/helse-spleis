package no.nav.helse.spleis.mediator.meldinger

import java.time.LocalDateTime
import java.util.UUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.UtbetalingshistorikkEtterInfotrygdendringRiver
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class UtbetalingshistorikkEtterInfotrygdendringRiverTest : RiverTest() {
    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        UtbetalingshistorikkEtterInfotrygdendringRiver(rapidsConnection, mediator)
    }

    @Test
    fun `Kan mappe om message til modell uten feil`() {
        assertNoErrors(json())
    }

    @Test
    fun `ignorer melding om vedtaksperiodeId er satt`() {
        assertIgnored(json("id"))
    }

    @Language("JSON")
    private fun json(vedtaksperiodeId: String? = null) = """
      {
          "@event_name": "behov",
          "historikkFom": "2014-12-08",
          "historikkTom": "2019-12-08",
          "@behov": [
            "Sykepengehistorikk"
          ],
          "@id": "${UUID.randomUUID()}",
          ${if (vedtaksperiodeId != null) """"vedtaksperiodeId": "$vedtaksperiodeId",""" else ""}
          "@opprettet": "2020-01-24T11:25:00",
          "aktørId": "aktørId",
          "fødselsnummer": "08127411111",
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
}
