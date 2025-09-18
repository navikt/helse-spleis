package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.dsl.UgyldigeSituasjonerObservatør.Companion.assertUgyldigSituasjon
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_5
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_4
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.IdInnhenter
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.forlengTilGodkjenning
import no.nav.helse.spleis.e2e.forlengTilSimulering
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.spleis.e2e.tilGodkjent
import no.nav.helse.spleis.e2e.tilSimulering
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RevurderingV2E2ETest : AbstractEndToEndTest() {

    @Test
    fun `revurdere første periode`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        forlengVedtak(mars)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(1.vedtaksperiode, 19.januar, 1431.0)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(1.vedtaksperiode, 19.januar, 0.0)
        assertDiff(-1431)
        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurdere periode med forlengelse i avventer godkjenning`() {
        nyttVedtak(januar)
        forlengTilGodkjenning(februar)
        nullstillTilstandsendringer()

        assertDag<Sykedag, NavDag>(1.vedtaksperiode, 17.januar, 1431.0)

        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)

        assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(1.vedtaksperiode, 17.januar, 0.0)
        assertDiff(-1431)
        val utbetalinger = inspektør.utbetalinger(2.vedtaksperiode)
        assertEquals(1, utbetalinger.size)
        assertEquals(Utbetalingstatus.FORKASTET, utbetalinger.single().inspektør.tilstand)

        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `revurdere andre periode`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        forlengVedtak(mars)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(2.vedtaksperiode, 5.februar, 1431.0)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(2.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(2.vedtaksperiode, 5.februar, 0.0)
        assertDiff(-1431)
        assertVarsel(RV_UT_23, 2.vedtaksperiode.filter())
        håndterSimulering(2.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `revurdere tredje periode`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        forlengVedtak(mars)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(3.vedtaksperiode, 5.mars, 1431.0)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.mars, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(3.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(3.vedtaksperiode, 5.mars, 0.0)
        assertDiff(-1431)
        assertVarsel(RV_UT_23, 3.vedtaksperiode.filter())
        håndterSimulering(3.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
    }

    @Test
    fun `revurdere første to perioder`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        forlengVedtak(mars)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(1.vedtaksperiode, 31.januar, 1431.0)
        assertDag<Sykedag, NavDag>(2.vedtaksperiode, 1.februar, 1431.0)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(1.februar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(1.vedtaksperiode, 31.januar, 0.0)
        assertDag<Dag.Feriedag, Utbetalingsdag.UkjentDag>(2.vedtaksperiode, 1.februar, null, null)
        assertDiff(-1431)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `en ny forlengelse på en aktiv revurdering`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        nyPeriode(mars)
        assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
        assertVarsler(emptyList(), 2.vedtaksperiode.filter())
        assertVarsler(emptyList(), 3.vedtaksperiode.filter())
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `revurdere dager i arbeidsgiverperioden på tidligere utbetaling`() {
        nyttVedtak(januar)
        assertDag<Sykedag, ArbeidsgiverperiodeDag>(1.vedtaksperiode, 5.januar, 0.0)
        nyttVedtak(mars, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, ArbeidsgiverperiodeDag>(1.vedtaksperiode, 5.januar, 0.0)
        assertDiff(0)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurdere periode med nyere førstegangsbehandling innenfor samme agp`() {
        nyttVedtak(januar)
        nyttVedtak(10.februar til 28.februar, arbeidsgiverperiode = emptyList(), vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        nullstillTilstandsendringer()
        assertDag<Sykedag, ArbeidsgiverperiodeDag>(1.vedtaksperiode, 5.januar, 0.0)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, ArbeidsgiverperiodeDag>(1.vedtaksperiode, 5.januar, 0.0)
        assertDiff(0)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)

        assertEquals(3, inspektør.antallUtbetalinger)
        inspektør.utbetaling(2).also { revurdering ->
            val januarutbetaling = inspektør.utbetaling(0)
            val februarutbetaling = inspektør.utbetaling(1)
            assertEquals(revurdering.korrelasjonsId, januarutbetaling.korrelasjonsId)
            assertEquals("PPPPPPP PPPPPPP PPNNNHH NNNNNHH NNN", inspektør.utbetalingstidslinjer(1.vedtaksperiode).toString().trim())
            assertEquals(januar, januarutbetaling.periode)
            assertEquals(10.februar til 28.februar, februarutbetaling.periode)
            assertEquals(januar, revurdering.periode)
            assertEquals(0, revurdering.personOppdrag.size)
            assertEquals(1, revurdering.arbeidsgiverOppdrag.size)
            assertEquals(Endringskode.UEND, revurdering.arbeidsgiverOppdrag.inspektør.endringskode)
            assertEquals(0, revurdering.arbeidsgiverOppdrag.inspektør.nettoBeløp)
            revurdering.arbeidsgiverOppdrag.inspektør.also { arbeidsgiveroppdrag ->
                assertEquals(17.januar, arbeidsgiveroppdrag.fom(0))
                assertEquals(31.januar, arbeidsgiveroppdrag.tom(0))
                assertEquals(1431, arbeidsgiveroppdrag.beløp(0))
                assertEquals(Endringskode.UEND, arbeidsgiveroppdrag.endringskode(0))
            }
        }
    }

    @Test
    fun `starte revurdering av eldre skjæringstidspunkt, så revurdere nyere`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        nyttVedtak(april, vedtaksperiodeIdInnhenter = 3.vedtaksperiode)
        nullstillTilstandsendringer()

        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)

        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(1.vedtaksperiode, 31.januar, 0.0)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)

        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(10.februar, Feriedag)))

        assertDag<Dag.Feriedag, Utbetalingsdag.UkjentDag>(1.vedtaksperiode, 10.februar, null, null)
        assertTilstander(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING)

        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.april, Feriedag)))

        assertDag<Dag.Feriedag, Utbetalingsdag.UkjentDag>(1.vedtaksperiode, 20.april, null, null)
        assertTilstander(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurdere nyere skjæringstidspunkt så revurdere eldste`() {
        nyttVedtak(januar)
        nyttVedtak(10.februar til 28.februar, arbeidsgiverperiode = emptyList(), vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        forlengVedtak(mars)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(1.vedtaksperiode, 19.januar, 1431.0)
        assertDag<Sykedag, NavDag>(2.vedtaksperiode, 15.februar, 1431.0)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(15.februar, Feriedag)))
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(1.vedtaksperiode, 19.januar, 0.0)
        assertDiff(-1431)
        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        this@RevurderingV2E2ETest.håndterYtelser(2.vedtaksperiode)
        assertVarsel(RV_UT_23, 2.vedtaksperiode.filter())
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(2.vedtaksperiode, 15.februar, 0.0)
        assertDiff(-1431)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurdere nyere arbeidsgiverperiode så revurdere eldste`() {
        nyttVedtak(januar)
        assertDag<Sykedag, NavDag>(1.vedtaksperiode, 19.januar, 1431.0)
        nyttVedtak(mars, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        assertDag<Sykedag, NavDag>(2.vedtaksperiode, 19.mars, 1431.0)
        forlengVedtak(april)
        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.mars, Feriedag)))
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(1.vedtaksperiode, 19.januar, 0.0)
        assertDiff(-1431)
        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        this@RevurderingV2E2ETest.håndterYtelser(2.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(2.vedtaksperiode, 19.mars, 0.0)
        assertDiff(-1431)
        assertVarsler(listOf(RV_UT_23), 2.vedtaksperiode.filter())

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurdere eldre skjæringstidspunkt mens nyere revurdert skjæringstidspunkt utbetales`() {
        nyttVedtak(1.januar til 20.januar)
        forlengVedtak(21.januar til 31.januar)
        nyttVedtak(10.februar til 28.februar, arbeidsgiverperiode = emptyList(), vedtaksperiodeIdInnhenter = 3.vedtaksperiode)
        forlengVedtak(mars)
        nullstillTilstandsendringer()

        assertDag<Sykedag, NavDag>(3.vedtaksperiode, 15.februar, 1431.0)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(15.februar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(3.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(3.vedtaksperiode, 15.februar, 0.0)
        assertDiff(-1431)
        assertVarsel(RV_UT_23, 3.vedtaksperiode.filter())
        håndterSimulering(3.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(3.vedtaksperiode)

        assertEquals(5, inspektør.antallUtbetalinger)
        inspektør.utbetaling(4).also { revurdering ->
            val januar1utbetaling = inspektør.utbetaling(0)
            val januar2utbetaling = inspektør.utbetaling(1)
            val februarutbetaling = inspektør.utbetaling(2)
            val marsutbetaling = inspektør.utbetaling(3)
            assertEquals(revurdering.korrelasjonsId, februarutbetaling.korrelasjonsId)
            assertEquals("PPPPPPP PPPPPPP PPNNNH", inspektør.utbetalingstidslinjer(1.vedtaksperiode).toString().trim())
            assertEquals("H NNNNNHH NNN", inspektør.utbetalingstidslinjer(2.vedtaksperiode).toString().trim())
            assertEquals("HH NNNFNHH NNNNNHH NNN", inspektør.utbetalingstidslinjer(3.vedtaksperiode).toString().trim())
            assertEquals(1.januar til 20.januar, januar1utbetaling.periode)
            assertEquals(21.januar til 31.januar, januar2utbetaling.periode)
            assertEquals(10.februar til 28.februar, februarutbetaling.periode)
            assertEquals(1.mars til 31.mars, marsutbetaling.periode)
            assertEquals(10.februar til 28.februar, revurdering.periode)
            assertEquals(0, revurdering.personOppdrag.size)
            assertEquals(2, revurdering.arbeidsgiverOppdrag.size)
            assertEquals(Endringskode.ENDR, revurdering.arbeidsgiverOppdrag.inspektør.endringskode)
            assertEquals(-1431, revurdering.arbeidsgiverOppdrag.inspektør.nettoBeløp)
            revurdering.arbeidsgiverOppdrag.inspektør.also { arbeidsgiveroppdrag ->
                assertEquals(10.februar, arbeidsgiveroppdrag.fom(0))
                assertEquals(14.februar, arbeidsgiveroppdrag.tom(0))
                assertEquals(1431, arbeidsgiveroppdrag.beløp(0))
                assertEquals(Endringskode.ENDR, arbeidsgiveroppdrag.endringskode(0))

                assertEquals(16.februar, arbeidsgiveroppdrag.fom(1))
                assertEquals(28.februar, arbeidsgiveroppdrag.tom(1))
                assertEquals(1431, arbeidsgiveroppdrag.beløp(1))
                assertEquals(Endringskode.NY, arbeidsgiveroppdrag.endringskode(1))
            }
        }

        assertDag<Sykedag, NavDag>(1.vedtaksperiode, 19.januar, 1431.0)
        assertDag<SykHelgedag, NavHelgDag>(2.vedtaksperiode, 21.januar, 0.0)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(21.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVVENTER_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)

        nullstillTilstandsendringer()
        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVVENTER_REVURDERING)

        assertNotNull(observatør.avsluttetMedVedtakEvent[4.vedtaksperiode.id(a1)])

        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)

        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(1.vedtaksperiode, 19.januar, 0.0)
        assertDag<Dag.Feriedag, Utbetalingsdag.UkjentDag>(2.vedtaksperiode, 21.januar, null, null)
        assertDiff(-1431)

        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurdering av eldre skjæringstidspunkt mens nyere skjæringstidspunkt utbetales`() {
        nyttVedtak(januar)
        tilGodkjent(mars, 100.prosent, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING)

        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING)

        assertNotNull(observatør.avsluttetMedVedtakEvent[2.vedtaksperiode.id(a1)])
    }

    @Test
    fun `Periode med forlengelse - forlengelse blir revurdert, deretter revurdering på første`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)))
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING)
    }

    @Test
    fun `Periode med to forlengelser - forlengelse nummer en blir revurdert, deretter revurdering på første`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        forlengVedtak(mars)
        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)))
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `periode med forlengelse etterfulgt av kort periode - kort periode avsluttes ikke før revurdering er ferdig`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        håndterSykmelding(Sykmeldingsperiode(1.april, 16.april))
        håndterSøknad(Sykdom(1.april, 16.april, 100.prosent))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `revurdere eldste skjæringstidspunkt så revurdere nyeste`() {
        nyttVedtak(januar)
        nyttVedtak(10.februar til 28.februar, arbeidsgiverperiode = emptyList(), vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        forlengVedtak(mars)
        nullstillTilstandsendringer()

        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)

        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(15.februar, Feriedag)))
        assertTrue(inspektør.sykdomstidslinje[15.februar] is Dag.Feriedag)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `nyere revurdering til simulering, så revurdering på eldre`() {
        nyttVedtak(januar)
        nyttVedtak(mars, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(2.vedtaksperiode, 20.mars, 1431.0)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.mars, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(2.vedtaksperiode)
        assertVarsel(RV_UT_23, 2.vedtaksperiode.filter())
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(2.vedtaksperiode, 20.mars, 0.0)
        assertDiff(-1431)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_REVURDERING)
    }

    @Test
    fun `nyere revurdering til godkjenning, så revurdering på eldre`() {
        nyttVedtak(januar)
        nyttVedtak(mars, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(2.vedtaksperiode, 20.mars, 1431.0)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.mars, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(2.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(2.vedtaksperiode, 20.mars, 0.0)
        assertDiff(-1431)
        assertVarsel(RV_UT_23, 2.vedtaksperiode.filter())
        håndterSimulering(2.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING)
    }

    @Test
    fun `Periode med forlengelse - forlengelse blir revurdert og er til godkjenning, deretter revurdering på første`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(2.vedtaksperiode, 5.februar, 1431.0)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(2.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(2.vedtaksperiode, 5.februar, 0.0)
        assertDiff(-1431)
        assertVarsel(RV_UT_23, 2.vedtaksperiode.filter())
        håndterSimulering(2.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING)
    }

    @Test
    fun `revudering påvirkes ikke av gjenoppta behandling ved avsluttet uten utbetaling`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        assertDag<Sykedag, NavDag>(1.vedtaksperiode, 17.januar, 1431.0)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(1.vedtaksperiode, 17.januar, 0.0)
        assertDiff(-1431)
        assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        nullstillTilstandsendringer()

        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        håndterSykmelding(Sykmeldingsperiode(1.juni, 10.juni))
        håndterSøknad(Sykdom(1.juni, 10.juni, 100.prosent))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `periode til utbetaling blir overstyrt`() {
        tilGodkjent(januar, 100.prosent)
        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING)
        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertNotNull(observatør.avsluttetMedVedtakEvent[1.vedtaksperiode.id(a1)])
    }

    @Test
    fun `revurdert periode til utbetaling blir revurdert`() {
        nyttVedtak(januar)
        assertDag<Sykedag, NavDag>(1.vedtaksperiode, 17.januar, 1431.0)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(1.vedtaksperiode, 17.januar, 0.0)
        assertDiff(-1431)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING)
        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertNotNull(observatør.avsluttetMedVedtakEvent[1.vedtaksperiode.id(a1)])
    }

    @Test
    fun `revurdering på tidligere skjæringstidspunkt mens nyere førstegangsbehandling står i avventer simulering`() {
        nyttVedtak(januar)
        assertDag<Sykedag, NavDag>(1.vedtaksperiode, 17.januar, 1431.0)
        tilSimulering(mars, 100.prosent, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(1.vedtaksperiode, 17.januar, 0.0)
        assertDiff(-1431)
        assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `revurdering på tidligere skjæringstidspunkt mens nyere førstegangsbehandling står i avventer godkjenning`() {
        nyttVedtak(januar)
        assertDag<Sykedag, NavDag>(1.vedtaksperiode, 17.januar, 1431.0)
        tilGodkjenning(mars, 100.prosent, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(1.vedtaksperiode, 17.januar, 0.0)
        assertDiff(-1431)
        assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `revurdering på tidligere skjæringstidspunkt mens nyere forlengelse står i avventer simulering`() {
        nyttVedtak(januar)
        assertDag<Sykedag, NavDag>(1.vedtaksperiode, 17.januar, 1431.0)
        nyttVedtak(mars, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        forlengTilSimulering(april, 100.prosent)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(1.vedtaksperiode, 17.januar, 0.0)
        assertDiff(-1431)
        assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        this@RevurderingV2E2ETest.håndterYtelser(2.vedtaksperiode)
        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `revurdering på tidligere skjæringstidspunkt mens nyere forlengelse står i avventer godkjenning`() {
        nyttVedtak(januar)
        assertDag<Sykedag, NavDag>(1.vedtaksperiode, 17.januar, 1431.0)
        nyttVedtak(mars, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        forlengTilGodkjenning(april, 100.prosent)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(1.vedtaksperiode, 17.januar, 0.0)
        assertDiff(-1431)
        assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        this@RevurderingV2E2ETest.håndterYtelser(2.vedtaksperiode)
        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `revurdering på tidligere skjæringstidspunkt mens nyere revurdering står i avventer simulering revurdering`() {
        nyttVedtak(januar)
        nyttVedtak(mars, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)

        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(2.vedtaksperiode, 19.mars, 1431.0)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.mars, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(2.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(2.vedtaksperiode, 19.mars, 0.0)
        assertDiff(-1431)
        assertVarsler(listOf(RV_UT_23), 2.vedtaksperiode.filter())
        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)

        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurdering på tidligere skjæringstidspunkt mens nyere revurdering står i avventer godkjenning revurdering`() {
        nyttVedtak(januar)
        nyttVedtak(mars, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)

        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(2.vedtaksperiode, 19.mars, 1431.0)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.mars, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(2.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(2.vedtaksperiode, 19.mars, 0.0)
        assertDiff(-1431)
        assertVarsler(listOf(RV_UT_23), 2.vedtaksperiode.filter())
        håndterSimulering(2.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)

        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING)
    }

    @Test
    fun `fire skjæringstidspunkter der første og siste blir revurdert - kun skjæringstidspunkter med endring i utbetaling skal utbetales`() {
        nyttVedtak(januar)
        assertDag<Sykedag, NavDag>(1.vedtaksperiode, 17.januar, 1431.0)
        nyttVedtak(mars, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        nyttVedtak(mai, vedtaksperiodeIdInnhenter = 3.vedtaksperiode)
        nyttVedtak(juli, vedtaksperiodeIdInnhenter = 4.vedtaksperiode)

        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(4.vedtaksperiode, 17.juli, 1431.0)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)

        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.juli, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVVENTER_REVURDERING)

        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(1.vedtaksperiode, 17.januar, 0.0)
        assertDiff(-1431)
        assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVVENTER_REVURDERING)

        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterYtelser(2.vedtaksperiode)
        assertDiff(0)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVVENTER_REVURDERING)

        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterYtelser(3.vedtaksperiode)
        assertDiff(0)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
        assertTilstander(4.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)

        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterYtelser(4.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(4.vedtaksperiode, 17.juli, 0.0)
        assertDiff(-1431)
        assertVarsler(listOf(RV_UT_23), 4.vedtaksperiode.filter())
        håndterSimulering(4.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(4.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVSLUTTET)
        assertTilstander(4.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
    }

    @Test
    fun `fire skjæringstidspunkter der første og siste blir revurdert - kun skjæringstidspunkter med endring i utbetaling skal utbetales - første skjæringstidspunkt har forlengelse`() {
        nyttVedtak(1.desember(2017) til 31.desember(2017))
        assertDag<Sykedag, NavDag>(1.vedtaksperiode, 19.desember(2017), 1431.0)
        forlengVedtak(januar)
        nyttVedtak(mars, vedtaksperiodeIdInnhenter = 3.vedtaksperiode)
        nyttVedtak(mai, vedtaksperiodeIdInnhenter = 4.vedtaksperiode)
        nyttVedtak(juli, vedtaksperiodeIdInnhenter = 5.vedtaksperiode)

        assertDag<Sykedag, NavDag>(5.vedtaksperiode, 17.juli, 1431.0)
        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.desember(2017), Feriedag)))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(5.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)

        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.juli, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVVENTER_REVURDERING)
        assertTilstander(5.vedtaksperiode, AVVENTER_REVURDERING)

        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(1.vedtaksperiode, 19.desember(2017), 0.0)
        assertDiff(-1431)
        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVVENTER_REVURDERING)
        assertTilstander(5.vedtaksperiode, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurdering der vi har vært innom Infotrygd`() {
        createPingPongPerson()

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET)

        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `avslår revurdering uten utbetaling - som tidligere har vært utbetalt`() {
        nyttVedtak(januar)
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), Ferie(17.januar, 31.januar))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Permisjonsdag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        assertUgyldigSituasjon("En vedtaksperiode i AVVENTER_GODKJENNING_REVURDERING trenger hjelp!") {
            this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false)
        }
        assertVarsler(listOf(RV_UT_23, Varselkode.RV_UT_24), 1.vedtaksperiode.filter())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertTrue(inspektør.periodeErIkkeForkastet(1.vedtaksperiode))
    }

    @Test
    fun `revurdering uten utbetaling - som tidligere har vært utbetalt`() {
        nyttVedtak(januar)
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), Ferie(17.januar, 31.januar))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Permisjonsdag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        val revurdering1 = inspektør.utbetaling(1)
        val revurdering2 = inspektør.utbetaling(2)
        assertEquals(revurdering1.korrelasjonsId, revurdering2.korrelasjonsId)
        revurdering1.also { utbetalingInspektør ->
            assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
            assertEquals(0, utbetalingInspektør.personOppdrag.size)
            utbetalingInspektør.arbeidsgiverOppdrag.also { oppdrag ->
                assertEquals(Endringskode.ENDR, oppdrag.inspektør.endringskode)
                oppdrag[0].inspektør.also { linjeInspektør ->
                    assertEquals(Endringskode.ENDR, linjeInspektør.endringskode)
                    assertEquals(17.januar til 31.januar, linjeInspektør.periode)
                    assertEquals(17.januar, linjeInspektør.datoStatusFom)
                }
            }
        }
        revurdering2.also { utbetalingInspektør ->
            assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
            assertEquals(0, utbetalingInspektør.personOppdrag.size)
            utbetalingInspektør.arbeidsgiverOppdrag.also { oppdrag ->
                assertEquals(Endringskode.UEND, oppdrag.inspektør.endringskode)
                oppdrag[0].inspektør.also { linjeInspektør ->
                    assertEquals(Endringskode.UEND, linjeInspektør.endringskode)
                    assertEquals(17.januar til 31.januar, linjeInspektør.periode)
                    assertEquals(17.januar, linjeInspektør.datoStatusFom)
                }
            }
        }
    }

    @Test
    fun `revurdere nyeste periode når vi har vært innom Infotrygd, deretter eldste periode`() {
        createPingPongPerson()

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET)

        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.mars, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)

        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurdere frem og tilbake mellom feriedag og sykedag hvor første overstyring blir utbetalt`() {
        nyttVedtak(januar)
        assertDag<Sykedag, NavDag>(1.vedtaksperiode, 17.januar, 1431.0)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(1.vedtaksperiode, 17.januar, 0.0)
        assertDiff(-1431)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Sykedag, 100)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertDag<Sykedag, NavDag>(1.vedtaksperiode, 17.januar, 1431.0)
        assertDiff(1431)
    }

    @Test
    fun `overstyr utkast til revurdering - tidslinje`() {
        nyttVedtak(januar)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
    }

    @Test
    fun `overstyr utkast til revurdering - med flere perioder`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        nullstillTilstandsendringer()
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.februar, Feriedag)))
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
    }

    @Test
    fun `overlappende ytelser ved revurdering skal gi warning, ikke error`() {
        nyttVedtak(januar)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(1.januar til 10.januar, 100)))

        assertVarsler(listOf(RV_AY_5, RV_UT_23), 1.vedtaksperiode.filter())
        assertIngenFunksjonelleFeil()
    }

    @Test
    fun `valider ytelser kun for de periodene som påvirkes av revurderingen`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        forlengVedtak(mars)

        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(2.februar, Feriedag)))
        this@RevurderingV2E2ETest.håndterYtelser(2.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(20.januar til 31.januar, 100)))

        assertTilstand(1.vedtaksperiode, AVSLUTTET)
        assertTilstand(2.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        assertTilstand(3.vedtaksperiode, AVVENTER_REVURDERING)

        assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        assertVarsler(listOf(RV_UT_23), 2.vedtaksperiode.filter())
    }

    @Test
    fun `Alle perioder som er en del av revurdering skal kunne avvise dager`() {
        nyttVedtak(januar)
        forlengVedtak(februar)

        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Sykedag, 19)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(16.februar, Dagtype.Sykedag, 19)))
        this@RevurderingV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        this@RevurderingV2E2ETest.håndterYtelser(2.vedtaksperiode)

        assertVarsler(listOf(RV_VV_4, RV_UT_23), 1.vedtaksperiode.filter())
        assertVarsler(listOf(RV_VV_4, RV_UT_23), 2.vedtaksperiode.filter())
        assertEquals(19, inspektør.sykdomstidslinje.inspektør.grader[17.januar])
        assertEquals(19, inspektør.sykdomstidslinje.inspektør.grader[16.februar])
        inspektør.utbetalingstidslinjer(1.vedtaksperiode)[17.januar].økonomi.inspektør.also { økonomi ->
            assertEquals(19, økonomi.totalGrad)
        }
        inspektør.utbetalingstidslinjer(2.vedtaksperiode)[16.februar].økonomi.inspektør.also { økonomi ->
            assertEquals(19, økonomi.totalGrad)
        }
    }

    @Test
    fun `maksdato hensyntar ikke fremtidige perioder ved revurdering`() {
        nyttVedtak(januar)
        nyttVedtak(mars, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)

        inspektør.sisteMaksdato(1.vedtaksperiode).also {
            assertEquals(11, it.antallForbrukteDager)
            assertEquals(237, it.gjenståendeDager)
            assertEquals(28.desember, it.maksdato)
        }
        this@RevurderingV2E2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Sykedag, 50)))
        this@RevurderingV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        inspektør.sisteMaksdato(1.vedtaksperiode).also {
            assertEquals(11, it.antallForbrukteDager)
            assertEquals(237, it.gjenståendeDager)
            assertEquals(28.desember, it.maksdato)
        }
    }

    private inline fun <reified D : Dag, reified UD : Utbetalingsdag> assertDag(vedtaksperiodeId: IdInnhenter, dato: LocalDate, arbeidsgiverbeløp: Double?, personbeløp: Double? = 0.0) {
        inspektør.sykdomshistorikk.sykdomstidslinje()[dato].let {
            assertTrue(it is D) { "Forventet at $dato er ${D::class.simpleName} men var ${it::class.simpleName}" }
        }
        inspektør.utbetalingstidslinjer(vedtaksperiodeId)[dato].let {
            assertTrue(it is UD) { "Forventet at $dato er ${UD::class.simpleName} men var ${it::class.simpleName}" }
            assertEquals(arbeidsgiverbeløp?.daglig, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(personbeløp?.daglig, it.økonomi.inspektør.personbeløp)
        }
    }

    private fun assertDiff(diff: Int) {
        assertEquals(diff, inspektør.sisteUtbetaling().nettobeløp)
    }
}
