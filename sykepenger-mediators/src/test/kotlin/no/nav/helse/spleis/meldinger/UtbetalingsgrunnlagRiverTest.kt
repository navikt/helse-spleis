package no.nav.helse.spleis.meldinger

import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.person.TilstandType
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.TestMessageFactory
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*

internal class UtbetalingsgrunnlagRiverTest : RiverTest() {
    val messageFactory = TestMessageFactory("20046913370", "69", "98765432", 420.69)

    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        UtbetalingsgrunnlagRiver(rapidsConnection, mediator)
    }

    @Disabled
    @Test
    fun `gjennkjenner ikke vilkårsgrunnlag som utbetalingsgrunnlag`() {
        assertIgnored(
            messageFactory.lagVilkårsgrunnlag(
                vedtaksperiodeId = UUID.randomUUID(),
                tilstand = TilstandType.AVVENTER_VILKÅRSPRØVING,
                inntekter = emptyList(),
                inntekterForSykepengegrunnlag = emptyList(),
                arbeidsforhold = emptyList(),
                medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja
            )
        )
    }
}
