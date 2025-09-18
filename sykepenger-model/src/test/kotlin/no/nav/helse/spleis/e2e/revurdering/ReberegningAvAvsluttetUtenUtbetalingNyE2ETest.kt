package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.august
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.Arbeidsgiveropplysning.Companion.fraInntektsmelding
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding
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
import no.nav.helse.person.BehandlingView.TilstandView.AVSLUTTET_UTEN_VEDTAK
import no.nav.helse.person.BehandlingView.TilstandView.UBEREGNET_OMGJØRING
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_24
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_13
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.PersonUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.sisteBehov
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertInfo
import no.nav.helse.spleis.e2e.assertInntektshistorikkForDato
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterKorrigerteArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlagFlereArbeidsgivere
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ReberegningAvAvsluttetUtenUtbetalingNyE2ETest : AbstractEndToEndTest() {

    @Test
    fun `arbeidsgiver opplyser om egenmeldinger og bruker opplyser om ferie`() {
        håndterSøknad(Sykdom(1.februar, 10.februar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        nullstillTilstandsendringer()

        håndterInntektsmelding(
            listOf(10.januar til 25.januar)
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(10.januar, 28.januar, 100.prosent), Ferie(10.januar, 28.januar))

        assertEquals("UUUGG UUUUUGG UUUUFFF", inspektør.vedtaksperiodeSykdomstidslinje(2.vedtaksperiode).toShortString())
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `omgjøre kort periode etter mottatt im - med eldre utbetalt periode`() {
        nyttVedtak(januar)

        nyPeriode(10.august til 20.august)
        håndterInntektsmelding(
            listOf(1.august til 16.august)
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        val førsteUtbetaling = inspektør.utbetaling(0)
        inspektør.utbetaling(1).also { utbetalingInspektør ->
            assertNotEquals(førsteUtbetaling.korrelasjonsId, utbetalingInspektør.korrelasjonsId)
            assertNotEquals(førsteUtbetaling.arbeidsgiverOppdrag.inspektør.fagsystemId(), utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
            assertNotEquals(førsteUtbetaling.personOppdrag.inspektør.fagsystemId(), utbetalingInspektør.personOppdrag.inspektør.fagsystemId())
            assertEquals(1.august til 20.august, utbetalingInspektør.periode)
        }
    }

    @Test
    fun `inntektsmelding på kort periode gjør at en nyere kort periode skal utbetales`() {
        håndterSykmelding(Sykmeldingsperiode(10.januar, 20.januar))
        håndterSøknad(Sykdom(10.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.februar, 20.februar))
        håndterSøknad(Sykdom(9.februar, 20.februar, 100.prosent))
        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(10.januar til 25.januar))
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTrue((21.januar til 25.januar).all {
            inspektør.sykdomstidslinje[it] is Dag.UkjentDag
        })
    }

    @Test
    fun `revurderer eldre skjæringstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        nyttVedtak(mars, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurderer eldre skjæringstidspunkt selv ved flere mindre perioder`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 26.januar))
        håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent))

        nyttVedtak(mars, vedtaksperiodeIdInnhenter = 3.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(10.januar til 25.januar)
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `gjenopptar ikke behandling dersom det er nyere periode som er utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        nyttVedtak(mars, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar)
        )

        håndterSykmelding(Sykmeldingsperiode(1.mai, 15.mai))
        håndterSykmelding(Sykmeldingsperiode(16.mai, 28.mai))
        håndterSøknad(Sykdom(1.mai, 15.mai, 100.prosent))
        håndterSøknad(Sykdom(16.mai, 28.mai, 100.prosent))

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
        assertTilstander(4.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `revurderer ikke avsluttet periode dersom perioden fortsatt er innenfor agp etter IM`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(5.januar til 20.januar))
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `avvist revurdering uten tidligere utbetaling kan forkastes`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))

        håndterInntektsmelding(
            listOf(10.januar til 25.januar),
            beregnetInntekt = INNTEKT
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        nullstillTilstandsendringer()
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, utbetalingGodkjent = false)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, TIL_INFOTRYGD)
    }

    @Test
    fun `infotrygd har utbetalt perioden - vi har kun arbeidsgiverperiode`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        nullstillTilstandsendringer()
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingshistorikkEtterInfotrygdendring(
            ArbeidsgiverUtbetalingsperiode(a1, 17.januar, 27.januar)
        )
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        assertVarsel(RV_IT_3, 1.vedtaksperiode.filter())
    }

    @Test
    fun `infotrygd har utbetalt perioden - vi har ingenting`() {
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        nullstillTilstandsendringer()
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingshistorikkEtterInfotrygdendring(
            PersonUtbetalingsperiode(a1, 1.januar, 27.januar)
        )
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode)

        assertVarsel(RV_IT_3, 1.vedtaksperiode.filter())
        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `infotrygd har utbetalt perioden - vi har ingenting - flere ag`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar), orgnummer = a1)
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar), orgnummer = a2)
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar), orgnummer = a2)
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            orgnummer = a2
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a2)
        nullstillTilstandsendringer()
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingshistorikkEtterInfotrygdendring(
            PersonUtbetalingsperiode(a2, 1.januar, 27.januar)
        )
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertVarsel(RV_IT_3, 1.vedtaksperiode.filter(orgnummer = a2))

        assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `tildele utbetaling etter reberegning`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        nullstillTilstandsendringer()
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.januar, Dagtype.Feriedag)))
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val vedtaksperiode1Utbetalinger = inspektør.utbetalinger(1.vedtaksperiode)
        val vedtaksperiode2Utbetalinger = inspektør.utbetalinger(2.vedtaksperiode)
        assertEquals(2, vedtaksperiode1Utbetalinger.size)
        assertEquals(0, vedtaksperiode2Utbetalinger.size)

        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `avvist omgjøring uten tidligere utbetaling forkaster nyere forlengelser`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(28.januar, 27.februar))
        håndterSøknad(Sykdom(28.januar, 27.februar, 100.prosent))

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        nullstillTilstandsendringer()
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
    }

    @Test
    fun `avvist omgjøring uten tidligere utbetaling forkaster ikke nyere perioder`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(31.januar, 27.februar))
        håndterSøknad(Sykdom(31.januar, 27.februar, 100.prosent))

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        nullstillTilstandsendringer()
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
    }

    @Test
    fun `avvist omgjøring uten tidligere utbetaling gjenopptar nyere perioder som har inntekt`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(31.januar, 27.februar))
        håndterSøknad(Sykdom(31.januar, 27.februar, 100.prosent))

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT
        )

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 31.januar,
            beregnetInntekt = INNTEKT
        )
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        nullstillTilstandsendringer()
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
    }

    @Test
    fun `inntektsmelding gjør om kort periode til arbeidsdager`() {
        håndterSykmelding(Sykmeldingsperiode(19.januar, 20.januar))
        håndterSøknad(Sykdom(18.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 3.februar))
        håndterSøknad(Sykdom(21.januar, 3.februar, 100.prosent))

        nullstillTilstandsendringer()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        håndterInntektsmelding(
            listOf(10.januar til 20.januar, 28.januar til 1.februar)
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)

        assertTrue(inspektør.sykdomstidslinje[21.januar] is Dag.FriskHelgedag)
        assertTrue(inspektør.sykdomstidslinje[27.januar] is Dag.FriskHelgedag)

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(2.vedtaksperiode)

        //assertVarsel(RV_IM_4, 2.vedtaksperiode.filter(a1)) // huh? Ser bare 1 IM
        nullstillTilstandsendringer()

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVVENTER_SIMULERING)
    }

    @Test
    fun `støtter omgjøring om det er utbetalt en senere periode på samme skjæringstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(19.januar, 20.januar))
        håndterSøknad(Sykdom(18.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 3.februar))
        håndterSøknad(Sykdom(21.januar, 3.februar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(4.februar, 28.februar))
        håndterSøknad(Sykdom(4.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(listOf(19.januar til 3.februar))
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        nullstillTilstandsendringer()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)

        håndterInntektsmelding(
            listOf(10.januar til 20.januar, 28.januar til 1.februar)
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `støtter omgjøring om det er utbetalt en senere periode på nyere skjæringstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(19.januar, 20.januar))
        håndterSøknad(Sykdom(18.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 2.februar))
        håndterSøknad(Sykdom(21.januar, 2.februar, 100.prosent))

        nyttVedtak(mai, vedtaksperiodeIdInnhenter = 3.vedtaksperiode)
        nullstillTilstandsendringer()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)

        håndterInntektsmelding(
            listOf(10.januar til 20.januar, 28.januar til 1.februar)
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `inntektsmelding gjør at kort periode faller utenfor agp - før vilkårsprøving`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(28.januar, 31.januar))
        håndterSøknad(Sykdom(28.januar, 31.januar, 100.prosent))

        nullstillTilstandsendringer()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        håndterInntektsmelding(listOf(10.januar til 25.januar))
        assertInfo("Denne perioden var tidligere regnet som innenfor arbeidsgiverperioden", 2.vedtaksperiode.filter(a1))

        assertVarsler(emptyList(), 1.vedtaksperiode.filter(a1))
        assertVarsler(emptyList(), 2.vedtaksperiode.filter(a1))
        assertVarsler(emptyList(), 3.vedtaksperiode.filter(a1))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `inntektsmelding gjør at kort periode faller utenfor agp - etter vilkårsprøving`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(28.januar, 31.januar))
        håndterSøknad(Sykdom(28.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(12.januar til 27.januar))
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)

        nullstillTilstandsendringer()

        håndterInntektsmelding(listOf(10.januar til 25.januar))

        assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        assertVarsler(emptyList(), 2.vedtaksperiode.filter())
        assertVarsler(emptyList(), 3.vedtaksperiode.filter())

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `revurderer ikke avsluttet periode dersom perioden fortsatt er innenfor agp etter IM selv ved flere mindre`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 25.januar))

        håndterSøknad(Sykdom(21.januar, 25.januar, 100.prosent))

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(10.januar til 25.januar))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `avsluttet periode trenger egen inntektsmelding etter at inntektsmelding treffer forrige`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(23.januar, 25.januar))
        håndterSøknad(Sykdom(23.januar, 25.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(29.januar, 29.januar))
        håndterSøknad(Sykdom(29.januar, 29.januar, 100.prosent))

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(5.januar til 20.januar))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `avsluttet periode trenger egen inntektsmelding etter at inntektsmelding treffer forrige 2`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 25.januar))
        håndterSøknad(Sykdom(21.januar, 25.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(29.januar, 29.januar))
        håndterSøknad(Sykdom(29.januar, 29.januar, 100.prosent))

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(5.januar til 20.januar))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `gjenopptar behandling på neste periode dersom inntektsmelding treffer avsluttet periode`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(5.januar til 20.januar))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `revurderer ved mottatt inntektsmelding - påfølgende periode med im går i vanlig løype`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 26.januar))
        håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(30.januar, 31.januar))
        håndterSøknad(Sykdom(30.januar, 31.januar, 100.prosent))

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(10.januar til 25.januar))
        håndterInntektsmelding(
            listOf(10.januar til 25.januar),
            30.januar
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `omgjører ved mottatt inntektsmelding - påfølgende periode med im går i vanlig løype - omvendt`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 26.januar))
        håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(30.januar, 31.januar))
        håndterSøknad(Sykdom(30.januar, 31.januar, 100.prosent))

        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(10.januar til 25.januar),
            30.januar
        )
        håndterInntektsmelding(
            listOf(10.januar til 25.januar)
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `inntektsmelding ag1 - ag1 må vente på inntekt for ag2`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar), orgnummer = a2)

        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar), orgnummer = a2)

        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a2)

        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            orgnummer = a1
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, orgnummer = a2)
    }

    @Test
    fun `inntektsmelding ag2 - ag2 må vente på inntekt for ag1`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar), orgnummer = a2)

        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar), orgnummer = a2)

        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a2)

        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            orgnummer = a2
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `inntektsmelding for begge arbeidsgivere - bare én av de korte periodene skal utbetales`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar), orgnummer = a2)

        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar), orgnummer = a2)

        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a2)

        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            orgnummer = a1
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, orgnummer = a2)

        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(3.januar til 18.januar),
            orgnummer = a2
        )

        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `inntektsmelding for begge arbeidsgivere - begge de korte periodene skal utbetales`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar), orgnummer = a2)

        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar), orgnummer = a2)

        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a2)

        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            orgnummer = a1
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, orgnummer = a2)

        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            orgnummer = a2
        )
        assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)

        assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)

        assertEquals("FLERE_ARBEIDSGIVERE", personlogg.sisteBehov(Godkjenning).detaljer()["inntektskilde"] as? String)

        assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        nullstillTilstandsendringer()
        håndterUtbetalt(orgnummer = a1)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)

        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        nullstillTilstandsendringer()
        håndterUtbetalt(orgnummer = a2)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `arbeidsgiver 1 er utenfor arbeidsgiverperioden, men ikke arbeidsgiver 2`() {
        håndterSykmelding(Sykmeldingsperiode(17.juni(2022), 21.juni(2022)), orgnummer = a1)
        håndterSøknad(Sykdom(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(17.juni(2022), 21.juni(2022)), orgnummer = a2)
        håndterSøknad(Sykdom(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2022), 3.juli(2022)), orgnummer = a1)
        håndterSøknad(Sykdom(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2022), 3.juli(2022)), orgnummer = a2)
        håndterSøknad(Sykdom(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(20.juli(2022), 28.juli(2022)), orgnummer = a1)
        håndterSøknad(Sykdom(20.juli(2022), 28.juli(2022), 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(29.juli(2022), 3.august(2022)), orgnummer = a1)
        håndterSøknad(Sykdom(29.juli(2022), 3.august(2022), 100.prosent), orgnummer = a1)

        nullstillTilstandsendringer()

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(4.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)

        nullstillTilstandsendringer()

        håndterInntektsmelding(
            listOf(
                7.juni(2022) til 7.juni(2022),
                9.juni(2022) til 10.juni(2022),
                17.juni(2022) til 29.juni(2022)
            ),
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(
                17.juni(2022) til 2.juli(2022)
            ),
            orgnummer = a2
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(4.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
    }

    @Test
    fun `arbeidsgiver 1 er utenfor arbeidsgiverperioden, men ikke arbeidsgiver 2 - feil ved revurdering forkaster periodene`() {
        håndterSykmelding(Sykmeldingsperiode(17.juni(2022), 21.juni(2022)), orgnummer = a1)
        håndterSøknad(Sykdom(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(17.juni(2022), 21.juni(2022)), orgnummer = a2)
        håndterSøknad(Sykdom(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2022), 3.juli(2022)), orgnummer = a1)
        håndterSøknad(Sykdom(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2022), 3.juli(2022)), orgnummer = a2)
        håndterSøknad(Sykdom(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(20.juli(2022), 28.juli(2022)), orgnummer = a1)
        håndterSøknad(Sykdom(20.juli(2022), 28.juli(2022), 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(29.juli(2022), 3.august(2022)), orgnummer = a1)
        håndterSøknad(Sykdom(29.juli(2022), 3.august(2022), 100.prosent), orgnummer = a1)

        håndterInntektsmelding(
            listOf(
                7.juni(2022) til 7.juni(2022),
                9.juni(2022) til 10.juni(2022),
                17.juni(2022) til 29.juni(2022)
            ),
            orgnummer = a1
        )
        håndterInntektsmelding(listOf(17.juni(2022) til 2.juli(2022)), orgnummer = a2)
        håndterVilkårsgrunnlag(2.vedtaksperiode, orgnummer = a1)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)

        nullstillTilstandsendringer()
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, utbetalingGodkjent = false, orgnummer = a1)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, TIL_INFOTRYGD, orgnummer = a1)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD, orgnummer = a2)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD, orgnummer = a1)
        assertForkastetPeriodeTilstander(4.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD, orgnummer = a1)
    }

    @Test
    fun `infotrygd har plutselig utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(10.januar, 20.januar))
        håndterSøknad(Sykdom(10.januar, 20.januar, 100.prosent))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)

        nullstillTilstandsendringer()
        val utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(a1, 17.januar, 20.januar))
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingshistorikkEtterInfotrygdendring(*utbetalinger.toTypedArray())
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        assertVarsler(listOf(RV_IT_3), 1.vedtaksperiode.filter())
    }

    @Test
    fun `endrer arbeidsgiverperiode etter igangsatt revurdering`() {
        val forMyeInntekt = INNTEKT * 1.2
        val riktigInntekt = INNTEKT

        håndterSøknad(Sykdom(5.februar, 11.februar, 100.prosent))
        håndterSøknad(Sykdom(12.februar, 20.februar, 100.prosent))
        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(
                24.januar til 8.februar
            ),
            beregnetInntekt = forMyeInntekt
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode)
        val im = håndterInntektsmelding(
            listOf(
                22.januar til 6.februar
            ),
            beregnetInntekt = riktigInntekt
        )
        assertTrue(im in observatør.inntektsmeldingHåndtert.map { it.first })
        assertVarsler(listOf(RV_IM_24), 1.vedtaksperiode.filter())
        assertVarsler(emptyList(), 2.vedtaksperiode.filter())
        assertEquals(22.januar til 11.februar, inspektør.periode(1.vedtaksperiode))
        assertEquals("UUUUUGG UUUUUGG SSSSSHH SSSSSHH SS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        assertInfo("Denne perioden var tidligere regnet som innenfor arbeidsgiverperioden", 1.vedtaksperiode.filter(a1))
        assertInfo("Denne perioden var tidligere regnet som innenfor arbeidsgiverperioden", 2.vedtaksperiode.filter(a1))

        assertInntektsgrunnlag(22.januar, forventetAntallArbeidsgivere = 1) {
            assertInntektsgrunnlag(a1, riktigInntekt)
        }

        val førsteUtbetalingsdag = inspektør.utbetalingstidslinjer(1.vedtaksperiode)[7.februar]
        assertEquals(riktigInntekt, førsteUtbetalingsdag.økonomi.inspektør.aktuellDagsinntekt)
        assertEquals(riktigInntekt, førsteUtbetalingsdag.økonomi.inspektør.arbeidsgiverRefusjonsbeløp)

        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING
        )
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `arbeidsgiver angrer på innsendt arbeidsgiverperiode - endrer ikke på sykdomstidslinjen fra im2`() {
        håndterSykmelding(Sykmeldingsperiode(5.februar, 20.februar))
        håndterSøknad(Sykdom(5.februar, 20.februar, 100.prosent), Ferie(10.februar, 20.februar))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)

        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(
                29.januar til 13.februar
            )
        )
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)

        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(
                22.januar til 6.februar
            )
        )

        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
        assertTrue(inspektør.sykdomstidslinje[10.februar] is Dag.ArbeidsgiverHelgedag)
        assertTrue(inspektør.sykdomstidslinje[11.februar] is Dag.ArbeidsgiverHelgedag)
        assertTrue(inspektør.sykdomstidslinje[13.februar] is Dag.Arbeidsgiverdag)
        assertTilstander(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `omgjøring med ghost`() {
        val beregnetInntektA1 = 31000.månedlig

        håndterSykmelding(Sykmeldingsperiode(10.januar, 25.januar), orgnummer = a1)
        håndterSøknad(Sykdom(10.januar, 25.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = beregnetInntektA1,
            orgnummer = a1
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        inspektør(a1).utbetalingstidslinjer(1.vedtaksperiode)[17.januar].let {
            assertEquals(1080.daglig, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(0.daglig, it.økonomi.inspektør.personbeløp)
            assertEquals(beregnetInntektA1, it.økonomi.inspektør.aktuellDagsinntekt)
        }

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING
        )
    }

    @Test
    fun `revurdere etter at én arbeidsgiver har blitt til to`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        forlengVedtak(februar, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(10.mars, 22.mars), orgnummer = a2)
        håndterSøknad(Sykdom(10.mars, 22.mars, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterOverstyrTidslinje(
            listOf(
                ManuellOverskrivingDag(27.februar, Dagtype.Feriedag)
            ), orgnummer = a1
        )

        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter(orgnummer = a1))
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        nullstillTilstandsendringer()

        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `forkaster vedtaksperioder i revurdering som kun består av AUU`() {
        håndterSykmelding(Sykmeldingsperiode(10.januar, 20.januar))
        håndterSøknad(Sykdom(10.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 25.januar))
        håndterSøknad(Sykdom(21.januar, 25.januar, 100.prosent))

        håndterInntektsmelding(listOf(2.januar til 17.januar))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))

        assertFunksjonellFeil(RV_SØ_13, 1.vedtaksperiode.filter())
        assertForkastetPeriodeTilstander(3.vedtaksperiode, START, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
    }

    @Test
    fun `Skal ikke forkaste vedtaksperioder med overlappende utbetaling som treffes av søknad som forkastes på direkten`() {
        håndterSøknad(Sykdom(10.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))

        håndterInntektsmelding(listOf(5.januar til 20.januar))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        nullstillTilstandsendringer()

        assertEquals(5.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(5.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))

        håndterInntektsmelding(
            listOf(2.januar til 17.januar),
            beregnetInntekt = INNTEKT * 1.2
        )
        assertEquals(2.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(2.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)

        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))

        assertTilstander(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, START, TIL_INFOTRYGD)

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)

        assertEquals(2.januar til 20.januar, inspektør.periode(1.vedtaksperiode))
        assertEquals(21.januar til 31.januar, inspektør.periode(2.vedtaksperiode))
        assertEquals(2.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(2.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
    }

    @Test
    fun `allerede utbetalt i Infotrygd uten utbetaling etterpå`() {
        nyPeriode(2.januar til 17.januar)
        this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 17.januar, 17.januar))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `omgjøring med funksjonell feil i inntektsmelding fra Altinn eller LPS`() {
        håndterSøknad(Sykdom(2.januar, 17.januar, 100.prosent))
        nyttVedtak(18.januar til 31.januar, arbeidsgiverperiode = listOf(2.januar til 17.januar), vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        nullstillTilstandsendringer()
        val im = håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            begrunnelseForReduksjonEllerIkkeUtbetalt = "FiskerMedHyre"
        )

        assertEquals(listOf(2.januar til 16.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertInntektshistorikkForDato(INNTEKT, 1.januar, inspektør)
        assertTrue(im !in observatør.inntektsmeldingIkkeHåndtert)
        assertFunksjonellFeil(RV_IM_8, 1.vedtaksperiode.filter())
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.let {
            assertEquals(3, it.size)
            assertEquals(AVSLUTTET_UTEN_VEDTAK, it[0].tilstand)
            assertEquals(AVSLUTTET_UTEN_VEDTAK, it[1].tilstand)
            assertEquals(UBEREGNET_OMGJØRING, it[2].tilstand)
            assertEquals(im, it[2].kilde.meldingsreferanseId)
        }
    }

    @Test
    fun `omgjøring med funksjonell feil i inntektsmelding fra portalen`()  {
        håndterSøknad(Sykdom(2.januar, 17.januar, 100.prosent))
        nyttVedtak(18.januar til 31.januar, arbeidsgiverperiode = listOf(2.januar til 17.januar), vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        nullstillTilstandsendringer()
        val im = this@ReberegningAvAvsluttetUtenUtbetalingNyE2ETest.håndterKorrigerteArbeidsgiveropplysninger(
            *fraInntektsmelding(
                beregnetInntekt = INNTEKT,
                refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()),
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "FiskerMedHyre",
                opphørAvNaturalytelser = emptyList()
            ).toTypedArray(),
            vedtaksperiodeId = 1.vedtaksperiode
        )
        assertVarsler(listOf(RV_IM_24, RV_IM_8), 1.vedtaksperiode.filter())
        assertEquals(2.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertInntektshistorikkForDato(INNTEKT, 2.januar, inspektør)

        assertTrue(im in observatør.inntektsmeldingHåndtert.map { it.first })
        assertFalse(im in observatør.inntektsmeldingIkkeHåndtert)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.let {
            assertEquals(3, it.size)
            assertTrue(it.all { behalding -> behalding.tilstand == AVSLUTTET_UTEN_VEDTAK })
            assertEquals(im, it[2].kilde.meldingsreferanseId)
        }
    }
}
