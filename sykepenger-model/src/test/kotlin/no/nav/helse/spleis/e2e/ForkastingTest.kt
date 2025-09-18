package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.UgyldigeSituasjonerObservatør.Companion.assertUgyldigSituasjon
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Papirsykmelding
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
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
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ForkastingTest : AbstractEndToEndTest() {

    @Test
    fun `forlengelse av infotrygd uten inntektsopplysninger`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar))
        this@ForkastingTest.håndterUtbetalingshistorikkEtterInfotrygdendring(
            ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar)
        )
        håndterSøknad(1.februar til 23.februar)
        assertEquals(0, inspektør.antallUtbetalinger)
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
    }

    @Test
    fun `når utbetaling er ikke godkjent skal påfølgende perioder også kastes ut`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterArbeidsgiveropplysninger(listOf(Periode(3.januar, 18.januar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@ForkastingTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar))
        håndterSøknad(29.januar til 23.februar)
        this@ForkastingTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, false)
        assertEquals(Utbetalingstatus.IKKE_GODKJENT, inspektør.utbetalingtilstand(0))
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
            TIL_INFOTRYGD
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `når utbetaling er ikke godkjent skal påfølgende perioder også kastes ut -- alternativ syntax`() {
        hendelsene {
            håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
            håndterSøknad(3.januar til 26.januar)
        } førerTil AVVENTER_INNTEKTSMELDING somEtterfulgtAv {
            håndterArbeidsgiveropplysninger(
                listOf(Periode(3.januar, 18.januar)),
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode
            )
        } førerTil AVVENTER_VILKÅRSPRØVING somEtterfulgtAv {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
        } førerTil AVVENTER_HISTORIKK somEtterfulgtAv {
            this@ForkastingTest.håndterYtelser(1.vedtaksperiode)
        } førerTil AVVENTER_SIMULERING somEtterfulgtAv {
            håndterSimulering(1.vedtaksperiode)
        } førerTil AVVENTER_GODKJENNING somEtterfulgtAv {
            håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar))
            håndterSøknad(29.januar til 23.februar)
        } førerTil listOf(AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE) somEtterfulgtAv {
            this@ForkastingTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, false)
        } førerTil listOf(TIL_INFOTRYGD, TIL_INFOTRYGD)
        assertEquals(Utbetalingstatus.IKKE_GODKJENT, inspektør.utbetalingtilstand(0))
    }

    @Test
    fun `kan ikke forlenge en periode som er gått TilInfotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar))
        håndterSøknad(29.januar til 23.februar)
        håndterArbeidsgiveropplysninger(listOf(Periode(3.januar, 18.januar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@ForkastingTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@ForkastingTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, false) // går til TilInfotrygd

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
            TIL_INFOTRYGD
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `søknad med papirsykmelding`() {
        håndterSykmelding(Sykmeldingsperiode(21.januar, 28.februar))
        håndterSøknad(
            Sykdom(1.februar, 28.februar, 100.prosent),
            Papirsykmelding(1.januar, 20.januar)
        )
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `refusjon opphører i perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterArbeidsgiveropplysninger(
            listOf(Periode(3.januar, 18.januar)),
            refusjon = Refusjon(INNTEKT, 20.januar, emptyList()),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@ForkastingTest.håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `refusjon endres i perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar)),
            refusjon = Refusjon(INNTEKT, null, listOf(Refusjon.EndringIRefusjon(INNTEKT / 2, 14.januar))),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@ForkastingTest.håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `forkaster ikke i til utbetaling ved overlapp`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterArbeidsgiveropplysninger(listOf(Periode(3.januar, 18.januar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@ForkastingTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@ForkastingTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

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
    fun `forkaster i avventer godkjenning ved overlapp`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterArbeidsgiveropplysninger(listOf(Periode(3.januar, 18.januar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@ForkastingTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(3.januar, 27.januar))
        håndterSøknad(3.januar til 27.januar)

        assertEquals(Utbetalingstatus.FORKASTET, inspektør.utbetalingtilstand(0))
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
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `forkaster ikke revurderinger - avventer simulering revurdering`() {
        nyttVedtak(3.januar til 26.januar)
        nullstillTilstandsendringer()
        this@ForkastingTest.håndterOverstyrTidslinje(
            listOf(
                ManuellOverskrivingDag(26.januar, Dagtype.Feriedag)
            )
        )
        this@ForkastingTest.håndterYtelser(1.vedtaksperiode)
        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        this@ForkastingTest.håndterAnmodningOmForkasting(1.vedtaksperiode)
        assertEquals(Utbetalingstatus.UTBETALT, inspektør.utbetalingtilstand(0))
        assertEquals(Utbetalingstatus.IKKE_UTBETALT, inspektør.utbetalingtilstand(1))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
    }

    @Test
    fun `forkaster ikke revurderinger - avventer godkjenning revurdering`() {
        nyttVedtak(3.januar til 26.januar)
        nullstillTilstandsendringer()
        this@ForkastingTest.håndterOverstyrTidslinje(
            listOf(
                ManuellOverskrivingDag(26.januar, Dagtype.Feriedag)
            )
        )
        this@ForkastingTest.håndterYtelser(1.vedtaksperiode)
        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@ForkastingTest.håndterAnmodningOmForkasting(1.vedtaksperiode)
        assertEquals(Utbetalingstatus.UTBETALT, inspektør.utbetalingtilstand(0))
        assertEquals(Utbetalingstatus.IKKE_UTBETALT, inspektør.utbetalingtilstand(1))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
    }

    @Test
    fun `forkaster ikke revurderinger - til utbetaling`() {
        nyttVedtak(3.januar til 26.januar)
        nullstillTilstandsendringer()
        this@ForkastingTest.håndterOverstyrTidslinje(
            listOf(
                ManuellOverskrivingDag(26.januar, Dagtype.Feriedag)
            )
        )
        this@ForkastingTest.håndterYtelser(1.vedtaksperiode)
        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@ForkastingTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        this@ForkastingTest.håndterAnmodningOmForkasting(1.vedtaksperiode)
        assertEquals(Utbetalingstatus.UTBETALT, inspektør.utbetalingtilstand(0))
        assertEquals(Utbetalingstatus.OVERFØRT, inspektør.utbetalingtilstand(1))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING)
    }

    @Test
    fun `forkaster ikke revurderinger - revurdering feilet`() {
        nyttVedtak(3.januar til 26.januar)
        nullstillTilstandsendringer()
        this@ForkastingTest.håndterOverstyrTidslinje(
            listOf(
                ManuellOverskrivingDag(26.januar, Dagtype.Feriedag)
            )
        )
        this@ForkastingTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertUgyldigSituasjon("En vedtaksperiode i AVVENTER_GODKJENNING_REVURDERING trenger hjelp!") {
            this@ForkastingTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false)
        }
        assertVarsler(listOf(RV_UT_23, Varselkode.RV_UT_24), 1.vedtaksperiode.filter())
        assertUgyldigSituasjon("En vedtaksperiode i AVVENTER_GODKJENNING_REVURDERING trenger hjelp!") {
            this@ForkastingTest.håndterAnmodningOmForkasting(1.vedtaksperiode)
        }
        assertEquals(Utbetalingstatus.UTBETALT, inspektør.utbetalingtilstand(0))
        assertEquals(Utbetalingstatus.IKKE_GODKJENT, inspektør.utbetalingtilstand(1))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
    }
}
