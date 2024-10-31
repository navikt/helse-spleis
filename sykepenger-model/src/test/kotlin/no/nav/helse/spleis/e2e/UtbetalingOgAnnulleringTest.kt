package no.nav.helse.spleis.e2e

import java.time.LocalDateTime
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.OVERFØRT
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UtbetalingOgAnnulleringTest : AbstractEndToEndTest() {

    @Test
    fun `annullere senere periode enn perioden til godkjenning`() {
        nyttVedtak(mars)
        tilGodkjenning(januar, ORGNUMMER, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
        håndterUtbetalt()
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `annullere senere periode enn perioden til revurdering`() {
        nyttVedtak(januar)
        nyttVedtak(mars, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(2.vedtaksperiode))
        håndterUtbetalt()
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `annullere førstegangsbehandling uten utbetaling`() {
        nyPeriode(1.januar til 20.januar, grad = 19.prosent)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        håndterAnnullerUtbetaling(ORGNUMMER)
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertEquals(Utbetalingstatus.ANNULLERT, inspektør.utbetaling(1).tilstand)
    }

    @Test
    fun `periode med syk nav strekkes tilbake med foreldrepenger - opphører tidligere utbetaling`() {
        håndterSøknad(1.februar til 2.februar)
        håndterInntektsmelding(emptyList(), 1.februar, begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening",)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(januar, 100)))
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrTidslinje(
            (januar).map { ManuellOverskrivingDag(it, Dagtype.Foreldrepengerdag) } +
            listOf(ManuellOverskrivingDag(1.februar, Dagtype.Sykedag, 100))
        )
        håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(januar, 100)))
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val utbetaling = inspektør.utbetaling(0)
        val revurdering = inspektør.utbetaling(1)
        assertEquals(utbetaling.korrelasjonsId, revurdering.korrelasjonsId)
        assertEquals(1, revurdering.arbeidsgiverOppdrag.size)
        revurdering.arbeidsgiverOppdrag[0].inspektør.also { linje ->
            assertEquals(1.februar til 1.februar, linje.fom til linje.tom)
            assertEquals(Endringskode.ENDR, linje.endringskode)
            assertEquals("OPPH", linje.statuskode)
            assertEquals(1.februar, linje.datoStatusFom)
            assertEquals(1, linje.delytelseId)
            Assertions.assertNull(linje.refDelytelseId)
        }
    }

    @Test
    fun `annullerer første periode før andre periode starter i en ikke-sammenhengende utbetaling med mer enn 16 dager gap`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        val utbetalingId = inspektør.sisteUtbetaling().utbetalingId
        håndterAnnullerUtbetaling(utbetalingId = utbetalingId)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(20.februar, 20.mars))
        håndterSøknad(20.februar til 20.mars)
        håndterInntektsmelding(listOf(Periode(20.februar, 7.mars)), førsteFraværsdag = 20.februar, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertEquals(26.januar, observatør.annulleringer[0].tom)
        assertEquals(20.mars, observatør.utbetalingMedUtbetalingEventer.last().tom)
    }

    @Test
    fun `annullering kaster alle etterfølgende perioder`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(20.februar, 25.mars))
        håndterSøknad(20.februar til 25.mars)
        håndterInntektsmelding(listOf(Periode(20.februar, 7.mars)), førsteFraværsdag = 20.februar, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)

        val utbetalingId = inspektør.sisteUtbetaling().utbetalingId
        håndterAnnullerUtbetaling(utbetalingId = utbetalingId)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            TIL_INFOTRYGD
        )

        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `kan annullere selv om vi har en etterfølgende periode som har gått til infotrygd etter simulering`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(3.mars, 26.mars))
        håndterSøknad(3.mars til 26.mars)
        håndterInntektsmelding(listOf(Periode(3.mars, 18.mars)), vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, false)

        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
        assertEquals(3, inspektør.antallUtbetalinger)
        assertTrue(inspektør.utbetaling(2).erAnnullering)
    }

    @Test
    fun `kan ikke annullere utbetaling etter sammenhengede periode TIL_UTBETALING`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)),)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(27.januar, 15.februar))
        håndterSøknad(27.januar til 15.februar)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(2.vedtaksperiode))
        assertEquals(2, inspektør.antallUtbetalinger)
        assertFalse(inspektør.utbetaling(1).erAnnullering)
    }

    @Test
    fun `utbetaling med teknisk feil blir stående i til utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)),)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.FEIL, sendOverførtKvittering = false)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING
        )
        assertEquals(OVERFØRT, inspektør.utbetalingtilstand(0))
    }

    @Test
    fun `utbetaling med teknisk feil blir stående i til utbetaling og prøver igjen`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)),)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.FEIL, sendOverførtKvittering = false)
        håndterUtbetalingpåminnelse(0, OVERFØRT, LocalDateTime.now().minusDays(1))
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING
        )
        assertEquals(OVERFØRT, inspektør.utbetalingtilstand(0))
    }

    @Test
    fun `utbetaling med teknisk feil for lenge går til Utbetaling feilet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)),)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.FEIL, sendOverførtKvittering = false)
        håndterUtbetalingpåminnelse(0, OVERFØRT, LocalDateTime.now().minusDays(8))
        håndterPåminnelse(1.vedtaksperiode, TIL_UTBETALING)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING
        )
        assertEquals(OVERFØRT, inspektør.utbetalingtilstand(0))
    }

    @Test
    fun `utbetaling som blir avvist går til utbetaling feilet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)),)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AVVIST)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING
        )
    }

    @Test
    fun `kan forsøke utbetaling på nytt etter Utbetaling feilet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)),)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AVVIST)
        assertEquals(OVERFØRT, inspektør.utbetalingtilstand(0))
        håndterUtbetalingpåminnelse(0, OVERFØRT)
        assertEquals(OVERFØRT, inspektør.utbetalingtilstand(0))
        håndterUtbetalt(Oppdragstatus.AKSEPTERT, sendOverførtKvittering = false)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `utbetaling går videre dersom vi går glipp av Overført-kvittering`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)),)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT, sendOverførtKvittering = false)
        assertEquals(Utbetalingstatus.UTBETALT, inspektør.utbetalingtilstand(0))
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Kan ikke annullere over en fastlåst annullering`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)),)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode)) // Stale
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode), opprettet = LocalDateTime.now().plusHours(3))
        håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)

        assertTrue(inspektør.sisteUtbetaling().erAnnullering)
        assertEquals(1, observatør.annulleringer.size)
        assertEquals(2,
            person.personLogg.behov()
                .filter { it.detaljer()["fagsystemId"] == inspektør.sisteArbeidsgiveroppdragFagsystemId(1.vedtaksperiode) }
                .filter { it.type == Aktivitet.Behov.Behovtype.Utbetaling }
                .size
        )
        assertEquals(2, inspektør.antallUtbetalinger)
    }

    @Test
    fun `utbetaling med ubetalt periode etterpå inkluderer ikke dager etter perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(3.januar til 26.januar)
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)),)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        val utbetalingUtbetalingstidslinje = inspektør.utbetalingUtbetalingstidslinje(0)
        assertEquals(3.januar til 26.januar, utbetalingUtbetalingstidslinje.periode())
    }

    @Test
    fun `utbetaling_utbetalt tar med begrunnelse på avviste dager`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar), Sykmeldingsperiode(21.januar, 30.januar))
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), Sykdom(21.januar, 30.januar, 15.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)),)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        val avvisteDager = observatør.utbetalingMedUtbetalingEventer.first().utbetalingsdager.filter { it.type == PersonObserver.Utbetalingsdag.Dagtype.AvvistDag }
        val ikkeAvvisteDager = observatør.utbetalingMedUtbetalingEventer.first().utbetalingsdager.filter { it.type != PersonObserver.Utbetalingsdag.Dagtype.AvvistDag }

        assertEquals(1, observatør.utbetalingMedUtbetalingEventer.size)
        assertEquals(7, avvisteDager.size)
        assertEquals(23, ikkeAvvisteDager.size)
        assertTrue(avvisteDager.all { it.begrunnelser == listOf(PersonObserver.Utbetalingsdag.EksternBegrunnelseDTO.MinimumSykdomsgrad) })
        assertTrue(ikkeAvvisteDager.all { it.begrunnelser == null })
    }
}
