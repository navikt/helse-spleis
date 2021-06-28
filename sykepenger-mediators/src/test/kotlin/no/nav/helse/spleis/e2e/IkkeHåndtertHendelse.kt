package no.nav.helse.spleis.e2e

import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.testhelpers.januar
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class IkkeHåndtertHendelse: AbstractEndToEndMediatorTest() {

    @Test
    fun `hendelse ikke håndtert ved korrigerende søknad av utbetalt periode`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendUtbetalingsgrunnlag(0)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenSykepengehistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        sendKorrigerendeSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 80)))

        val hendelseIkkeHåndtert = testRapid.inspektør.siste("hendelse_ikke_håndtert")
        assertNotNull(hendelseIkkeHåndtert)
    }
}
