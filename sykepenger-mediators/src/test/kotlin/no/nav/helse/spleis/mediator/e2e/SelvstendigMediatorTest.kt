package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.Toggle
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import org.junit.jupiter.api.Test

internal class SelvstendigMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun selvstendigsøknad() = Toggle.SelvstendigNæringsdrivende.enable {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), venteperiode = 3.januar til 18.januar)
        sendVilkårsgrunnlagSelvstendig(0)
        sendYtelserSelvstendig(0)
        sendSimuleringSelvstendig(0)
        sendUtbetalingsgodkjenningSelvstendig(0)
        sendUtbetaling()
        assertTilstander(
            0,
            "SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK",
            "SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE",
            "SELVSTENDIG_AVVENTER_VILKÅRSPRØVING",
            "SELVSTENDIG_AVVENTER_HISTORIKK",
            "SELVSTENDIG_AVVENTER_SIMULERING",
            "SELVSTENDIG_AVVENTER_GODKJENNING",
            "SELVSTENDIG_TIL_UTBETALING",
            "SELVSTENDIG_AVSLUTTET"
        )
    }
}
