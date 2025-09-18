package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.INNTEKT
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
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
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.utbetalingslinjer.Endringskode.ENDR
import no.nav.helse.utbetalingslinjer.Endringskode.NY
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.OVERFØRT
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.UTBETALT
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtbetalingFeiletE2ETest : AbstractEndToEndTest() {

    @Test
    fun `revurdering feilet med ett oppdrag status avvist som bygger på tidligere`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        nullstillTilstandsendringer()

        this@UtbetalingFeiletE2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
        this@UtbetalingFeiletE2ETest.håndterYtelser(1.vedtaksperiode)
        assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@UtbetalingFeiletE2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(Oppdragstatus.AVVIST)

        håndterUtbetalingpåminnelse(2, OVERFØRT)
        håndterUtbetalt()

        val utbetalingJanuar = inspektør.utbetaling(0)
        assertEquals(1, utbetalingJanuar.arbeidsgiverOppdrag.size)
        utbetalingJanuar.arbeidsgiverOppdrag[0].inspektør.also { linje ->
            assertEquals(NY, linje.endringskode)
            assertEquals(17.januar, linje.fom)
            assertEquals(31.januar, linje.tom)
        }

        val utbetalingFebruar = inspektør.utbetaling(1)
        assertEquals(1, utbetalingFebruar.arbeidsgiverOppdrag.size)
        utbetalingFebruar.arbeidsgiverOppdrag[0].inspektør.also { linje ->
            assertEquals(NY, linje.endringskode)
            assertEquals(1.februar, linje.fom)
            assertEquals(28.februar, linje.tom)
        }

        val utbetalingOverstyringJanuar = inspektør.utbetaling(2)
        assertEquals(1, utbetalingOverstyringJanuar.arbeidsgiverOppdrag.size)
        assertEquals(utbetalingJanuar.arbeidsgiverOppdrag.inspektør.fagsystemId(), utbetalingOverstyringJanuar.arbeidsgiverOppdrag.inspektør.fagsystemId())
        utbetalingOverstyringJanuar.arbeidsgiverOppdrag[0].inspektør.also { linje ->
            assertEquals(ENDR, linje.endringskode)
            assertEquals(17.januar, linje.fom)
            assertEquals(30.januar, linje.tom)
        }

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `utbetaling feilet med ett oppdrag status avvist`() {
        tilGodkjent(januar, 100.prosent)
        håndterUtbetalt(status = Oppdragstatus.AVVIST)
        nullstillTilstandsendringer()

        håndterUtbetalingpåminnelse(0, OVERFØRT)
        håndterUtbetalt()

        assertEquals(1, inspektør.antallUtbetalinger)
        assertEquals(UTBETALT, inspektør.utbetaling(0).tilstand)

        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
    }

    @Test
    fun `utbetaling feilet med ett oppdrag status avvist som bygger på tidligere`() {
        nyttVedtak(januar)
        forlengTilGodkjentVedtak(februar)
        håndterUtbetalt(status = Oppdragstatus.AVVIST)
        nullstillTilstandsendringer()
        håndterUtbetalingpåminnelse(1, OVERFØRT)
        håndterUtbetalt()

        assertEquals(2, inspektør.antallUtbetalinger)
        assertEquals(UTBETALT, inspektør.utbetaling(1).tilstand)

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
    }

    @Test
    fun `utbetaling feilet med ett oppdrag status avvist og ett som er ok`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT / 2, null, emptyList()),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@UtbetalingFeiletE2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@UtbetalingFeiletE2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(
            status = Oppdragstatus.AKSEPTERT,
            fagsystemId = inspektør.utbetaling(0).arbeidsgiverOppdrag.inspektør.fagsystemId()
        )
        håndterUtbetalt(
            status = Oppdragstatus.AVVIST,
            fagsystemId = inspektør.utbetaling(0).personOppdrag.inspektør.fagsystemId()
        )
        nullstillTilstandsendringer()

        håndterUtbetalingpåminnelse(0, OVERFØRT)
        assertEquals(1, hendelselogg.behov.size)
        håndterUtbetalt(
            status = Oppdragstatus.AKSEPTERT,
            fagsystemId = inspektør.utbetaling(0).personOppdrag.inspektør.fagsystemId()
        )

        assertEquals(1, inspektør.antallUtbetalinger)
        assertEquals(UTBETALT, inspektør.utbetaling(0).tilstand)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `utbetaling feilet med ett oppdrag status ok og ett som er avvist`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT / 2, null, emptyList()),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@UtbetalingFeiletE2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@UtbetalingFeiletE2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(
            status = Oppdragstatus.AVVIST,
            fagsystemId = inspektør.utbetaling(0).arbeidsgiverOppdrag.inspektør.fagsystemId()
        )
        håndterUtbetalt(
            status = Oppdragstatus.AKSEPTERT,
            fagsystemId = inspektør.utbetaling(0).personOppdrag.inspektør.fagsystemId()
        )
        nullstillTilstandsendringer()

        håndterUtbetalingpåminnelse(0, OVERFØRT)
        assertEquals(1, hendelselogg.behov.size)
        håndterUtbetalt(
            status = Oppdragstatus.AKSEPTERT,
            fagsystemId = inspektør.utbetaling(0).arbeidsgiverOppdrag.inspektør.fagsystemId()
        )

        assertEquals(1, inspektør.antallUtbetalinger)
        assertEquals(UTBETALT, inspektør.utbetaling(0).tilstand)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `nyere perioder må vente til periode med feilet utbetaling er ok`() {
        nyttVedtak(januar, status = Oppdragstatus.AVVIST)
        nyPeriode(mars)
        håndterArbeidsgiveropplysninger(
            listOf(1.mars til 16.mars),
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode
        )

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }
}
