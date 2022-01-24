package no.nav.helse.spleis.e2e

import no.nav.helse.desember
import no.nav.helse.januar
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
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
    fun `sender hendelse_ikke_håndtert når sykmelding er for gammel`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), meldingOpprettet = 5.desember.atStartOfDay())

        val hendelseIkkeHåndtert = testRapid.inspektør.siste("hendelse_ikke_håndtert")
        assertNotNull(hendelseIkkeHåndtert)
        assertEquals(
            listOf("Søknadsperioden kan ikke være eldre enn 6 måneder fra mottattidspunkt"),
            hendelseIkkeHåndtert["årsaker"].toList().map { it.textValue() }
        )
    }

    @Test
    fun `tar bare med errors som er relatert til hendelse`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenSykepengehistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetalingsgodkjenning(0) // for å legge på en feil som ikke skal være med i hendelse_ikke_håndtert
        sendUtbetaling()

        sendKorrigerendeSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 25.januar, sykmeldingsgrad = 80)))

        val hendelseIkkeHåndtert = testRapid.inspektør.siste("hendelse_ikke_håndtert")
        assertNotNull(hendelseIkkeHåndtert)
        assertEquals(
            listOf("Mottatt flere søknader for perioden - det støttes ikke før replay av hendelser er på plass"),
            hendelseIkkeHåndtert["årsaker"].toList().map { it.textValue() }
        )
    }
}
