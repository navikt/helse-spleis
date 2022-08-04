package no.nav.helse.spleis.meldinger

import no.nav.helse.januar
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.TestMessageFactory
import no.nav.helse.spleis.TestMessageFactory.ArbeidsforholdOverstyrt
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.AKTØRID
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.INNTEKT
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.ORGNUMMER
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.UNG_PERSON_FNR_2018
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.UNG_PERSON_FØDSELSDATO
import org.junit.jupiter.api.Test

internal class OverstyrArbeidsforholdRiverTest : RiverTest() {
    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        OverstyrArbeidsforholdRiver(rapidsConnection, mediator)
    }

    private val testMessageFactory = TestMessageFactory(
        UNG_PERSON_FNR_2018,
        AKTØRID,
        ORGNUMMER,
        INNTEKT,
        UNG_PERSON_FØDSELSDATO
    )

    @Test
    fun `kan mappe melding om overstyring av arbeidsforhold til modell uten feil`() {
        assertNoErrors(
            testMessageFactory.lagOverstyrArbeidsforhold(
                1.januar, listOf(
                    ArbeidsforholdOverstyrt(
                        ORGNUMMER,
                        false,
                        "Dette arbeidsforholdet gjelder"
                    ),
                    ArbeidsforholdOverstyrt(
                        "987654322",
                        true,
                        "Dette arbeidsforholdet gjelder ikke"
                    ),
                )
            )
        )
    }

    @Test
    fun `skal feile ved tom liste av overstyrte arbeidsforhold`() {
        assertErrors(
            testMessageFactory.lagOverstyrArbeidsforhold(1.januar, emptyList())
        )
    }

    @Test
    fun `feiler ved manglende forklaring`() {
        assertErrors(
            testMessageFactory.lagOverstyrArbeidsforhold(
                1.januar, listOf(
                    ArbeidsforholdOverstyrt(ORGNUMMER, false, null),
                )
            )
        )

    }
}
