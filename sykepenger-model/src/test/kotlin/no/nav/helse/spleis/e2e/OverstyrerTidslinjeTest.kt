package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.TilstandType
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.testhelpers.TestEvent
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class OverstyrerTidslinjeTest : AbstractEndToEndTest() {

    @Test
    fun `kan ikke utbetale overstyrt utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 18.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellSykedag(2.januar), manuellArbeidsgiverdag(24.januar), manuellFeriedag(25.januar)))
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
    }

    @Test
    fun `overstyrer sykedag på slutten av perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 18.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellSykedag(2.januar), manuellArbeidsgiverdag(24.januar), manuellFeriedag(25.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
        assertEquals(Utbetaling.Sendt, inspektør.utbetalingtilstand(1))
        assertNotEquals(inspektør.utbetaling(0).arbeidsgiverOppdrag().fagsystemId(), inspektør.utbetaling(1).arbeidsgiverOppdrag().fagsystemId())
        assertEquals("SSSSHH SSSSSHH SSSSSHH SSUFS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `overstyrer siste utbetalte periode`() {
        Toggles.RevurderUtbetaltPeriode.enable {
            håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
            håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 18.januar)), førsteFraværsdag = 2.januar)
            håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt(1.vedtaksperiode)
            håndterOverstyring(listOf(manuellSykedag(26.januar, 80)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertEquals(Utbetaling.Utbetalt, inspektør.utbetalingtilstand(0))
            assertEquals(Utbetaling.Sendt, inspektør.utbetalingtilstand(1))
            assertEquals(inspektør.utbetaling(0).arbeidsgiverOppdrag().fagsystemId(), inspektør.utbetaling(1).arbeidsgiverOppdrag().fagsystemId())
            inspektør.utbetaling(1).arbeidsgiverOppdrag().also { oppdrag ->
                assertEquals(2, oppdrag.size)
                assertEquals(18.januar, oppdrag[0].fom)
                assertEquals(25.januar, oppdrag[0].tom)
                assertEquals(100.0, oppdrag[0].grad)

                assertEquals(26.januar, oppdrag[1].fom)
                assertEquals(26.januar, oppdrag[1].tom)
                assertEquals(80.0, oppdrag[1].grad)
            }
        }
    }

    @Test
    fun `overstyrer siste utbetalte periode i en forlengelse med bare ferie og permisjon`() {
        Toggles.RevurderUtbetaltPeriode.enable {
            håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
            håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 18.januar)), førsteFraværsdag = 2.januar)
            håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt(1.vedtaksperiode)

            håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(29.januar, 23.februar, 100.prosent))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
            håndterUtbetalt(2.vedtaksperiode)

            håndterOverstyring((29.januar til 15.februar).map { manuellFeriedag(it) } + (16.februar til 23.februar).map { manuellPermisjonsdag(it) })
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            assertEquals(Utbetaling.Utbetalt, inspektør.utbetalingtilstand(0))
            assertEquals(Utbetaling.Utbetalt, inspektør.utbetalingtilstand(1))
            assertEquals(Utbetaling.Sendt, inspektør.utbetalingtilstand(2))
            assertEquals(inspektør.utbetaling(1).arbeidsgiverOppdrag().fagsystemId(), inspektør.utbetaling(2).arbeidsgiverOppdrag().fagsystemId())
            inspektør.utbetaling(2).arbeidsgiverOppdrag().also { oppdrag ->
                assertEquals(1, oppdrag.size)
                assertEquals(18.januar, oppdrag[0].fom)
                assertEquals(26.januar, oppdrag[0].tom)
                assertTrue(oppdrag[0].erForskjell())
                assertEquals(100.0, oppdrag[0].grad)
            }
        }
    }

    @Test
    fun `vedtaksperiode rebehandler informasjon etter endring fra saksbehandler`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(2.januar, 25.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellArbeidsgiverdag(18.januar)))
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)

        assertTilstander(
            0,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING
        )
        assertNotEquals(inspektør.utbetaling(0).arbeidsgiverOppdrag().fagsystemId(), inspektør.utbetaling(1).arbeidsgiverOppdrag().fagsystemId())
        assertEquals(19.januar, inspektør.utbetalinger.last().utbetalingstidslinje().sykepengeperiode()?.start)
    }

    @Test
    fun `grad over grensen endres på enkeltdag`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(2.januar, 25.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellSykedag(22.januar, 30)))

        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(3, inspektør.utbetalinger.last().arbeidsgiverOppdrag().size)
        assertEquals(21.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[0].tom)
        assertEquals(30.0, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[1].grad)
        assertEquals(23.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[2].fom)
    }

    @Test
    fun `grad under grensen blir ikke utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(2.januar, 25.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellSykedag(22.januar, 0)))

        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)

        assertEquals(2, inspektør.utbetalinger.last().arbeidsgiverOppdrag().size)
        assertEquals(21.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[0].tom)
        assertEquals(23.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[1].fom)
    }

    @Test
    fun `fridager i midten av en periode blir ikke utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(2.januar, 25.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellFeriedag(22.januar), manuellPermisjonsdag(23.januar)))

        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(2, inspektør.utbetalinger.last().arbeidsgiverOppdrag().size)
        assertEquals(21.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[0].tom)
        assertEquals(24.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[1].fom)
    }

    @Test
    fun `Overstyring oppdaterer sykdomstidlinjene`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellFeriedag(26.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

        assertEquals("SSSHH SSSSSHH SSSSSHH SSSSF", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertEquals("SSSHH SSSSSHH SSSSSHH SSSSF", inspektør.sykdomstidslinje.toShortString())
        assertEquals("PPPPP PPPPPPP PPPPNHH NNNNF", inspektør.utbetalingstidslinjer(1.vedtaksperiode).toString())
    }

    @Test
    fun `Overstyring av sykHelgDag`() {
        håndterSykmelding(Sykmeldingsperiode(17.desember(2017), 31.desember(2017), 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(17.desember(2017), 1.januar)), førsteFraværsdag = 17.desember(2017))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(17.desember(2017), 31.desember(2017), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(10.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(17.desember(2017), 1.januar)), førsteFraværsdag = 10.januar)
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(10.januar, 31.januar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        inspektør.arbeidsgiver.oppdaterSykdom(object : SykdomstidslinjeHendelse(UUID.randomUUID(), LocalDateTime.now()) {
            override fun sykdomstidslinje() = Sykdomstidslinje.ukjent(2.januar, 9.januar, TestEvent.testkilde)
            override fun valider(periode: Periode) = TODO()
            override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = TODO()
            override fun aktørId() = TODO()
            override fun fødselsnummer() = TODO()
            override fun organisasjonsnummer() = TODO()
        })
        håndterOverstyringSykedag(2.januar til 9.januar)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)

        assertEquals("H SSSSSHH SSSSSHH USSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertEquals(" PNNNNHH NNNNNHH NNNNNHH NNNNNHH NNN", inspektør.utbetalingstidslinjer(2.vedtaksperiode).toString())
    }

    private fun håndterOverstyringSykedag(periode: Periode) = håndterOverstyring(periode.map { manuellSykedag(it) })

    private fun manuellPermisjonsdag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Permisjonsdag)
    private fun manuellFeriedag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Feriedag)
    private fun manuellSykedag(dato: LocalDate, grad: Int = 100) = ManuellOverskrivingDag(dato, Dagtype.Sykedag, grad)
    private fun manuellArbeidsgiverdag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Egenmeldingsdag)
}
