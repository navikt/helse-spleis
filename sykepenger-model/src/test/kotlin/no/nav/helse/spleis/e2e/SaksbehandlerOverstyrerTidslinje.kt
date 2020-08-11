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

    @Test
    fun `vedtaksperiode rebehandler informasjon etter endring fra saksbehandler`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(0, Søknad.Søknadsperiode.Sykdom(2.januar, 25.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterSimulering(0)
        håndterOverstyring(listOf(manuellArbeidsdag(2.januar), manuellArbeidsgiverdag(18.januar)))

        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(0))

        håndterYtelser(0)   // No history
        håndterSimulering(0)

        assertTilstander(
            0,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
            TilstandType.AVVENTER_VILKÅRSPRØVING_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING
        )
        assertEquals(19.januar, inspektør.utbetalinger.last().utbetalingstidslinje().førsteSykepengedag())
    }

    @Test
    @Disabled
    fun `saksbehandler oppretter gap mellom to tidligere sammenhengede perioder`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(0, Søknad.Søknadsperiode.Sykdom(2.januar, 25.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterSimulering(0)
        håndterUtbetalingsgodkjenning(0, true)
        håndterUtbetalt(0, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertTilstander(
            0,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
            TilstandType.AVVENTER_VILKÅRSPRØVING_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(26.januar, 30.januar, 100))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(26.januar, 30.januar, 100))
        håndterYtelser(1)   // No history
        håndterSimulering(1)
        håndterOverstyring(listOf(manuellArbeidsdag(26.januar)))

        assertEquals(2.januar, inspektør.førsteFraværsdag(0))
        assertEquals(27.januar, inspektør.førsteFraværsdag(1))

        assertEquals(2, inspektør.arbeidsgiverOppdrag.size)
        assertEquals(18.januar, inspektør.arbeidsgiverOppdrag.first().førstedato)
        assertEquals(25.januar, inspektør.arbeidsgiverOppdrag.first().sistedato)
        assertEquals(27.januar, inspektør.arbeidsgiverOppdrag.last().førstedato)
        assertEquals(30, inspektør.arbeidsgiverOppdrag.last().sistedato)

    }

    private fun manuellFeriedag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Feriedag)
    private fun manuellSykedag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Sykedag, 100)
    private fun manuellArbeidsdag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Arbeidsdag)
    private fun manuellArbeidsgiverdag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Egenmeldingsdag)
}
