package no.nav.helse.spleis.e2e

import no.nav.helse.testhelpers.januar
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Test

internal class HendelseYtelserMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `ingen historie med Søknad først`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0, listOf(Triple(3.januar, 26.januar, 100)))

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_GAP",
            "AVVENTER_VILKÅRSPRØVING_GAP",
            "AVVENTER_HISTORIKK",
            "TIL_INFOTRYGD"
        )
    }
}


