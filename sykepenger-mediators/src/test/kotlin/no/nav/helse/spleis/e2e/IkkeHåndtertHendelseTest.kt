package no.nav.helse.spleis.e2e

import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class IkkeHåndtertHendelseTest: AbstractEndToEndMediatorTest() {

    @Test
    fun `sender hendelse_ikke_håndtert ved korrigerende søknad av utbetalt periode`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenSykepengehistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        sendKorrigerendeSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 80)))

        val hendelseIkkeHåndtert = testRapid.inspektør.siste("hendelse_ikke_håndtert")
        assertNotNull(hendelseIkkeHåndtert)
        assertEquals(
            listOf("Mottatt flere søknader for perioden - det støttes ikke før replay av hendelser er på plass"),
            hendelseIkkeHåndtert["årsaker"].toList().map { it.textValue() }
        )
    }

    @Test
    fun `sender hendelse_ikke_håndtert når søknad er for gammel`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            vedtaksperiodeIndeks = 0,
            perioder = listOf(SoknadsperiodeDTO(fom = 27.januar, tom = 30.januar, sykmeldingsgrad = 100)),
            sendtNav = 1.desember.atStartOfDay()
        )

        val hendelseIkkeHåndtert = testRapid.inspektør.siste("hendelse_ikke_håndtert")
        assertNotNull(hendelseIkkeHåndtert)
        assertEquals(
            listOf("Søknaden kan ikke være eldre enn avskjæringsdato", "Forventet ikke Søknad. Oppretter ikke vedtaksperiode."),
            hendelseIkkeHåndtert["årsaker"].toList().map { it.textValue() }
        )
    }
}
