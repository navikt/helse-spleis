package no.nav.helse.spleis.mediator.e2e

internal class SelvstendigMediatorTest : AbstractEndToEndMediatorTest() {

    /*
    @Test
    fun selvstendigsøknad() = Toggle.SelvstendigNæringsdrivende.enable {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), venteperiode = 3.januar til 18.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK, forventedeFagområder = setOf("SP"))
        assertTilstander(0, "SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK", "SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE", "SELVSTENDIG_AVVENTER_VILKÅRSPRØVING", "SELVSTENDIG_AVVENTER_HISTORIKK", "SELVSTENDIG_AVVENTER_SIMULERING")
    }
    */
}
