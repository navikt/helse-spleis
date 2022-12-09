package no.nav.helse.spleis.e2e.overstyr_utkast_til_revurdering

import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
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
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrInntekt
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.manuellFeriedag
import no.nav.helse.spleis.e2e.manuellSykedag
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.Fridag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OverstyrUtkastTilRevurderingTest: AbstractEndToEndTest() {

    @Test
    fun `overstyr utkast til revurdering av periode`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje((28.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellSykedag(it) })
        håndterYtelser(1.vedtaksperiode)

        val utbetalingstidslinje = inspektør.sisteUtbetalingUtbetalingstidslinje()
        assertTrue(utbetalingstidslinje[28.januar] is Fridag)
        assertTrue(utbetalingstidslinje[29.januar] is NavDag)
        assertTrue(utbetalingstidslinje[30.januar] is Fridag)
        assertTrue(utbetalingstidslinje[31.januar] is Fridag)

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
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING
        )
    }

    @Test
    fun `skal kunne overstyre inntekt i utkast til revurdering ved revurdering av tidslinje`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrTidslinje((20.januar til 29.januar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        // 23075 = round((20000 * 12) / 260) * 25 (25 nav-dager i januar + februar 2018)
        assertEquals(23075, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.totalbeløp())
        assertEquals(
            "SSSSSHH SSSSSHH SSSSSFF FFFFFFF FSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS",
            inspektør.sykdomshistorikk.sykdomstidslinje().toShortString().trim()
        )
        assertEquals(
            "PPPPPPP PPPPPPP PPNNNFF FFFFFFF FNNNNHH NNNNNHH NNNNNHH NNNNNHH NNN",
            inspektør.sisteUtbetalingUtbetalingstidslinje().toString().trim()
        )

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

    @Test
    fun `skal kunne overstyre inntekt i utkast til revurdering`() {
        nyttVedtak(1.januar, 31.januar)

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        // 10153 = round((20000 * 12) / 260) * 11 (11 nav-dager i januar 2018)
        assertEquals(10153, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.totalbeløp())

        håndterOverstyrInntekt(inntekt = 25000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        // 12694 = round((25000 * 12) / 260) * 11 (11 nav-dager i januar)
        assertEquals(12694, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.totalbeløp())

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

    @Test
    fun `Ved overstyring av revurdering av inntekt til under krav til minste sykepengegrunnlag skal vi opphøre den opprinnelige utbetalingen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, beregnetInntekt = 50000.årlig)
        val inntekter = listOf(grunnlag(ORGNUMMER, 1.januar, 50000.årlig.repeat(3)))
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(ORGNUMMER, 1.januar, 50000.årlig.repeat(12)),
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekter, arbeidsforhold = emptyList())
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrInntekt(48000.årlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        håndterOverstyrInntekt(46000.årlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val utbetalinger = inspektør.utbetalinger
        assertTrue(utbetalinger[0].inspektør.erUtbetalt)
        assertTrue(utbetalinger[1].inspektør.erForkastet)
        assertTrue(utbetalinger[2].inspektør.erUtbetalt)
        assertEquals(
            utbetalinger.first().inspektør.arbeidsgiverOppdrag.nettoBeløp(),
            -1 * utbetalinger.last().inspektør.arbeidsgiverOppdrag.nettoBeløp()
        )
        assertEquals(1, utbetalinger.map { it.inspektør.arbeidsgiverOppdrag.fagsystemId() }.toSet().size)
    }


    @Test
    fun `skal kunne overstyre inntekt i utkast til revurdering, også når det er snakk om flere perioder`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        // 28613 = round((20000 * 12) / 260) * 31 (31 nav-dager i januar + februar 2018)
        assertEquals(28613, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.totalbeløp())

        håndterOverstyrInntekt(inntekt = 25000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        // 35774 = round((25000 * 12) / 260) * 31 (31 nav-dager i januar + februar)
        assertEquals(35774, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.totalbeløp())

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

    @Test
    fun `overstyr dager i andre periode i pågående revurdering`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterOverstyrTidslinje((28.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterOverstyrTidslinje((1.februar til 2.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        val utbetalingstidslinje = inspektør.sisteUtbetalingUtbetalingstidslinje()
        assertTrue(utbetalingstidslinje[27.januar] is NavHelgDag)
        assertTrue(utbetalingstidslinje[28.januar] is Fridag)
        assertTrue(utbetalingstidslinje[29.januar] is Fridag)
        assertTrue(utbetalingstidslinje[30.januar] is Fridag)
        assertTrue(utbetalingstidslinje[31.januar] is Fridag)
        assertTrue(utbetalingstidslinje[1.februar] is Fridag)
        assertTrue(utbetalingstidslinje[2.februar] is Fridag)
        assertTrue(utbetalingstidslinje[3.februar] is NavHelgDag)

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
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVSLUTTET
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
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `overstyr dager i andre periode i pågående revurdering med tre perioder`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)

        håndterOverstyrTidslinje((28.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)

        håndterOverstyrTidslinje((1.februar til 2.februar).map { manuellFeriedag(it) })
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        val utbetalingstidslinje = inspektør.sisteUtbetalingUtbetalingstidslinje()
        assertTrue(utbetalingstidslinje[27.januar] is NavHelgDag)
        assertTrue(utbetalingstidslinje[28.januar] is Fridag)
        assertTrue(utbetalingstidslinje[29.januar] is Fridag)
        assertTrue(utbetalingstidslinje[30.januar] is Fridag)
        assertTrue(utbetalingstidslinje[31.januar] is Fridag)
        assertTrue(utbetalingstidslinje[1.februar] is Fridag)
        assertTrue(utbetalingstidslinje[2.februar] is Fridag)
        assertTrue(utbetalingstidslinje[3.februar] is NavHelgDag)

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
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVSLUTTET
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
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVSLUTTET
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
            AVVENTER_GODKJENNING_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `revurder tidligere periode når det finnes en periode til revurdering`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterOverstyrTidslinje((1.februar til 2.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        håndterOverstyrTidslinje((29.januar til 30.januar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        val utbetalingstidslinje = inspektør.sisteUtbetalingUtbetalingstidslinje()
        assertTrue(utbetalingstidslinje[28.januar] is NavHelgDag)
        assertTrue(utbetalingstidslinje[29.januar] is Fridag)
        assertTrue(utbetalingstidslinje[30.januar] is Fridag)
        assertTrue(utbetalingstidslinje[31.januar] is NavDag)
        assertTrue(utbetalingstidslinje[1.februar] is Fridag)
        assertTrue(utbetalingstidslinje[2.februar] is Fridag)
        assertTrue(utbetalingstidslinje[3.februar] is NavHelgDag)

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
            AVSLUTTET
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
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }
}
