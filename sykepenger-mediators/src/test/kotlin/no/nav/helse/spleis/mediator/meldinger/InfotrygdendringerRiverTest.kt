package no.nav.helse.spleis.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import java.time.LocalDate
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.mediator.TestMessageFactory
import no.nav.helse.spleis.mediator.e2e.AbstractEndToEndMediatorTest.Companion.INNTEKT
import no.nav.helse.spleis.mediator.e2e.AbstractEndToEndMediatorTest.Companion.ORGNUMMER
import no.nav.helse.spleis.mediator.e2e.AbstractEndToEndMediatorTest.Companion.UNG_PERSON_FNR_2018
import no.nav.helse.spleis.meldinger.InfotrygdendringerRiver
import org.junit.jupiter.api.Test

internal class InfotrygdendringerRiverTest : RiverTest() {

    @Test
    fun `kan mappe melding om infotrygdendring uten feil`() {
        assertNoErrors(testMessageFactory.lagInfotrygdendringer("1234567"))
    }

    @Test
    fun `kan mappe melding om infotrygdendring med feil`() {
        assertErrors(testMessageFactory.lagInfotrygdendringer("tull"))
    }

    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        InfotrygdendringerRiver(rapidsConnection, mediator)
    }

    private val testMessageFactory =
        TestMessageFactory(UNG_PERSON_FNR_2018, ORGNUMMER, INNTEKT, LocalDate.of(1992, 2, 12))
}
