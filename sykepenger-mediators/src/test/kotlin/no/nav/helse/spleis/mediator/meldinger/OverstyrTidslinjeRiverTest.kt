package no.nav.helse.spleis.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.desember
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.mediator.TestMessageFactory
import no.nav.helse.spleis.meldinger.OverstyrTidlinjeRiver
import org.junit.jupiter.api.Test

internal class OverstyrTidslinjeRiverTest : RiverTest() {
    private val fabrikk = TestMessageFactory("fnr", "orgnr", 1000.0, 24.desember(2000))

    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        OverstyrTidlinjeRiver(rapidsConnection, mediator)
    }

    @Test
    fun `overstyring uten dager`() {
        assertErrors(fabrikk.lagOverstyringTidslinje(emptyList()))
    }
}
