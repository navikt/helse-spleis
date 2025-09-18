package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.antallEtterspurteBehov
import no.nav.helse.dsl.UgyldigeSituasjonerObservatør.Companion.assertUgyldigSituasjon
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_24
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_ANNULLERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_ANNULLERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.IdInnhenter
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertHarHendelseIder
import no.nav.helse.spleis.e2e.assertHarIkkeHendelseIder
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.forlengPeriode
import no.nav.helse.spleis.e2e.forlengTilGodkjentVedtak
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterAnnullerUtbetaling
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterPåminnelse
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.manuellFeriedag
import no.nav.helse.spleis.e2e.manuellPermisjonsdag
import no.nav.helse.spleis.e2e.manuellSykedag
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.spleis.e2e.tilGodkjent
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RevurderTidslinjeTest : AbstractEndToEndTest() {

    @Test
    fun `revurdere mens en forlengelse er til utbetaling`() {
        nyttVedtak(januar)
        forlengTilGodkjentVedtak(februar)
        nullstillTilstandsendringer()
        this@RevurderTidslinjeTest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING)
        assertNotNull(observatør.avsluttetMedVedtakEvent[2.vedtaksperiode.id(a1)])
    }

    @Test
    fun `revurdere mens en førstegangsbehandling er til utbetaling`() {
        tilGodkjent(januar, 100.prosent)
        nullstillTilstandsendringer()
        this@RevurderTidslinjeTest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertNotNull(observatør.avsluttetMedVedtakEvent[1.vedtaksperiode.id(a1)])
    }

    @Test
    fun `revurdere mens en førstegangsbehandling er til utbetaling - utbetalingen feiler`() {
        tilGodkjent(januar, 100.prosent)
        this@RevurderTidslinjeTest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
        nullstillTilstandsendringer()
        håndterUtbetalt(status = Oppdragstatus.AVVIST)
        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
        assertNull(observatør.avsluttetMedVedtakEvent[1.vedtaksperiode.id(a1)])
    }

    @Test
    fun `revurdere mens en periode har feilet i utbetaling`() {
        nyttVedtak(januar)
        forlengTilGodkjentVedtak(februar)
        håndterUtbetalt(Oppdragstatus.FEIL)
        nullstillTilstandsendringer()
        this@RevurderTidslinjeTest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING)
    }

    @Test
    fun `annullering etter revurdering med utbetaling feilet`() {
        nyttVedtak(3.januar til 26.januar)
        forlengVedtak(27.januar til 26.februar)
        nullstillTilstandsendringer()
        this@RevurderTidslinjeTest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(26.januar, Dagtype.Feriedag)))

        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(Oppdragstatus.AVVIST)

        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        håndterAnnullerUtbetaling(utbetalingId = inspektør.utbetaling(2).utbetalingId)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVVENTER_ANNULLERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_ANNULLERING)
    }

    @Test
    fun `annullering etter revurdering feilet`() {
        nyttVedtak(3.januar til 26.januar)
        forlengVedtak(29.januar til 26.februar)

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(26.januar, Dagtype.Feriedag)))
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertUgyldigSituasjon("En vedtaksperiode i AVVENTER_GODKJENNING_REVURDERING trenger hjelp!") {
            this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false)
        }
        assertVarsler(listOf(RV_UT_23, Varselkode.RV_UT_24), 1.vedtaksperiode.filter())
        nullstillTilstandsendringer()
        håndterAnnullerUtbetaling(utbetalingId = inspektør.utbetaling(2).utbetalingId)
        håndterUtbetalt()
        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_ANNULLERING, TIL_ANNULLERING)
    }

    @Test
    fun `to perioder - revurder dager i eldste`() {
        nyttVedtak(3.januar til 26.januar)
        forlengVedtak(27.januar til 14.februar)
        nullstillTilstandsendringer()

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) })
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        assertIngenFunksjonelleFeil()

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `to perioder - revurder dag i nyeste`() {
        nyttVedtak(3.januar til 26.januar)
        forlengVedtak(27.januar til 14.februar)

        assertEquals(6, inspektør.sisteMaksdato(1.vedtaksperiode).antallForbrukteDager)
        assertEquals(19, inspektør.sisteMaksdato(2.vedtaksperiode).antallForbrukteDager)

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((4.februar til 8.februar).map { manuellFeriedag(it) })
        this@RevurderTidslinjeTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt()

        assertEquals(15, inspektør.sisteMaksdato(2.vedtaksperiode).antallForbrukteDager)

        assertVarsel(RV_UT_23, 2.vedtaksperiode.filter())
        assertIngenFunksjonelleFeil()
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
            AVSLUTTET,
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
        )
    }

    @Test
    fun `to perioder - hele den nyeste perioden blir ferie`() {
        nyttVedtak(3.januar til 26.januar)
        forlengVedtak(27.januar til 14.februar)

        assertEquals(6, inspektør.sisteMaksdato(1.vedtaksperiode).antallForbrukteDager)
        assertEquals(19, inspektør.sisteMaksdato(2.vedtaksperiode).antallForbrukteDager)

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((27.januar til 14.februar).map { manuellFeriedag(it) })
        this@RevurderTidslinjeTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt()

        assertVarsel(RV_UT_23, 2.vedtaksperiode.filter())
        assertEquals(6, inspektør.sisteMaksdato(2.vedtaksperiode).antallForbrukteDager)
        assertIngenFunksjonelleFeil()

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
            AVSLUTTET,
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
        )
    }

    @Test
    fun `kan ikke utvide perioden med sykedager`() {
        nyttVedtak(3.januar til 26.januar)
        nullstillTilstandsendringer()

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((25.januar til 14.februar).map { manuellSykedag(it) })
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVSLUTTET
        )

        assertEquals(Utbetalingstatus.GODKJENT_UTEN_UTBETALING, inspektør.utbetalingtilstand(1))
    }

    @Test
    fun `ledende uferdig periode`() {
        nyttVedtak(3.januar til 26.januar)
        håndterSykmelding(Sykmeldingsperiode(27.januar, 14.februar))
        håndterSøknad(Sykdom(27.januar, 14.februar, 100.prosent))
        this@RevurderTidslinjeTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((19.januar til 22.januar).map { manuellFeriedag(it) })  // ferie på første navdag
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
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
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK
        )
        val revurdering = inspektør.utbetaling(2)
        assertIngenFunksjonelleFeil()
        assertEquals(2, revurdering.arbeidsgiverOppdrag.size)
        assertEquals(19.januar, revurdering.arbeidsgiverOppdrag[0].datoStatusFom)
        assertEquals(23.januar til 26.januar, revurdering.arbeidsgiverOppdrag[1].periode)
    }

    @Test
    fun `ledende uferdig periode som ikke har en utbetaling`() {
        nyttVedtak(3.januar til 26.januar)
        håndterSykmelding(Sykmeldingsperiode(27.januar, 14.februar))
        håndterSøknad(Sykdom(27.januar, 14.februar, 100.prosent))
        nullstillTilstandsendringer()

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((6.januar til 10.januar).map { manuellFeriedag(it) })
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            AVVENTER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK
        )
        assertIngenFunksjonelleFeil()
    }

    @Test
    fun `samme fagsystemId og forskjellig skjæringstidspunkt - revurder første periode i siste skjæringstidspunkt`() {
        nyttVedtak(3.januar til 26.januar)
        nyttVedtak(1.februar til 20.februar, vedtaksperiodeIdInnhenter = 2.vedtaksperiode, arbeidsgiverperiode = emptyList())
        forlengVedtak(21.februar til 10.mars)

        nullstillTilstandsendringer()

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((5.februar til 15.februar).map { manuellFeriedag(it) })
        this@RevurderTidslinjeTest.håndterYtelser(2.vedtaksperiode)
        assertVarsel(RV_UT_23, 2.vedtaksperiode.filter())

        håndterSimulering(2.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        this@RevurderTidslinjeTest.håndterYtelser(3.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(3.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
    }

    @Test
    fun `samme fagsystemId og forskjellig skjæringstidspunkt - revurder siste periode i siste skjæringstidspunkt`() {
        nyttVedtak(3.januar til 26.januar)
        nyttVedtak(1.februar til 20.februar, vedtaksperiodeIdInnhenter = 2.vedtaksperiode, arbeidsgiverperiode = emptyList())
        forlengVedtak(21.februar til 10.mars)

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((22.februar til 25.februar).map { manuellFeriedag(it) })
        this@RevurderTidslinjeTest.håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertVarsel(RV_UT_23, 3.vedtaksperiode.filter())
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
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `forsøk på å revurdere eldre fagsystemId med nyere perioder uten utbetaling, og periode med utbetaling etterpå`() {
        nyttVedtak(3.januar til 26.januar)
        tilGodkjenning(mai, 100.prosent, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((4.januar til 20.januar).map { manuellFeriedag(it) })
        inspektør.sykdomstidslinje.inspektør.also { sykdomstidslinjeInspektør ->
            assertTrue((4.januar til 20.januar).none { sykdomstidslinjeInspektør.dager[it] == Dag.Feriedag::class })
        }

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
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            AVVENTER_BLOKKERENDE_PERIODE
        )
    }

    @Test
    fun `revurderer siste utbetalte periode med bare ferie og permisjon`() {
        nyttVedtak(3.januar til 26.januar)
        nullstillTilstandsendringer()

        assertEquals(6, inspektør.sisteMaksdato(1.vedtaksperiode).antallForbrukteDager)

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((3.januar til 20.januar).map { manuellFeriedag(it) } + (21.januar til 26.januar).map { manuellPermisjonsdag(it) })
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        assertEquals(0, inspektør.sisteMaksdato(1.vedtaksperiode).antallForbrukteDager)
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
        )

        assertIngenFunksjonelleFeil()
    }

    @Test
    fun `revurderer siste utbetalte periode med bare ferie og permisjon - med tidligere utbetaling`() {
        nyttVedtak(3.januar til 26.januar)
        nyttVedtak(3.mars til 26.mars, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        nullstillTilstandsendringer()

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((3.mars til 20.mars).map { manuellFeriedag(it) } + (21.mars til 26.mars).map { manuellPermisjonsdag(it) })
        this@RevurderTidslinjeTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((3.mars til 20.mars).map { manuellSykedag(it) })

        this@RevurderTidslinjeTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        assertVarsel(RV_UT_23, 2.vedtaksperiode.filter())
        assertEquals(4, inspektør.antallUtbetalinger)
        val førsteUtbetaling = inspektør.utbetaling(0)
        val andreUtbetaling = inspektør.utbetaling(1)
        val annulleringenAvMars = inspektør.utbetaling(2)
        val revurderingenAvMars = inspektør.utbetaling(3)

        assertEquals(andreUtbetaling.korrelasjonsId, annulleringenAvMars.korrelasjonsId)
        assertNotEquals(førsteUtbetaling.korrelasjonsId, andreUtbetaling.korrelasjonsId)
        assertEquals(andreUtbetaling.korrelasjonsId, revurderingenAvMars.korrelasjonsId)

        assertEquals(1, annulleringenAvMars.arbeidsgiverOppdrag.size)
        annulleringenAvMars.arbeidsgiverOppdrag[0].inspektør.also { linje ->
            assertEquals(19.mars, linje.fom)
            assertEquals(26.mars, linje.tom)
            assertEquals(19.mars, linje.datoStatusFom)
            assertEquals("OPPH", linje.statuskode)
        }

        assertEquals(1, revurderingenAvMars.arbeidsgiverOppdrag.size)
        assertEquals(Endringskode.ENDR, revurderingenAvMars.arbeidsgiverOppdrag.inspektør.endringskode)
        assertEquals(3.mars til 26.mars, revurderingenAvMars.periode)
        revurderingenAvMars.arbeidsgiverOppdrag[0].inspektør.also { linje ->
            assertEquals(19.mars til 20.mars, linje.fom til linje.tom)
        }
    }

    @Test
    fun `Avslag fører til feilet revurdering`() {
        håndterSykmelding(januar)
        håndterInntektsmelding(
            listOf(Periode(1.januar, 16.januar)),
            førsteFraværsdag = 1.januar
        )
        håndterSøknad(januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje(listOf(manuellFeriedag(18.januar)))
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertUgyldigSituasjon("En vedtaksperiode i AVVENTER_GODKJENNING_REVURDERING trenger hjelp!") {
            this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false)
        }
        assertVarsler(listOf(RV_UT_23, Varselkode.RV_UT_24), 1.vedtaksperiode.filter())
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
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )
    }

    @Test
    fun `Feilet simulering gir warning`() {
        nyttVedtak(januar)

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje(listOf(manuellFeriedag(18.januar)))
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode, simuleringOK = false)
        nullstillTilstandsendringer()

        assertTilstander(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
    }

    @Test
    fun `annullering av feilet revurdering`() {
        håndterSykmelding(januar)
        håndterInntektsmelding(
            listOf(Periode(1.januar, 16.januar)),
            førsteFraværsdag = 1.januar
        )
        håndterSøknad(januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje(listOf(manuellFeriedag(18.januar)))
        this@RevurderTidslinjeTest.håndterYtelser(
            1.vedtaksperiode,
            foreldrepenger = listOf(GradertPeriode(16.januar til 28.januar, 100))
        )
        assertVarsler(listOf(Varselkode.RV_AY_5, RV_UT_23), 1.vedtaksperiode.filter())
        håndterAnnullerUtbetaling()
        håndterUtbetalt()

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
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_ANNULLERING,
            TIL_ANNULLERING,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `påminnet revurdering timer ikke ut`() {
        nyttVedtak(3.januar til 26.januar)

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((16.januar til 26.januar).map { manuellFeriedag(it) })

        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, LocalDateTime.now().minusDays(14))

        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
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
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )
    }

    @Test
    fun `revurder med kun ferie`() {
        nyttVedtak(3.januar til 26.januar)
        forlengVedtak(27.januar til 13.februar)
        forlengPeriode(14.februar til 15.februar)

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((27.januar til 13.februar).map { manuellFeriedag(it) })
        this@RevurderTidslinjeTest.håndterYtelser(2.vedtaksperiode)
        assertVarsel(RV_UT_23, 2.vedtaksperiode.filter())
        håndterSimulering(2.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt()

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK
        )
        assertIngenFunksjonelleFeil()
    }

    @Test
    fun `revurder en revurdering`() {
        nyttVedtak(3.januar til 26.januar)
        nullstillTilstandsendringer()

        assertEquals(6, inspektør.sisteMaksdato(1.vedtaksperiode).antallForbrukteDager)

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) })
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        assertEquals(5, inspektør.sisteMaksdato(1.vedtaksperiode).antallForbrukteDager)

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((23.januar til 23.januar).map { manuellFeriedag(it) })
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        assertEquals(4, inspektør.sisteMaksdato(1.vedtaksperiode).antallForbrukteDager)
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertIngenFunksjonelleFeil()
    }

    @Test
    fun `tre utbetalte perioder - midterste blir revurdert - to siste perioder blir revurdert, første er urørt`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        forlengVedtak(mars)
        nullstillTilstandsendringer()
        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((15.februar til 28.februar).map { manuellFeriedag(it) })

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertIngenFunksjonelleFeil()
    }

    @Test
    fun `tre utbetalte perioder - midterste blir revurdert og utbetalt - to siste perioder blir revurdert, første er urørt`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        forlengVedtak(mars)
        nullstillTilstandsendringer()
        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((15.februar til 28.februar).map { manuellFeriedag(it) })
        this@RevurderTidslinjeTest.håndterYtelser(2.vedtaksperiode)
        assertVarsel(RV_UT_23, 2.vedtaksperiode.filter())
        håndterSimulering(2.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        this@RevurderTidslinjeTest.håndterYtelser(3.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(3.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
        assertIngenFunksjonelleFeil()
        assertEquals(0, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
        assertEquals(0, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        assertEquals(0, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(3.vedtaksperiode).size)
        assertEquals(1, inspektør.avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
        assertEquals(2, inspektør.avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        assertEquals(2, inspektør.avsluttedeUtbetalingerForVedtaksperiode(3.vedtaksperiode).size)
    }

    @Test
    fun `forlengelse samtidig som en aktiv revurdering hvor forlengelsen sin IM flytter skjæringstidspunktet til aktive revurderingen`() {
        nyttVedtak(3.januar til 26.januar)

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((16.januar til 26.januar).map { manuellFeriedag(it) })
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(30.januar, 5.februar))
        assertEquals("SSSHH SSSSSHH SFFFFFF FFFFF", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 30.januar
        )

        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
        assertEquals("UUSSSHH SSSSSHH SFFFFFF FFFFF", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())

        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(30.januar, 5.februar, 100.prosent))

        assertTilstander(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        assertVarsler(listOf(RV_IV_7, RV_UT_23, RV_IM_24), 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(
            2.vedtaksperiode,
            START,
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
    fun `feilet revurdering blokkerer videre behandling`() {
        nyttVedtak(3.januar til 26.januar)

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((16.januar til 26.januar).map { manuellFeriedag(it) })

        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertUgyldigSituasjon("En vedtaksperiode i AVVENTER_GODKJENNING_REVURDERING trenger hjelp!") {
            this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false)
        }
        assertVarsler(listOf(RV_UT_23, Varselkode.RV_UT_24), 1.vedtaksperiode.filter())
        håndterSykmelding(Sykmeldingsperiode(27.januar, 5.februar))
        assertUgyldigSituasjon("En vedtaksperiode i AVVENTER_GODKJENNING_REVURDERING trenger hjelp!") {
            håndterSøknad(Sykdom(27.januar, 5.februar, 100.prosent))
        }

        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `etterspør ytelser ved påminnelser i avventer_historikk_revurdering`() {
        nyttVedtak(januar)
        assertEtterspurteYtelser(1, 1.vedtaksperiode)

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((25.januar til 26.januar).map { manuellFeriedag(it) })
        assertEtterspurteYtelser(2, 1.vedtaksperiode)

        this@RevurderTidslinjeTest.håndterPåminnelse(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertEtterspurteYtelser(3, 1.vedtaksperiode)
    }

    @Test
    fun `Håndter påminnelser i alle tilstandene knyttet til en revurdering med en arbeidsgiver`() {
        nyttVedtak(januar)
        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((25.januar til 26.januar).map { manuellFeriedag(it) })
        this@RevurderTidslinjeTest.håndterPåminnelse(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertEtterspurteYtelser(3, 1.vedtaksperiode)

        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        this@RevurderTidslinjeTest.håndterPåminnelse(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        assertEquals(3, personlogg.antallEtterspurteBehov(1.vedtaksperiode, Aktivitet.Behov.Behovtype.Simulering))

        håndterSimulering(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertEquals(3, personlogg.antallEtterspurteBehov(1.vedtaksperiode, Aktivitet.Behov.Behovtype.Godkjenning))
    }

    @Test
    fun `validering av infotrygdhistorikk i revurdering skal føre til en warning i stedet for en feilet revurdering`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        this@RevurderTidslinjeTest.håndterUtbetalingshistorikkEtterInfotrygdendring(
            ArbeidsgiverUtbetalingsperiode(a1, 17.januar, 31.januar)
        )
        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((20.januar til 26.januar).map { manuellFeriedag(it) })
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)

        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        assertEquals(listOf(1.januar til 16.januar), inspektør.arbeidsgiverperiode(1.vedtaksperiode))

        val utbetaling1 = inspektør.utbetaling(0)
        val revurdering = inspektør.utbetaling(1)

        assertEquals(utbetaling1.korrelasjonsId, revurdering.korrelasjonsId)
        val oppdragInspektør = revurdering.arbeidsgiverOppdrag.inspektør
        assertEquals(Endringskode.ENDR, oppdragInspektør.endringskode)
        // infotrygd-dagene påvirker ikke resultatet. oppdraget kortes ned mht feriedagene
        assertEquals(2, oppdragInspektør.antallLinjer())
        revurdering.arbeidsgiverOppdrag[0].inspektør.also { linje ->
            assertEquals(Endringskode.ENDR, linje.endringskode)
            assertEquals(17.januar til 19.januar, linje.fom til linje.tom)
            assertEquals(null, linje.datoStatusFom)
            assertEquals(null, linje.statuskode)
        }
        revurdering.arbeidsgiverOppdrag[1].inspektør.also { linje ->
            assertEquals(Endringskode.NY, linje.endringskode)
            assertEquals(27.januar til 31.januar, linje.fom til linje.tom)
            assertEquals(null, linje.datoStatusFom)
            assertEquals(null, linje.statuskode)
        }

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        assertVarsel(RV_IT_3, AktivitetsloggFilter.person())
    }

    @Test
    fun `warning dersom det er utbetalt en periode i Infotrygd etter perioden som revurderes nå`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        this@RevurderTidslinjeTest.håndterUtbetalingshistorikkEtterInfotrygdendring(
            ArbeidsgiverUtbetalingsperiode(a1, 1.februar, 28.februar)
        )
        this@RevurderTidslinjeTest.håndterOverstyrTidslinje((20.januar til 26.januar).map { manuellFeriedag(it) })
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        assertVarsler(listOf(RV_IT_1, RV_UT_23), 1.vedtaksperiode.filter())
    }

    @Test
    fun `revurderer siste utbetalte periode`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterInntektsmelding(
            listOf(Periode(2.januar, 17.januar)),
            førsteFraværsdag = 2.januar
        )
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()
        this@RevurderTidslinjeTest.håndterOverstyrTidslinje(listOf(manuellSykedag(26.januar, 80)))
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)

        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())

        håndterSimulering(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        assertEquals(Utbetalingstatus.UTBETALT, inspektør.utbetalingtilstand(0))
        assertEquals(Utbetalingstatus.OVERFØRT, inspektør.utbetalingtilstand(1))
        assertEquals(inspektør.utbetaling(0).arbeidsgiverOppdrag.fagsystemId, inspektør.utbetaling(1).arbeidsgiverOppdrag.fagsystemId)
        inspektør.utbetaling(1).arbeidsgiverOppdrag.also { oppdrag ->
            assertEquals(2, oppdrag.size)
            assertEquals(18.januar, oppdrag[0].fom)
            assertEquals(25.januar, oppdrag[0].tom)
            assertEquals(100, oppdrag[0].grad)

            assertEquals(26.januar, oppdrag[1].fom)
            assertEquals(26.januar, oppdrag[1].tom)
            assertEquals(80, oppdrag[1].grad)
        }
    }

    @Test
    fun `oppdager nye utbetalte dager fra infotrygd i revurderingen`() {
        håndterSykmelding(januar)
        håndterInntektsmelding(
            listOf(Periode(1.januar, 16.januar))
        )
        håndterSøknad(januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        nullstillTilstandsendringer()

        this@RevurderTidslinjeTest.håndterUtbetalingshistorikkEtterInfotrygdendring(
            ArbeidsgiverUtbetalingsperiode(a1, 1.november(2017), 30.november(2017))
        )
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje(overstyringsdager = listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)

        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING
        )
    }

    @Test
    fun `Kun periode berørt av endringene skal ha hendelseIden - forlengelse får hendelseId`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        val hendelseId = UUID.randomUUID()
        this@RevurderTidslinjeTest.håndterOverstyrTidslinje(
            meldingsreferanseId = hendelseId,
            overstyringsdager = (30.januar til 31.januar).map { ManuellOverskrivingDag(it, Dagtype.Feriedag) }
        )
        assertEquals(januar, inspektør.periode(1.vedtaksperiode))
        assertEquals(februar, inspektør.periode(2.vedtaksperiode))
        assertHarHendelseIder(1.vedtaksperiode, hendelseId)
        assertHarIkkeHendelseIder(2.vedtaksperiode, hendelseId)
    }

    @Test
    fun `Kun periode berørt av endringene skal ha hendelseIden`() {
        nyttVedtak(januar)
        nyttVedtak(2.februar til 28.februar, vedtaksperiodeIdInnhenter = 2.vedtaksperiode, arbeidsgiverperiode = emptyList())

        val hendelseId = UUID.randomUUID()
        this@RevurderTidslinjeTest.håndterOverstyrTidslinje(
            meldingsreferanseId = hendelseId,
            overstyringsdager = (30.januar til 31.januar).map { ManuellOverskrivingDag(it, Dagtype.Feriedag) }
        )
        assertEquals(januar, inspektør.periode(1.vedtaksperiode))
        assertEquals(2.februar til 28.februar, inspektør.periode(2.vedtaksperiode))
        assertHarHendelseIder(1.vedtaksperiode, hendelseId)
        assertHarIkkeHendelseIder(2.vedtaksperiode, hendelseId)
    }

    @Test
    fun `revurder første dag i periode på en sykedag som forlenger tidligere arbeidsgiverperiode med nytt skjæringstidspunkt`() {
        nyttVedtak(1.januar til 20.januar)
        håndterSøknad(Sykdom(25.januar, 25.januar, 100.prosent))
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 25.januar
        )
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        this@RevurderTidslinjeTest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(25.januar, Dagtype.Sykedag, 80)))
        this@RevurderTidslinjeTest.håndterYtelser(2.vedtaksperiode)

        assertVarsel(RV_UT_23, 2.vedtaksperiode.filter())
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
        )
    }

    @Test
    fun `Revurdering med nyere IT historikk (frem i tid)`() {
        /*
        Når periode ble revurdert dukker det opp en utbetaling etter vedtaksperioden vi ikke kjente til før. Siden vi har utbetalingsdager i infotrygd prøver
        vi å beregne utbetaling uten å ha lagret en inntekt. Det fører til en error i aktivitetsloggen som igjen gjør at perioden ender opp i RevurderingFeilet
        og speil er i en tilstand hvor ting ikke er oppdatert i saksbildet
         */
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        this@RevurderTidslinjeTest.håndterUtbetalingshistorikkEtterInfotrygdendring(
            ArbeidsgiverUtbetalingsperiode(a1, 1.mars, 31.mars)
        )
        this@RevurderTidslinjeTest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.januar, Dagtype.Sykedag, 80)))
        this@RevurderTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        assertVarsel(RV_IT_1, 1.vedtaksperiode.filter())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
    }

    private fun assertEtterspurteYtelser(expected: Int, vedtaksperiodeIdInnhenter: IdInnhenter) {
        assertEquals(expected, personlogg.antallEtterspurteBehov(vedtaksperiodeIdInnhenter, Aktivitet.Behov.Behovtype.Foreldrepenger))
        assertEquals(expected, personlogg.antallEtterspurteBehov(vedtaksperiodeIdInnhenter, Aktivitet.Behov.Behovtype.Pleiepenger))
        assertEquals(expected, personlogg.antallEtterspurteBehov(vedtaksperiodeIdInnhenter, Aktivitet.Behov.Behovtype.Omsorgspenger))
        assertEquals(expected, personlogg.antallEtterspurteBehov(vedtaksperiodeIdInnhenter, Aktivitet.Behov.Behovtype.Opplæringspenger))
        assertEquals(expected, personlogg.antallEtterspurteBehov(vedtaksperiodeIdInnhenter, Aktivitet.Behov.Behovtype.Arbeidsavklaringspenger))
        assertEquals(expected, personlogg.antallEtterspurteBehov(vedtaksperiodeIdInnhenter, Aktivitet.Behov.Behovtype.Dagpenger))
        assertEquals(expected, personlogg.antallEtterspurteBehov(vedtaksperiodeIdInnhenter, Aktivitet.Behov.Behovtype.Institusjonsopphold))
    }
}
