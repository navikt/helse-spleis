package no.nav.helse.spleis.meldinger

import java.time.LocalDate
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.TestMessageFactory
import no.nav.helse.spleis.TestMessageFactory.Arbeidsgiveropplysning
import no.nav.helse.spleis.TestMessageFactory.Refusjonsopplysning
import no.nav.helse.spleis.TestMessageFactory.Subsumsjon
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.AKTØRID
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.INNTEKT
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.ORGNUMMER
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.UNG_PERSON_FNR_2018
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT_ORDER

internal class OverstyrArbeidsgiveropplysningerRiverTest : RiverTest() {

    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        OverstyrArbeidsgiveropplysningerRiver(rapidsConnection, mediator)
    }

    private val testMessageFactory = TestMessageFactory(
        UNG_PERSON_FNR_2018,
        AKTØRID,
        ORGNUMMER,
        INNTEKT,
        LocalDate.of(1992, 2, 12)
    )

    @Test
    fun `kan mappe melding om overstyring av arbeidsgiveropplysninger til modell uten feil`() {
        assertNoErrors(
            testMessageFactory.lagOverstyrArbeidsgiveropplysninger(
                1.januar,
                mapOf(ORGNUMMER to Arbeidsgiveropplysning(
                    INNTEKT,
                    "forklaring",
                    Subsumsjon("8-15", null, null),
                    listOf(Refusjonsopplysning(1.januar, null, 0.0))
                ))
            )
        )
    }

    @Test
    fun `lager riktig format på OverstyrArbeidsgiveropplysninger-hendelsen`() {
        val overstyrArbeidsgiveropplysninger = testMessageFactory.lagOverstyrArbeidsgiveropplysninger(
            1.januar,
            mapOf(
                ORGNUMMER to Arbeidsgiveropplysning(
                    INNTEKT,
                    "forklaring",
                    Subsumsjon("8-15", null, null),
                    listOf(Refusjonsopplysning(1.januar, 31.januar, INNTEKT/2), Refusjonsopplysning(1.februar, null, 0.0))
                )
            )
        )

        @Language("json")
        val forventetResultat = """
            {
              "@event_name": "overstyr_arbeidsgiveropplysninger",
              "aktørId": "42",
              "fødselsnummer": "12029240045",
              "skjæringstidspunkt": "2018-01-01",
              "arbeidsgiveropplysninger": {
                "987654321": {
                  "månedligInntekt": 31000.0,
                  "forklaring": "forklaring",
                  "refusjonsopplysninger": [
                    {
                      "fom": "2018-01-01",
                      "tom": "2018-01-31",
                      "beløp": 15500.0
                    },
                    {
                      "fom": "2018-02-01",
                      "beløp": 0.0
                    }
                  ],
                  "subsumsjon": {
                    "paragraf": "8-15"
                  }
                }
              }
            }"""

        JSONAssert.assertEquals(forventetResultat, overstyrArbeidsgiveropplysninger.second, STRICT_ORDER)
    }

}