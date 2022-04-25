package no.nav.helse.spleis.e2e

import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class OverstyrTidslinjeTest : AbstractEndToEndTest() {

    @Test
    fun `kan ikke utbetale overstyrt utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(2.januar, 18.januar)), førsteFraværsdag = 2.januar)
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode.id(ORGNUMMER))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellSykedag(2.januar), manuellArbeidsgiverdag(24.januar), manuellFeriedag(25.januar)))
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
    }

    @Test
    fun `overstyrer sykedag på slutten av perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode.id(ORGNUMMER))
        håndterYtelser(1.vedtaksperiode)
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
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknad(Sykdom(2.januar, 25.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode.id(ORGNUMMER))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellArbeidsgiverdag(18.januar)))
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertTilstander(
            0,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING
        )
        assertNotEquals(inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.fagsystemId(), inspektør.utbetaling(1).inspektør.arbeidsgiverOppdrag.fagsystemId())
        assertEquals(19.januar, inspektør.utbetalinger.last().utbetalingstidslinje().sykepengeperiode()?.start)
    }

    @Test
    fun `grad over grensen overstyres på enkeltdag`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknad(Sykdom(2.januar, 25.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode.id(ORGNUMMER))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellSykedag(22.januar, 30)))

        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(3, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.size)
        assertEquals(21.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[0].tom)
        assertEquals(30, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[1].grad)
        assertEquals(23.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[2].fom)
    }

    @Test
    fun `grad under grensen blir ikke utbetalt etter overstyring av grad`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknad(Sykdom(2.januar, 25.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode.id(ORGNUMMER))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellSykedag(22.januar, 0)))

        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(2, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.size)
        assertEquals(19.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[0].tom)
        assertEquals(23.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[1].fom)
    }

    @Test
    fun `overstyrt til fridager i midten av en periode blir ikke utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(2.januar, 25.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode.id(ORGNUMMER))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellFeriedag(22.januar), manuellPermisjonsdag(23.januar)))

        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(2, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.size)
        assertEquals(19.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[0].tom)
        assertEquals(24.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[1].fom)
    }

    @Test
    fun `Overstyring oppdaterer sykdomstidlinjene`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode.id(ORGNUMMER))
        håndterYtelser(1.vedtaksperiode)
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
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(20.januar, 21.januar))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode.id(ORGNUMMER))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyringSykedag(20.januar til 21.januar)
        håndterYtelser(1.vedtaksperiode)

        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertEquals("PPPPPPP PPPPPPP PPNNNHH NNNNNHH NNN", inspektør.utbetalingstidslinjer(1.vedtaksperiode).toString())
    }

    @Test
    fun `Overstyring av utkast til revurdering sender senere perioder til AVVENTER_ARBEIDSGIVERE_REVURDERING`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)

        håndterOverstyrTidslinje((20.januar til 29.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        // Denne overstyringen kommer før den forrige er ferdig prossessert
        håndterOverstyrTidslinje((30.januar til 31.januar).map { manuellFeriedag(it) })

        assertTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING
        )

        assertTilstander(
            2.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING
        )

        assertTilstander(
            3.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING
        )
    }

    @Test
    fun `Overstyring av utkast til revurdering sender senere periode i AVVENTER_SIMULERING_REVURDERING til AVVENTER_ARBEIDSGIVERE_REVURDERING`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)

        håndterOverstyrTidslinje((20.januar til 29.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)

        // Denne overstyringen kommer før den forrige er ferdig prossessert
        håndterOverstyrTidslinje((30.januar til 31.januar).map { manuellFeriedag(it) })

        assertTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING
        )

        assertTilstander(
            2.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING
        )

        assertTilstander(
            3.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_SIMULERING_REVURDERING,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING
        )
    }


    @Test
    fun `skal kunne overstyre dagtype i utkast til revurdering ved revurdering av inntekt`() {
            nyttVedtak(1.januar, 31.januar)
            forlengVedtak(1.februar, 28.februar)

            håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
            håndterYtelser(1.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)

            håndterOverstyrTidslinje((20.januar til 29.januar).map { manuellFeriedag(it) })
            håndterYtelser(1.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)

            // 23075 = round((20000 * 12) / 260) * 25 (25 nav-dager i januar + februar 2018)
            assertEquals(23075, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.totalbeløp())
            assertEquals("SSSSSHH SSSSSHH SSSSSFF FFFFFFF FSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString().trim())
            assertEquals("PPPPPPP PPPPPPP PPNNNFF FFFFFFF FNNNNHH NNNNNHH NNNNNHH NNNNNHH NNN", inspektør.sisteUtbetalingUtbetalingstidslinje().toString().trim())

            assertTilstander(1.vedtaksperiode,
                TilstandType.START,
                TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
                TilstandType.AVVENTER_HISTORIKK,
                TilstandType.AVVENTER_VILKÅRSPRØVING,
                TilstandType.AVVENTER_HISTORIKK,
                TilstandType.AVVENTER_SIMULERING,
                TilstandType.AVVENTER_GODKJENNING,
                TilstandType.TIL_UTBETALING,
                TilstandType.AVSLUTTET,
                TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING,
                TilstandType.AVVENTER_HISTORIKK_REVURDERING,
                TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING,
                TilstandType.AVVENTER_HISTORIKK_REVURDERING,
                TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
            )

            assertTilstander(2.vedtaksperiode,
                TilstandType.START,
                TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
                TilstandType.AVVENTER_HISTORIKK,
                TilstandType.AVVENTER_SIMULERING,
                TilstandType.AVVENTER_GODKJENNING,
                TilstandType.TIL_UTBETALING,
                TilstandType.AVSLUTTET,
                TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING,
                TilstandType.AVVENTER_HISTORIKK_REVURDERING,
                TilstandType.AVVENTER_SIMULERING_REVURDERING,
                TilstandType.AVVENTER_GODKJENNING_REVURDERING,
                TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING,
                TilstandType.AVVENTER_HISTORIKK_REVURDERING,
                TilstandType.AVVENTER_SIMULERING_REVURDERING,
                TilstandType.AVVENTER_GODKJENNING_REVURDERING
            )
        }
}
