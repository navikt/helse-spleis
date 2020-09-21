package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.person.TilstandType
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class OverstyrerTidslinjeTest : AbstractEndToEndTest() {

    @BeforeEach
    fun setup() {
        Toggles.problemdagerSkaperError = false
    }

    @AfterEach
    fun teardown() {
        Toggles.problemdagerSkaperError = true
    }

    @Test
    fun `overstyrer sykedag på slutten av perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 18.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellSykedag(2.januar), manuellArbeidsgiverdag(24.januar), manuellFeriedag(25.januar)))
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

        assertEquals("SSSSHH SSSSSHH SSSSSHH SSUFS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `vedtaksperiode rebehandler informasjon etter endring fra saksbehandler`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode , listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(2.januar, 25.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellArbeidsgiverdag(18.januar)))

        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(0))

        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)

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
    fun `grad over grensen endres på enkeltdag`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(2.januar, 25.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellSykedag(22.januar, 30)))

        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(0))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(3, inspektør.utbetalinger.last().arbeidsgiverOppdrag().size)
        assertEquals(21.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[0].tom)
        assertEquals(30.0, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[1].grad)
        assertEquals(23.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[2].fom)
    }

    @Test
    fun `grad under grensen blir ikke utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(2.januar, 25.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellSykedag(22.januar, 0)))

        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(0))

        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)

        assertEquals(2, inspektør.utbetalinger.last().arbeidsgiverOppdrag().size)
        assertEquals(21.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[0].tom)
        assertEquals(23.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[1].fom)
    }

    @Test
    fun `feriedag i midten av en periode blir ikke utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(2.januar, 25.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellFeriedag(22.januar)))

        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(0))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(2, inspektør.utbetalinger.last().arbeidsgiverOppdrag().size)
        assertEquals(21.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[0].tom)
        assertEquals(23.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[1].fom)
    }

    @Test
    fun `problemdag fra søknad blir fikset`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(2.januar, 25.januar, 100), Søknad.Søknadsperiode.Utlandsopphold(22.januar, 25.januar))
        håndterOverstyring(listOf(manuellFeriedag(22.januar), manuellFeriedag(23.januar), manuellFeriedag(24.januar), manuellFeriedag(25.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellFeriedag(22.januar)))

        assertEquals("SSSSHH SSSSSHH SSSSSHH FFFF", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `Overstyring oppdaterer sykdomstidlinjene`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellFeriedag(26.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

        assertEquals("SSSHH SSSSSHH SSSSSHH SSSSF", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertEquals("SSSHH SSSSSHH SSSSSHH SSSSF", inspektør.sykdomstidslinje.toShortString())
        assertEquals("PPPPP PPPPPPP PPPPNHH NNNNF", inspektør.utbetalingstidslinjer(0).toString())
    }

    @Test
    fun `overstyrer ikke vedtaksperioder som er avsluttet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertThrows<RuntimeException> {
            håndterOverstyring(listOf(manuellFeriedag(26.januar)))
        }
        assertNotEquals("SSSHH SSSSSHH SSSSSHH SSSSF", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertNotEquals("SSSHH SSSSSHH SSSSSHH SSSSF", inspektør.sykdomstidslinje.toShortString())
        assertNotEquals("PPPPP PPPPPPP PPPPNHH NNNNF", inspektør.utbetalingstidslinjer(0).toString())
    }

    private fun manuellFeriedag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Feriedag)
    private fun manuellSykedag(dato: LocalDate, grad: Int = 100) = ManuellOverskrivingDag(dato, Dagtype.Sykedag, grad)
    private fun manuellArbeidsgiverdag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Egenmeldingsdag)
}
