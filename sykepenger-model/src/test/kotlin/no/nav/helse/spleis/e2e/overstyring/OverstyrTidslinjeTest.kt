package no.nav.helse.spleis.e2e.overstyring

import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrInntekt
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterOverstyringSykedag
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterSøknadMedValidering
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.manuellArbeidsgiverdag
import no.nav.helse.spleis.e2e.manuellFeriedag
import no.nav.helse.spleis.e2e.manuellPermisjonsdag
import no.nav.helse.spleis.e2e.manuellSykedag
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OverstyrTidslinjeTest : AbstractEndToEndTest() {

    @Test
    fun `vedtaksperiode strekker seg tilbake og endrer skjæringstidspunktet`() {
        tilGodkjenning(10.januar, 31.januar, a1)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(
            ManuellOverskrivingDag(9.januar, Dagtype.Sykedag, 100)
        ), orgnummer = a1)

        assertEquals(9.januar til 31.januar, inspektør.periode(1.vedtaksperiode))

        assertForventetFeil(
            forklaring = "vedtaksperioden forkastes pga. vi ikke har inntekt for det nye skjæringstidspunktet",
            nå = {
                val dagen = inspektør.sykdomstidslinje[9.januar]
                assertEquals(Dag.UkjentDag::class, dagen::class)
                assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
            },
            ønsket = {
                val dagen = inspektør.sykdomstidslinje[9.januar]
                assertEquals(Dag.Sykedag::class, dagen::class)
                assertTrue(dagen.kommerFra(OverstyrTidslinje::class))
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
                håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
                håndterYtelser(1.vedtaksperiode, orgnummer = a1)
                assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
            }
        )
    }

    @Test
    fun `vedtaksperiode strekker seg tilbake og endrer ikke skjæringstidspunktet`() {
        tilGodkjenning(10.januar, 31.januar, a1)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(
            ManuellOverskrivingDag(9.januar, Dagtype.Arbeidsdag)
        ), orgnummer = a1)

        val dagen = inspektør.sykdomstidslinje[9.januar]
        assertEquals(Dag.Arbeidsdag::class, dagen::class)
        assertTrue(dagen.kommerFra(OverstyrTidslinje::class))

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertEquals(9.januar til 31.januar, inspektør.periode(1.vedtaksperiode))
        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `strekker ikke inn i forrige periode`() {
        nyPeriode(1.januar til 9.januar, a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        tilGodkjenning(10.januar, 31.januar, a1) // 1. jan - 9. jan blir omgjort til arbeidsdager ved innsending av IM her
        nullstillTilstandsendringer()
        // Saksbehandler korrigerer; 9.januar var vedkommende syk likevel
        håndterOverstyrTidslinje(listOf(
            ManuellOverskrivingDag(9.januar, Dagtype.Arbeidsdag)
        ), orgnummer = a1)

        val dagen = inspektør.sykdomstidslinje[9.januar]
        assertEquals(Dag.Arbeidsdag::class, dagen::class)
        assertTrue(dagen.kommerFra(OverstyrTidslinje::class))

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        assertEquals(1.januar til 9.januar, inspektør.periode(1.vedtaksperiode))
        assertEquals(10.januar til 31.januar, inspektør.periode(2.vedtaksperiode))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `endrer skjæringstidspunkt på en førstegangsbehandling ved å omgjøre en arbeidsdag til sykedag`() {
        nyPeriode(1.januar til 9.januar, a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        tilGodkjenning(10.januar, 31.januar, a1) // 1. jan - 9. jan blir omgjort til arbeidsdager ved innsending av IM her
        nullstillTilstandsendringer()
        // Saksbehandler korrigerer; 9.januar var vedkommende syk likevel
        håndterOverstyrTidslinje(listOf(
            ManuellOverskrivingDag(9.januar, Dagtype.Sykedag, 100)
        ), orgnummer = a1)

        val dagen = inspektør.sykdomstidslinje[9.januar]
        assertEquals(Dag.Sykedag::class, dagen::class)
        assertTrue(dagen.kommerFra(OverstyrTidslinje::class))

        assertForventetFeil(
            forklaring = "skjæringstidspunktet endres fra 10.jan til 9.jan, som gjør at vi også står uten inntekt",
            nå = {
              assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
            },
            ønsket = {
                assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, orgnummer = a1)
                håndterVilkårsgrunnlag(2.vedtaksperiode, orgnummer = a1)
                håndterYtelser(2.vedtaksperiode, orgnummer = a1)

                assertEquals(1.januar til 9.januar, inspektør.periode(1.vedtaksperiode))
                assertEquals(10.januar til 31.januar, inspektør.periode(2.vedtaksperiode))

                assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
                assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
            }
        )
    }

    @Test
    fun `vedtaksperiode strekker seg ikke tilbake hvis det er en periode foran`() {
        nyPeriode(1.januar til 9.januar, a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        nyPeriode(10.januar til 31.januar, a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(
            ManuellOverskrivingDag(9.januar, Dagtype.Sykedag, 100)
        ), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        val dagen = inspektør.sykdomstidslinje[9.januar]
        assertEquals(Dag.Sykedag::class, dagen::class)
        assertTrue(dagen.kommerFra(OverstyrTidslinje::class))

        assertEquals(1.januar til 9.januar, inspektør.periode(1.vedtaksperiode))
        assertEquals(10.januar til 31.januar, inspektør.periode(2.vedtaksperiode))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `kan ikke utbetale overstyrt utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterInntektsmelding(listOf(Periode(2.januar, 18.januar)), førsteFraværsdag = 2.januar)
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellSykedag(2.januar), manuellArbeidsgiverdag(24.januar), manuellFeriedag(25.januar)))
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
    }

    @Test
    fun `overstyrer sykedag på slutten av perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterInntektsmelding(listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellSykedag(2.januar), manuellArbeidsgiverdag(24.januar), manuellFeriedag(25.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
        assertEquals(Utbetaling.Sendt, inspektør.utbetalingtilstand(1))
        assertNotEquals(inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.fagsystemId(), inspektør.utbetaling(1).inspektør.arbeidsgiverOppdrag.fagsystemId())
        assertEquals("SSSSHH SSSSSHH SSSSSHH SSUFS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `vedtaksperiode rebehandler informasjon etter overstyring fra saksbehandler`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar))
        håndterInntektsmelding(listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknad(Sykdom(2.januar, 25.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellArbeidsgiverdag(18.januar)))
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
        assertNotEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertTilstander(
            0,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING
        )
        assertNotEquals(inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.fagsystemId(), inspektør.utbetaling(1).inspektør.arbeidsgiverOppdrag.fagsystemId())
        assertEquals(19.januar, inspektør.utbetalinger.last().utbetalingstidslinje().sykepengeperiode()?.start)
    }

    @Test
    fun `grad over grensen overstyres på enkeltdag`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar))
        håndterInntektsmelding(listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknad(Sykdom(2.januar, 25.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellSykedag(22.januar, 30)))

        assertNotEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(3, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.size)
        assertEquals(21.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[0].tom)
        assertEquals(30, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[1].grad)
        assertEquals(23.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[2].fom)
    }

    @Test
    fun `grad under grensen blir ikke utbetalt etter overstyring av grad`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar))
        håndterInntektsmelding(listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknad(Sykdom(2.januar, 25.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellSykedag(22.januar, 0)))

        assertNotEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(2, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.size)
        assertEquals(21.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[0].tom)
        assertEquals(23.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[1].fom)
    }

    @Test
    fun `overstyrt til fridager i midten av en periode blir ikke utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar))
        håndterInntektsmelding(listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(2.januar, 25.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellFeriedag(22.januar), manuellPermisjonsdag(23.januar)))

        assertNotEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(2, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.size)
        assertEquals(21.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[0].tom)
        assertEquals(24.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[1].fom)
    }

    @Test
    fun `Overstyring oppdaterer sykdomstidlinjene`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellFeriedag(26.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

        assertEquals("SSSHH SSSSSHH SSSSSHH SSSSF", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertEquals("SSSHH SSSSSHH SSSSSHH SSSSF", inspektør.sykdomstidslinje.toShortString())
        assertEquals("PPPPP PPPPPPP PPPPNHH NNNNF", inspektør.utbetalingstidslinjer(1.vedtaksperiode).toString())
    }

    @Test
    fun `Overstyring av sykHelgDag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(20.januar, 21.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyringSykedag(20.januar til 21.januar)
        håndterYtelser(1.vedtaksperiode)

        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertEquals("PPPPPPP PPPPPPP PPNNNHH NNNNNHH NNN", inspektør.utbetalingstidslinjer(1.vedtaksperiode).toString())
    }

    @Test
    fun `Overstyring av utkast til revurdering`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)

        håndterOverstyrTidslinje((20.januar til 29.januar).map { manuellFeriedag(it) })
        håndterYtelser(3.vedtaksperiode)

        // Denne overstyringen kommer før den forrige er ferdig prossessert
        håndterOverstyrTidslinje((30.januar til 31.januar).map { manuellFeriedag(it) })

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING
        )

        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING
        )
    }


    @Test
    fun `skal kunne overstyre dagtype i utkast til revurdering ved revurdering av inntekt`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        håndterOverstyrTidslinje((20.januar til 29.januar).map { manuellFeriedag(it) })
        inspektør.utbetalinger(2.vedtaksperiode).also { utbetalinger ->
            assertEquals(2, utbetalinger.size)
            assertEquals(Utbetaling.Forkastet, utbetalinger.last().inspektør.tilstand)
        }
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        // 23075 = round((20000 * 12) / 260) * 25 (25 nav-dager i januar + februar 2018)
        assertEquals(23075, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.totalbeløp())
        assertEquals("SSSSSHH SSSSSHH SSSSSFF FFFFFFF FSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString().trim())
        assertEquals("PPPPPPP PPPPPPP PPNNNFF FFFFFFF FNNNNHH NNNNNHH NNNNNHH NNNNNHH NNN", inspektør.sisteUtbetalingUtbetalingstidslinje().toString().trim())

        assertTilstander(1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING
        )

        assertTilstander(2.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )
    }
}
