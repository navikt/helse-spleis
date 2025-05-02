package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.Toggle
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import org.junit.jupiter.api.Test


internal class SelvstendigMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun selvstendigsøknad() = Toggle.SelvstendigNæringsdrivende.enable {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSelvstendigsøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        assertTilstander(0, "AVVENTER_INFOTRYGDHISTORIKK", "AVVENTER_BLOKKERENDE_PERIODE", "AVVENTER_VILKÅRSPRØVING", "AVVENTER_HISTORIKK", "AVVENTER_GODKJENNING")
    }
}
