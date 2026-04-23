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
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_24
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_13
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.PersonUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.Behovsoppsamler
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.enesteGodkjenningsbehovSomFølgeAv
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

internal class ReberegningAvAvsluttetUtenUtbetalingNyE2ETest : AbstractDslTest() {

    @Test
    fun `arbeidsgiver opplyser om egenmeldinger og bruker opplyser om ferie`() = a1 {
        håndterSøknad(Sykdom(1.februar, 10.februar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        nullstillTilstandsendringer()

        håndterInntektsmelding(
            listOf(10.januar til 25.januar),
            beregnetInntekt = INNTEKT
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(10.januar, 28.januar, 100.prosent), Ferie(10.januar, 28.januar))

        assertEquals("UUUGG UUUUUGG UUUUFFF", inspektør.vedtaksperiodeSykdomstidslinje(2.vedtaksperiode).toShortString())
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `omgjøre kort periode etter mottatt im - med eldre utbetalt periode`() = a1 {
        nyttVedtak(januar)
        nyPeriode(10.august til 20.august)
        håndterInntektsmelding(
            listOf(1.august til 16.august),
            beregnetInntekt = INNTEKT
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
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
    fun `inntektsmelding på kort periode gjør at en nyere kort periode skal utbetales`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(10.januar, 20.januar))
        håndterSøknad(Sykdom(10.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.februar, 20.februar))
        håndterSøknad(Sykdom(9.februar, 20.februar, 100.prosent))
        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(10.januar til 25.januar), beregnetInntekt = INNTEKT)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTrue((21.januar til 25.januar).all {
            inspektør.sykdomstidslinje[it] is Dag.UkjentDag
        })
    }

    @Test
    fun `revurderer eldre skjæringstidspunkt`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        nyttVedtak(mars)
        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurderer eldre skjæringstidspunkt selv ved flere mindre perioder`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 26.januar))
        håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent))

        nyttVedtak(mars)

        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(10.januar til 25.januar),
            beregnetInntekt = INNTEKT
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `gjenopptar ikke behandling dersom det er nyere periode som er utbetalt`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        nyttVedtak(mars)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT
        )

        håndterSykmelding(Sykmeldingsperiode(1.mai, 15.mai))
        håndterSykmelding(Sykmeldingsperiode(16.mai, 28.mai))
        håndterSøknad(Sykdom(1.mai, 15.mai, 100.prosent))
        håndterSøknad(Sykdom(16.mai, 28.mai, 100.prosent))

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(4.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `revurderer ikke avsluttet periode dersom perioden fortsatt er innenfor agp etter IM`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(5.januar til 20.januar), beregnetInntekt = INNTEKT)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `avvist revurdering uten tidligere utbetaling kan forkastes`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))

        håndterInntektsmelding(
            listOf(10.januar til 25.januar),
            beregnetInntekt = INNTEKT
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, godkjent = false)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, TIL_INFOTRYGD)
    }

    @Test
    fun `infotrygd har utbetalt perioden - vi har kun arbeidsgiverperiode`() = a1 {
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
        håndterUtbetalingshistorikkEtterInfotrygdendring(
            ArbeidsgiverUtbetalingsperiode(a1, 17.januar, 27.januar)
        )
        håndterYtelser(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        assertVarsel(RV_IT_3, 1.vedtaksperiode.filter())
    }

    @Test
    fun `infotrygd har utbetalt perioden - vi har ingenting`() = a1 {
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterUtbetalingshistorikkEtterInfotrygdendring(
            PersonUtbetalingsperiode(a1, 1.januar, 27.januar)
        )
        håndterYtelser(1.vedtaksperiode)

        assertVarsel(RV_IT_3, 1.vedtaksperiode.filter())
        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `infotrygd har utbetalt perioden - vi har ingenting - flere ag`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
            håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
            håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar))
            håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
        }
        nullstillTilstandsendringer()
        a2 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                PersonUtbetalingsperiode(a2, 1.januar, 27.januar)
            )
            håndterYtelser(1.vedtaksperiode)
        }
        a2 { assertVarsel(RV_IT_3, 1.vedtaksperiode.filter()) }

        a1 { assertTilstander(1.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING) }
        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `tildele utbetaling etter reberegning`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.januar, Dagtype.Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val vedtaksperiode1Utbetalinger = inspektør.utbetalinger(1.vedtaksperiode)
        val vedtaksperiode2Utbetalinger = inspektør.utbetalinger(2.vedtaksperiode)
        assertEquals(2, vedtaksperiode1Utbetalinger.size)
        assertEquals(0, vedtaksperiode2Utbetalinger.size)

        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `avvist omgjøring uten tidligere utbetaling forkaster nyere forlengelser`() = a1 {
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
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
    }

    @Test
    fun `avvist omgjøring uten tidligere utbetaling forkaster ikke nyere perioder`() = a1 {
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
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
    }

    @Test
    fun `avvist omgjøring uten tidligere utbetaling gjenopptar nyere perioder som har inntekt`() = a1 {
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
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 31.januar,
            beregnetInntekt = INNTEKT
        )
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
    }

    @Test
    fun `inntektsmelding gjør om kort periode til arbeidsdager`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(19.januar, 20.januar))
        håndterSøknad(Sykdom(18.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 3.februar))
        håndterSøknad(Sykdom(21.januar, 3.februar, 100.prosent))

        nullstillTilstandsendringer()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        håndterInntektsmelding(
            listOf(10.januar til 20.januar, 28.januar til 1.februar),
            beregnetInntekt = INNTEKT
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)

        assertTrue(inspektør.sykdomstidslinje[21.januar] is Dag.FriskHelgedag)
        assertTrue(inspektør.sykdomstidslinje[27.januar] is Dag.FriskHelgedag)

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        //assertVarsel(RV_IM_4, 2.vedtaksperiode.filter(a1)) // huh? Ser bare 1 IM
        nullstillTilstandsendringer()

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVVENTER_SIMULERING)
    }

    @Test
    fun `støtter omgjøring om det er utbetalt en senere periode på samme skjæringstidspunkt`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(19.januar, 20.januar))
        håndterSøknad(Sykdom(18.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 3.februar))
        håndterSøknad(Sykdom(21.januar, 3.februar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(4.februar, 28.februar))
        håndterSøknad(Sykdom(4.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(listOf(19.januar til 3.februar), beregnetInntekt = INNTEKT)
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        nullstillTilstandsendringer()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)

        val arbeidsgiverperioder = listOf(10.januar til 20.januar, 28.januar til 1.februar)

        håndterInntektsmelding(arbeidsgiverperioder, beregnetInntekt = INNTEKT)

        assertEquals("UUUGG UUUUSHR AAAAARH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomstidslinje.toShortString())
        assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 10.januar, arbeidsgiverperioder)
        assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 28.januar, arbeidsgiverperioder)
        assertSkjæringstidspunktOgVenteperiode(3.vedtaksperiode, 28.januar, arbeidsgiverperioder)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertVarsel(Varselkode.RV_IV_7, 3.vedtaksperiode.filter())
    }

    @Test
    fun `støtter omgjøring om det er utbetalt en senere periode på nyere skjæringstidspunkt`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(19.januar, 20.januar))
        håndterSøknad(Sykdom(18.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 2.februar))
        håndterSøknad(Sykdom(21.januar, 2.februar, 100.prosent))

        nyttVedtak(mai)
        nullstillTilstandsendringer()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)

        håndterInntektsmelding(
            listOf(10.januar til 20.januar, 28.januar til 1.februar),
            beregnetInntekt = INNTEKT
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `inntektsmelding gjør at kort periode faller utenfor agp - før vilkårsprøving`() = a1 {
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

        håndterInntektsmelding(listOf(10.januar til 25.januar), beregnetInntekt = INNTEKT)

        assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        assertVarsler(emptyList(), 2.vedtaksperiode.filter())
        assertVarsler(emptyList(), 3.vedtaksperiode.filter())

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `inntektsmelding gjør at kort periode faller utenfor agp - etter vilkårsprøving`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(28.januar, 31.januar))
        håndterSøknad(Sykdom(28.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(12.januar til 27.januar), beregnetInntekt = INNTEKT)
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)

        nullstillTilstandsendringer()

        håndterInntektsmelding(listOf(10.januar til 25.januar), beregnetInntekt = INNTEKT)

        assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        assertVarsler(emptyList(), 2.vedtaksperiode.filter())
        assertVarsler(emptyList(), 3.vedtaksperiode.filter())

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `revurderer ikke avsluttet periode dersom perioden fortsatt er innenfor agp etter IM selv ved flere mindre`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 25.januar))

        håndterSøknad(Sykdom(21.januar, 25.januar, 100.prosent))

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(10.januar til 25.januar), beregnetInntekt = INNTEKT)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `avsluttet periode trenger egen inntektsmelding etter at inntektsmelding treffer forrige`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(23.januar, 25.januar))
        håndterSøknad(Sykdom(23.januar, 25.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(29.januar, 29.januar))
        håndterSøknad(Sykdom(29.januar, 29.januar, 100.prosent))

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(5.januar til 20.januar), beregnetInntekt = INNTEKT)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `avsluttet periode trenger egen inntektsmelding etter at inntektsmelding treffer forrige 2`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 25.januar))
        håndterSøknad(Sykdom(21.januar, 25.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(29.januar, 29.januar))
        håndterSøknad(Sykdom(29.januar, 29.januar, 100.prosent))

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(5.januar til 20.januar), beregnetInntekt = INNTEKT)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `gjenopptar behandling på neste periode dersom inntektsmelding treffer avsluttet periode`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(5.januar til 20.januar), beregnetInntekt = INNTEKT)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `revurderer ved mottatt inntektsmelding - påfølgende periode med im går i vanlig løype`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 26.januar))
        håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(30.januar, 31.januar))
        håndterSøknad(Sykdom(30.januar, 31.januar, 100.prosent))

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(10.januar til 25.januar), beregnetInntekt = INNTEKT)
        håndterInntektsmelding(
            listOf(10.januar til 25.januar),
            beregnetInntekt = INNTEKT,
            førsteFraværsdag = 30.januar
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `omgjører ved mottatt inntektsmelding - påfølgende periode med im går i vanlig løype - omvendt`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 26.januar))
        håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(30.januar, 31.januar))
        håndterSøknad(Sykdom(30.januar, 31.januar, 100.prosent))

        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(10.januar til 25.januar),
            beregnetInntekt = INNTEKT,
            førsteFraværsdag = 30.januar
        )
        håndterInntektsmelding(
            listOf(10.januar til 25.januar),
            beregnetInntekt = INNTEKT
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `inntektsmelding ag1 - ag1 må vente på inntekt for ag2`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar))
            håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar))
            håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent))
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar))
            håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar))
            håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent))
        }

        nullstillTilstandsendringer()
        a1 {
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT
            )
        }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `inntektsmelding ag2 - ag2 må vente på inntekt for ag1`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar))
            håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar))
            håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent))
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar))
            håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar))
            håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent))
        }

        nullstillTilstandsendringer()
        a2 {
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT
            )
        }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
        }
    }

    @Test
    fun `inntektsmelding for begge arbeidsgivere - bare én av de korte periodene skal utbetales`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar))
            håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar))
            håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent))
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar))
            håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar))
            håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent))
        }

        nullstillTilstandsendringer()
        a1 {
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT
            )
        }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }

        nullstillTilstandsendringer()
        a2 {
            håndterInntektsmelding(
                listOf(3.januar til 18.januar),
                beregnetInntekt = INNTEKT
            )
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }

        a1 {
            assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `inntektsmelding for begge arbeidsgivere - begge de korte periodene skal utbetales`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar))
            håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar))
            håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent))
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar))
            håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar))
            håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent))
        }

        nullstillTilstandsendringer()
        a1 {
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT
            )
        }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }

        nullstillTilstandsendringer()
        a2 {
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT
            )
        }
        a1 {
            assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
        }

        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            val godkjennignsbehov = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                håndterSimulering(1.vedtaksperiode)
            }
            assertEquals("FLERE_ARBEIDSGIVERE", godkjennignsbehov.event.inntektskilde)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }

        a1 {
            assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
        }

        nullstillTilstandsendringer()
        a1 { håndterUtbetalt() }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }

        a1 {
            assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }

        nullstillTilstandsendringer()
        a2 { håndterUtbetalt() }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `arbeidsgiver 1 er utenfor arbeidsgiverperioden, men ikke arbeidsgiver 2`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(17.juni(2022), 21.juni(2022)))
            håndterSøknad(Sykdom(17.juni(2022), 21.juni(2022), 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(17.juni(2022), 21.juni(2022)))
            håndterSøknad(Sykdom(17.juni(2022), 21.juni(2022), 100.prosent))
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(22.juni(2022), 3.juli(2022)))
            håndterSøknad(Sykdom(22.juni(2022), 3.juli(2022), 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(22.juni(2022), 3.juli(2022)))
            håndterSøknad(Sykdom(22.juni(2022), 3.juli(2022), 100.prosent))
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(20.juli(2022), 28.juli(2022)))
            håndterSøknad(Sykdom(20.juli(2022), 28.juli(2022), 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(29.juli(2022), 3.august(2022)))
            håndterSøknad(Sykdom(29.juli(2022), 3.august(2022), 100.prosent))
        }

        nullstillTilstandsendringer()

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertTilstander(3.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(4.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }

        nullstillTilstandsendringer()

        a1 {
            håndterInntektsmelding(
                listOf(
                    7.juni(2022) til 7.juni(2022),
                    9.juni(2022) til 10.juni(2022),
                    17.juni(2022) til 29.juni(2022)
                ),
                beregnetInntekt = INNTEKT
            )
        }
        a2 {
            håndterInntektsmelding(
                listOf(
                    17.juni(2022) til 2.juli(2022)
                ),
                beregnetInntekt = INNTEKT
            )
        }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
            assertTilstander(3.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(4.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `arbeidsgiver 1 er utenfor arbeidsgiverperioden, men ikke arbeidsgiver 2 - feil ved revurdering forkaster periodene`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(17.juni(2022), 21.juni(2022)))
            håndterSøknad(Sykdom(17.juni(2022), 21.juni(2022), 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(17.juni(2022), 21.juni(2022)))
            håndterSøknad(Sykdom(17.juni(2022), 21.juni(2022), 100.prosent))
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(22.juni(2022), 3.juli(2022)))
            håndterSøknad(Sykdom(22.juni(2022), 3.juli(2022), 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(22.juni(2022), 3.juli(2022)))
            håndterSøknad(Sykdom(22.juni(2022), 3.juli(2022), 100.prosent))
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(20.juli(2022), 28.juli(2022)))
            håndterSøknad(Sykdom(20.juli(2022), 28.juli(2022), 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(29.juli(2022), 3.august(2022)))
            håndterSøknad(Sykdom(29.juli(2022), 3.august(2022), 100.prosent))
            håndterInntektsmelding(
                listOf(
                    7.juni(2022) til 7.juni(2022),
                    9.juni(2022) til 10.juni(2022),
                    17.juni(2022) til 29.juni(2022)
                ),
                beregnetInntekt = INNTEKT
            )
        }
        a2 { håndterInntektsmelding(listOf(17.juni(2022) til 2.juli(2022)), beregnetInntekt = INNTEKT) }
        a1 {
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
        }

        nullstillTilstandsendringer()
        a1 { håndterUtbetalingsgodkjenning(2.vedtaksperiode, godkjent = false) }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(4.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `infotrygd har plutselig utbetalt`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(10.januar, 20.januar))
        håndterSøknad(Sykdom(10.januar, 20.januar, 100.prosent))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)

        nullstillTilstandsendringer()
        val utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(a1, 17.januar, 20.januar))
        håndterUtbetalingshistorikkEtterInfotrygdendring(*utbetalinger.toTypedArray())
        håndterYtelser(1.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        assertVarsler(listOf(RV_IT_3), 1.vedtaksperiode.filter())
    }

    @Test
    fun `endrer arbeidsgiverperiode etter igangsatt revurdering`() = a1 {
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

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE)

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)

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
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        assertInntektsgrunnlag(22.januar, forventetAntallArbeidsgivere = 1) {
            assertInntektsgrunnlag(a1, riktigInntekt)
        }

        assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 22.januar, listOf(22.januar til 6.februar))
        assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 22.januar, listOf(22.januar til 6.februar))

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
    fun `arbeidsgiver angrer på innsendt arbeidsgiverperiode - endrer ikke på sykdomstidslinjen fra im2`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(5.februar, 20.februar))
        håndterSøknad(Sykdom(5.februar, 20.februar, 100.prosent), Ferie(10.februar, 20.februar))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)

        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(
                29.januar til 13.februar
            ),
            beregnetInntekt = INNTEKT
        )
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)

        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(
                22.januar til 6.februar
            ),
            beregnetInntekt = INNTEKT
        )

        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
        assertTrue(inspektør.sykdomstidslinje[10.februar] is Dag.ArbeidsgiverHelgedag)
        assertTrue(inspektør.sykdomstidslinje[11.februar] is Dag.ArbeidsgiverHelgedag)
        assertTrue(inspektør.sykdomstidslinje[13.februar] is Dag.Arbeidsgiverdag)
        assertTilstander(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `omgjøring med ghost`() = a1 {
        val beregnetInntektA1 = 31000.månedlig

        håndterSykmelding(Sykmeldingsperiode(10.januar, 25.januar))
        håndterSøknad(Sykdom(10.januar, 25.januar, 100.prosent))
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = beregnetInntektA1
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
        assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        inspektør.utbetalingstidslinjer(1.vedtaksperiode)[17.januar].let {
            assertEquals(1080.daglig, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(0.daglig, it.økonomi.inspektør.personbeløp)
            assertEquals(beregnetInntektA1, it.økonomi.inspektør.aktuellDagsinntekt)
        }

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_AVSLUTTET_UTEN_UTBETALING,
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
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a1 { håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2) }
        a1 { assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter()) }

        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 { forlengVedtak(februar) }

        a2 {
            håndterSykmelding(Sykmeldingsperiode(10.mars, 22.mars))
            håndterSøknad(Sykdom(10.mars, 22.mars, 100.prosent))
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        }

        a1 {
            håndterOverstyrTidslinje(
                listOf(
                    ManuellOverskrivingDag(27.februar, Dagtype.Feriedag)
                )
            )
        }

        a1 {
            håndterYtelser(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }

        nullstillTilstandsendringer()

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET)
            assertTilstander(3.vedtaksperiode, AVVENTER_HISTORIKK)
        }
        a2 { assertTilstander(1.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING) }
    }

    @Test
    fun `forkaster vedtaksperioder i revurdering som kun består av AUU`() = a1 {
        håndterSykmelding(Sykmeldingsperiode(10.januar, 20.januar))
        håndterSøknad(Sykdom(10.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 25.januar))
        håndterSøknad(Sykdom(21.januar, 25.januar, 100.prosent))

        håndterInntektsmelding(listOf(2.januar til 17.januar), beregnetInntekt = INNTEKT)
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
    fun `Skal ikke forkaste vedtaksperioder med overlappende utbetaling som treffes av søknad som forkastes på direkten`() = a1 {
        håndterSøknad(Sykdom(10.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))

        håndterInntektsmelding(listOf(5.januar til 20.januar), beregnetInntekt = INNTEKT)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        nullstillTilstandsendringer()

        assertEquals(5.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(5.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))

        val arbeidsgiverperioder = listOf(2.januar til 17.januar)
        håndterInntektsmelding(
            arbeidsgiverperioder,
            beregnetInntekt = INNTEKT * 1.2
        )
        assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 2.januar, arbeidsgiverperioder)
        assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 2.januar, arbeidsgiverperioder)

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
        assertVarsel(Varselkode.RV_IV_7, 2.vedtaksperiode.filter())
    }

    @Test
    fun `allerede utbetalt i Infotrygd uten utbetaling etterpå`() = a1 {
        nyPeriode(2.januar til 17.januar)
        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 17.januar, 17.januar))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `omgjøring med funksjonell feil, som blir varsel, i inntektsmelding fra Altinn eller LPS`() = a1 {
        håndterSøknad(Sykdom(2.januar, 17.januar, 100.prosent))
        nyttVedtak(18.januar til 31.januar, arbeidsgiverperiode = listOf(2.januar til 17.januar))
        nullstillTilstandsendringer()
        val im = håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "FiskerMedHyre"
        )

        assertEquals(listOf(2.januar til 16.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertInntektshistorikkForDato(INNTEKT, 1.januar, inspektør)
        assertTrue(im !in observatør.inntektsmeldingIkkeHåndtert)
        assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
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
    fun `omgjøring med funksjonell feil i inntektsmelding fra portalen`() = a1 {
        håndterSøknad(Sykdom(2.januar, 17.januar, 100.prosent))
        nyttVedtak(18.januar til 31.januar, arbeidsgiverperiode = listOf(2.januar til 17.januar))
        nullstillTilstandsendringer()
        val im = a1 {
            håndterKorrigerteArbeidsgiveropplysninger(
                1.vedtaksperiode,
                *fraInntektsmelding(
                    beregnetInntekt = INNTEKT,
                    refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()),
                    arbeidsgiverperioder = listOf(1.januar til 16.januar),
                    begrunnelseForReduksjonEllerIkkeUtbetalt = "FiskerMedHyre",
                    opphørAvNaturalytelser = emptyList()
                ).toTypedArray()
            )
        }
        assertVarsler(listOf(RV_IM_4, RV_IM_24, RV_IM_8), 1.vedtaksperiode.filter())
        assertEquals(2.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertInntektshistorikkForDato(INNTEKT, 2.januar, inspektør)

        assertTrue(im in observatør.inntektsmeldingHåndtert.map { it.first })
        assertFalse(im in observatør.inntektsmeldingIkkeHåndtert)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.let {
            assertEquals(3, it.size)
            assertTrue(it.all { behalding -> behalding.tilstand == AVSLUTTET_UTEN_VEDTAK })
            assertEquals(im, it[2].kilde.meldingsreferanseId)
        }
    }
}
