package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.person.TilstandType
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SaksbehandlerOverstyrerTidslinje : AbstractEndToEndTest() {

    @Test
    fun `overstyrer sykedag på slutten av perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(2.januar, 18.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(0, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterSimulering(0)
        håndterOverstyring(listOf(manuellSykedag(2.januar), manuellArbeidsgiverdag(24.januar), manuellFeriedag(25.januar), manuellArbeidsdag(26.januar)))
        håndterUtbetalingsgodkjenning(0, true)

        assertEquals("SSSSHH SSSSSHH SSSSSHH SSUFA", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
    }

    @Disabled
    @Test
    fun `vedtaksperiode rebehandler informasjon etter endring fra saksbehandler`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        håndterSøknadMedValidering(0, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterSimulering(0)
        håndterOverstyring(listOf(manuellArbeidsdag(3.januar), manuellArbeidsgiverdag(19.januar)))

        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(0))
        assertEquals(20.januar, inspektør.utbetalinger.last().utbetalingstidslinje().førsteSykepengedag())
    }

    private fun manuellFeriedag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Feriedag)
    private fun manuellSykedag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Sykedag, 100)
    private fun manuellArbeidsdag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Arbeidsdag)
    private fun manuellArbeidsgiverdag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Egenmeldingsdag)
}
