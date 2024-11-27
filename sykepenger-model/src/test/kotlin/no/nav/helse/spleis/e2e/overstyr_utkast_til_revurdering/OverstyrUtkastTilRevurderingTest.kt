package no.nav.helse.spleis.e2e.overstyr_utkast_til_revurdering

import no.nav.helse.dsl.lagStandardSykepengegrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrInntekt
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.manuellFeriedag
import no.nav.helse.spleis.e2e.manuellSykedag
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Fridag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OverstyrUtkastTilRevurderingTest : AbstractEndToEndTest() {

    @Test
    fun `overstyr utkast til revurdering av periode`() {
        nyttVedtak(januar)
        nullstillTilstandsendringer()
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
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
        )
    }

    @Test
    fun `skal kunne overstyre inntekt i utkast til revurdering ved revurdering av tidslinje`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje((20.januar til 29.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(33235, inspektør.sisteUtbetaling().arbeidsgiverOppdrag.totalbeløp())
        assertEquals(
            "SSSSSHH SSSSSHH SSSSSFF FFFFFFF FSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS",
            inspektør.sykdomshistorikk.sykdomstidslinje().toShortString(),
        )
        assertEquals(
            "PPPPPPP PPPPPPP PPNNNFF FFFFFFF FNN",
            inspektør.sisteUtbetalingUtbetalingstidslinje().toString(),
        )

        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
        )
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `skal kunne overstyre inntekt i utkast til revurdering`() {
        nyttVedtak(januar)
        nullstillTilstandsendringer()

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        // 10153 = round((20000 * 12) / 260) * 11 (11 nav-dager i januar 2018)
        assertEquals(10153, inspektør.sisteUtbetaling().arbeidsgiverOppdrag.totalbeløp())

        håndterOverstyrInntekt(inntekt = 25000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        // 12694 = round((25000 * 12) / 260) * 11 (11 nav-dager i januar)
        assertEquals(12694, inspektør.sisteUtbetaling().arbeidsgiverOppdrag.totalbeløp())

        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
        )
    }

    @Test
    fun `Ved overstyring av revurdering av inntekt til under krav til minste sykepengegrunnlag skal vi opphøre den opprinnelige utbetalingen`() {
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 50000.årlig,
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag =
                lagStandardSykepengegrunnlag(listOf(ORGNUMMER to 50000.månedlig), 1.januar),
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

        assertTrue(inspektør.utbetaling(0).erUtbetalt)
        assertTrue(inspektør.utbetaling(1).erForkastet)
        assertTrue(inspektør.utbetaling(2).erUtbetalt)
        assertEquals(
            inspektør.utbetaling(0).arbeidsgiverOppdrag.nettoBeløp(),
            -1 * inspektør.sisteUtbetaling().arbeidsgiverOppdrag.nettoBeløp(),
        )
        assertEquals(
            inspektør.utbetaling(0).arbeidsgiverOppdrag.fagsystemId,
            inspektør.sisteUtbetaling().arbeidsgiverOppdrag.fagsystemId,
        )
    }

    @Test
    fun `skal kunne overstyre inntekt i utkast til revurdering, også når det er snakk om flere perioder`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        nullstillTilstandsendringer()

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(38773, inspektør.sisteUtbetaling().arbeidsgiverOppdrag.totalbeløp())

        håndterOverstyrInntekt(inntekt = 25000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(41314, inspektør.sisteUtbetaling().arbeidsgiverOppdrag.totalbeløp())

        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
        )
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `overstyr dager i andre periode i pågående revurdering`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje((28.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje((1.februar til 2.februar).map { manuellFeriedag(it) })
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
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
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
        )
        assertTilstander(
            2.vedtaksperiode,
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
    fun `overstyr dager i andre periode i pågående revurdering med tre perioder`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        forlengVedtak(mars)
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje((28.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        håndterOverstyrTidslinje((1.februar til 2.februar).map { manuellFeriedag(it) })
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
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
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
        )
        assertTilstander(
            2.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
        )
        assertTilstander(
            3.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
        )
    }

    @Test
    fun `revurder tidligere periode når det finnes en periode til revurdering`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje((1.februar til 2.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        håndterOverstyrTidslinje((29.januar til 30.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

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
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
        )
        assertTilstander(
            2.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
        )
    }
}
