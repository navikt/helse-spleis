package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OverstyrInntektTest : AbstractEndToEndTest() {

    @Test
    fun `skal kunne overstyre en inntekt i et enkelt case`() {
        // Hva gjør vi egentlig med overstyring? Skal man kunne sette opp inntekten uten å ha mottatt en ny inntektsmelding med nye refusjonsopplysninger?
        val fom = 1.januar(2021)
        val overstyrtInntekt = 32000.månedlig
        tilGodkjenning(fom, 31.januar(2021), 100.prosent, fom)

        assertInntektForDato(INNTEKT, fom, inspektør = inspektør)

        håndterInntektsmelding(listOf(fom til fom.plusDays(15)), beregnetInntekt = overstyrtInntekt, refusjon = Refusjon(overstyrtInntekt, null, emptyList()))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrInntekt(inntekt = overstyrtInntekt, orgnummer = ORGNUMMER, skjæringstidspunkt = fom)

        assertTilstander(1.vedtaksperiode,
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
            TilstandType.AVVENTER_GODKJENNING, // <-- Her står vi når vi overstyrer inntekt.
            TilstandType.AVVENTER_VILKÅRSPRØVING)

        // dra saken til AVVENTER_GODKJENNING
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        // assert at vi går gjennom restene av tilstandene som vanlig
        assertTilstander(1.vedtaksperiode,
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
            TilstandType.AVVENTER_GODKJENNING, // <-- Her sto vi da vi overstyrte inntekt.
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING) // <-- og her skal den nye, overstyrte inntekten vært benyttet

        // assert at vi bruker den nye inntekten i beregning av penger til sjuk.
        assertInntektForDato(overstyrtInntekt, fom, inspektør = inspektør)
    }

    @Test
    fun `overstyrt inntekt til mer enn 25 prosent avvik skal sendes til infotrygd`() {
        val fom = 1.januar(2021)
        val overstyrtInntekt = INNTEKT*1.40
        tilGodkjenning(fom, 31.januar(2021), 100.prosent, fom)

        håndterOverstyrInntekt(inntekt = overstyrtInntekt, orgnummer = ORGNUMMER, skjæringstidspunkt = fom)

        håndterVilkårsgrunnlag(1.vedtaksperiode)

        assertForkastetPeriodeTilstander(1.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `avviser revurdering av inntekt for saker med flere arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, orgnummer = a2)

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekter, arbeidsforhold = emptyList()),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        håndterOverstyrInntekt(inntekt = 33000.månedlig, a1, 1.januar)
        assertEquals(1, observatør.avvisteRevurderinger.size)
        assertError("Forespurt overstyring av inntekt hvor personen har flere arbeidsgivere (inkl. ghosts)")
    }

    @Test
    fun `avviser revurdering av inntekt for saker med 1 arbeidsgiver og ghost`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjon = Refusjon(31000.månedlig, null, emptyList()),
            orgnummer = a1
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(12))
                )
            ),
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekter, arbeidsforhold = emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        håndterOverstyrInntekt(32000.månedlig, a1, 1.januar)
        assertEquals(1, observatør.avvisteRevurderinger.size)
        assertError("Forespurt overstyring av inntekt hvor personen har flere arbeidsgivere (inkl. ghosts)")
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
            TilstandType.AVVENTER_SIMULERING_REVURDERING,
            TilstandType.AVVENTER_GODKJENNING_REVURDERING,
            TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_SIMULERING_REVURDERING,
            TilstandType.AVVENTER_GODKJENNING_REVURDERING
            )
    }

    @Test
    fun `skal kunne overstyre inntekt i utkast til revurdering, også når det er snakk om flere perioder`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        // 28613 = round((20000 * 12) / 260) * 31 (31 nav-dager i januar + februar 2018)
        assertEquals(28613, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.totalbeløp())

        håndterOverstyrInntekt(inntekt = 25000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        // 35774 = round((25000 * 12) / 260) * 31 (31 nav-dager i januar + februar)
        assertEquals(35774, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.totalbeløp())

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
            TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING,
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

    @Test
    fun `skal kunne overstyre inntekt i utkast til revurdering ved revurdering av tidslinje`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrTidslinje((20.januar til 29.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
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
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING,
            TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING,
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

    @Test
    fun `Ved overstyring av inntekt til under krav til minste sykepengegrunnlag skal vi lage en utbetaling uten utbetaling`() {
        val OverMinstegrense = 50000.årlig
        val UnderMinstegrense = 46000.årlig

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, beregnetInntekt = OverMinstegrense)
        håndterYtelser(1.vedtaksperiode)
        val inntekter = listOf(grunnlag(ORGNUMMER, 1.januar, OverMinstegrense.repeat(3)))
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(ORGNUMMER, 1.januar, OverMinstegrense.repeat(12)),
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekter, arbeidsforhold = emptyList())
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        håndterOverstyrInntekt(UnderMinstegrense, skjæringstidspunkt = 1.januar)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(ORGNUMMER, 1.januar, OverMinstegrense.repeat(12)),
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekter, arbeidsforhold = emptyList())
        )
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        val utbetalinger = inspektør.utbetalinger
        assertEquals(2, utbetalinger.size)
        assertEquals(0, utbetalinger.last().inspektør.arbeidsgiverOppdrag.nettoBeløp())
        assertTrue(utbetalinger.last().erAvsluttet())
        assertTrue(utbetalinger.first().inspektør.erForkastet)
    }

    @Test
    fun `Ved overstyring av revurderging av inntekt til under krav til minste sykepengegrunnlag skal vi opphøre den opprinnelige utbetalingen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, beregnetInntekt = 50000.årlig)
        håndterYtelser(1.vedtaksperiode)
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
        assertEquals(utbetalinger.first().inspektør.arbeidsgiverOppdrag.nettoBeløp(), -1 * utbetalinger.last().inspektør.arbeidsgiverOppdrag.nettoBeløp())
        assertEquals(1, utbetalinger.map { it.inspektør.arbeidsgiverOppdrag.fagsystemId() }.toSet().size)
    }
}
