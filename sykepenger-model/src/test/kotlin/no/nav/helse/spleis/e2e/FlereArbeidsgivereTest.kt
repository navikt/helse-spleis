package no.nav.helse.spleis.e2e

import no.nav.helse.ForventetFeil
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class FlereArbeidsgivereTest : AbstractEndToEndTest() {

    @Test
    fun `Sammenligningsgrunnlag for flere arbeidsgivere`() {
        val periodeA1 = 1.januar til 31.januar
        nyPeriode(1.januar til 31.januar, a1)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(periodeA1.start, periodeA1.start.plusDays(15))),
            førsteFraværsdag = periodeA1.start,
            beregnetInntekt = 30000.månedlig,
            refusjon = Refusjon(30000.månedlig, null, emptyList()),
            orgnummer = a1
        )
        person.håndter(ytelser(1.vedtaksperiode, orgnummer = a1, inntektshistorikk = emptyList()))
        person.håndter(vilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.juni(2017) inntekter {
                        a1 inntekt INNTEKT
                        a2 inntekt 5000.månedlig
                    }
                    1.august(2017) til 1.desember(2017) inntekter {
                        a1 inntekt 17000.månedlig
                        a2 inntekt 3500.månedlig
                    }
                }
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList())
        )
        )

        val periodeA2 = 15.januar til 15.februar
        nyPeriode(periodeA2, a2)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(periodeA2.start, periodeA2.start.plusDays(15))),
            førsteFraværsdag = periodeA2.start,
            beregnetInntekt = 10000.månedlig,
            refusjon = Refusjon(10000.månedlig, null, emptyList()),
            orgnummer = a2

        )

        assertEquals(318500.årlig, person.sammenligningsgrunnlag(1.januar))
    }

    @Test
    fun `Sammenligningsgrunnlag for flere arbeidsgivere med flere sykeperioder`() {
        nyPeriode(15.januar til 5.februar, a1)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(15.januar til 28.januar, 2.februar til 3.februar),
            førsteFraværsdag = 2.februar,
            beregnetInntekt = 30000.månedlig,
            refusjon = Refusjon(30000.månedlig, null, emptyList()),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1, inntektshistorikk = emptyList())
        håndterVilkårsgrunnlag(1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.juni(2017) inntekter {
                        a1 inntekt INNTEKT
                        a2 inntekt 5000.månedlig
                    }
                    1.august(2017) til 1.desember(2017) inntekter {
                        a1 inntekt 17000.månedlig
                        a2 inntekt 3500.månedlig
                    }
                }
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList())
        )
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING, TIL_INFOTRYGD)
        assertEquals(282500.årlig, person.sammenligningsgrunnlag(2.februar))
    }

    @Test
    @ForventetFeil("Trenger inntekt fra Inntektskomponenten før disse virker (§8-28)")
    fun `Sammenligningsgrunnlag for flere arbeidsgivere som overlapper hverandres sykeperioder`() {
        nyPeriode(15.januar til 5.februar, a1)
        person.håndter(
            inntektsmelding(
                UUID.randomUUID(),
                arbeidsgiverperioder = listOf(15.januar til 28.januar, 2.februar til 3.februar),
                beregnetInntekt = 30000.månedlig,
                førsteFraværsdag = 2.februar,
                refusjon = Refusjon(30000.månedlig, null, emptyList()),
                orgnummer = a1
            )
        )
        val periodeA2 = 15.januar til 15.februar
        nyPeriode(periodeA2, a2)

        person.håndter(vilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.juni(2017) inntekter {
                        a1 inntekt INNTEKT
                        a2 inntekt 5000.månedlig
                    }
                    1.august(2017) til 1.desember(2017) inntekter {
                        a1 inntekt 17000.månedlig
                        a2 inntekt 3500.månedlig
                    }
                }
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList())
        ))

        person.håndter(
            inntektsmelding(
                UUID.randomUUID(),
                arbeidsgiverperioder = listOf(Periode(periodeA2.start, periodeA2.start.plusDays(15))),
                beregnetInntekt = 10000.månedlig,
                førsteFraværsdag = periodeA2.start,
                refusjon = Refusjon(10000.månedlig, null, emptyList()),
                orgnummer = a2
            )
        )

        assertEquals(318500.årlig, person.sammenligningsgrunnlag(15.januar))
    }

    @Test
    @ForventetFeil("Trenger inntekt fra Inntektskomponenten før disse virker (§8-28)")
    fun `overlappende arbeidsgivere ikke sendt til infotrygd`() {
        gapPeriode(1.januar til 31.januar, a1)
        gapPeriode(15.januar til 15.februar, a2)
        assertNoErrors(a1.inspektør)
        assertNoErrors(a2.inspektør)

        historikk(a1)
        assertNoErrors(a1.inspektør)
        assertNoErrors(a2.inspektør)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
    }

    @Test
    fun `vedtaksperioder atskilt med betydelig tid`() {
        prosessperiode(1.januar til 31.januar, a1)
        assertNoErrors(a1.inspektør)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)

        prosessperiode(1.mars til 31.mars, a2)
        assertNoErrors(a2.inspektør)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
    }

    @Test
    @ForventetFeil("Trenger inntekt fra Inntektskomponenten før disse virker (§8-28)")
    fun `Tre overlappende perioder med en ikke-overlappende periode`() {
        gapPeriode(1.januar til 31.januar, a1)
        gapPeriode(15.januar til 15.mars, a2)
        gapPeriode(1.februar til 28.februar, a3)
        gapPeriode(15.april til 15.mai, a4)

        historikk(a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a4)

        historikk(a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a4)

        historikk(a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a4)

        historikk(a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a4)

        historikk(a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a4)

        betale(a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a4)

        historikk(a3)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING, orgnummer = a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a4)

        betale(a3)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a4)

        historikk(a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a4)

        betale(a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a4)

        historikk(a4)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING, orgnummer = a4)

        betale(a4)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a3)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a4)
    }

    @Test
    @ForventetFeil("Trenger inntekt fra Inntektskomponenten før disse virker (§8-28)")
    fun `Tre paralelle perioder`() {
        gapPeriode(3.januar til 31.januar, a1)
        gapPeriode(1.januar til 31.januar, a2)
        gapPeriode(2.januar til 31.januar, a3)

        historikk(a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a3)

        historikk(a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a3)

        historikk(a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a3)

        historikk(a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a3)

        historikk(a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a3)

        betale(a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a3)

        historikk(a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a3)

        betale(a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a3)

        historikk(a3)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING, orgnummer = a3)

        betale(a3)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a3)
    }

    @Test
    fun `Tillater førstegangsbehandling av flere arbeidsgivere der inntekt i inntektsmelding er på samme dato`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a2)

        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a2)

        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a2)

        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)

        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2020) til 1.desember(2020) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INNTEKT
                }
            }
        ))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, TIL_UTBETALING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, TIL_UTBETALING, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
    }

    @Test
    fun `Tillater førstegangsbehandling av flere arbeidsgivere der inntekt i inntektsmelding ikke er på samme dato - så lenge de er i samme måned`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a2)

        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a2)

        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a2)

        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, orgnummer = a2)

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
            førsteFraværsdag = 1.januar(2021),
            orgnummer = a1
        )
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, orgnummer = a2)

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar(2021) til 3.januar(2021), 6.januar(2021) til 18.januar(2021)),
            førsteFraværsdag = 6.januar(2021),
            beregnetInntekt = 1000.månedlig,
            orgnummer = a2
        )
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2020) til 1.desember(2020) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 1000.månedlig
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)
    }

    @Test
    fun `Tillater ikke førstegangsbehandling av flere arbeidsgivere der inntekt i inntektsmelding ikke er på samme dato - hvis datoene er i forskjellig måned`() {
        val periode = 31.desember(2020) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a2)

        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a2)

        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a2)

        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, orgnummer = a2)

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(31.desember(2020) til 15.januar(2021)),
            førsteFraværsdag = 31.desember(2020),
            orgnummer = a1
        )
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, orgnummer = a2)

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar(2021) til 3.januar(2021), 6.januar(2021) til 18.januar(2021)),
            førsteFraværsdag = 6.januar(2021),
            beregnetInntekt = 1000.månedlig,
            orgnummer = a2
        )
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2020) til 1.desember(2020) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 1000.månedlig
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a2)
    }

    @Test
    fun `Lager ikke utbetalinger for vedtaksperioder hos andre arbeidsgivere som ligger senere i tid enn den som er først totalt sett`() {
        val periode = 1.februar(2021) til 28.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        val inntektshistorikk = listOf(
            Inntektsopplysning(a1.toString(), 1.januar(2021), INNTEKT, true)
        )
        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1.toString(), 1.januar(2021), 31.januar(2021), 100.prosent, INNTEKT)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1, inntektshistorikk = inntektshistorikk)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        val periode2 = 1.mars(2021) til 31.mars(2021)
        val a2Periode = 2.april(2021) til 30.april(2021)
        håndterSykmelding(Sykmeldingsperiode(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(a2Periode.start, a2Periode.endInclusive, 100.prosent), orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a2)

        håndterSøknad(Sykdom(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1, inntektshistorikk = inntektshistorikk)

        assertEquals(0, inspektør(a2).ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
        assertEquals(0, inspektør(a2).avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
    }

    @Test
    fun `tillater to arbeidsgivere med korte perioder, og forlengelse av disse`() {
        val periode = 1.januar(2021) til 14.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)

        val forlengelseperiode = 15.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
            førsteFraværsdag = 1.januar(2021),
            orgnummer = a1
        )

        assertSisteTilstand(2.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, orgnummer = a2)

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
            førsteFraværsdag = 1.januar(2021),
            orgnummer = a2
        )

        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)

        håndterYtelser(2.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)

        håndterVilkårsgrunnlag(2.vedtaksperiode, orgnummer = a1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2020) til 1.desember(2020) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(2.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true, orgnummer = a1)

        håndterUtbetalt(2.vedtaksperiode, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)

        håndterYtelser(2.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)
        håndterSimulering(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true, orgnummer = a2)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a2)

        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a2)
    }

    @Test
    fun `medlemskap ikke oppfyllt i vilkårsgrunnlag, avviser perioden riktig for begge arbeidsgivere`() {
        val periode = Periode(1.januar, 31.januar)
        nyPeriode(periode, a1)
        nyPeriode(periode, a2)

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(periode.start, periode.start.plusDays(15))),
            førsteFraværsdag = periode.start,
            orgnummer = a1
        )

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(periode.start, periode.start.plusDays(15))),
            førsteFraværsdag = periode.start,
            orgnummer = a2
        )

        historikk(a1)
        person.håndter(
            vilkårsgrunnlag(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                orgnummer = a1,
                inntektsvurdering = Inntektsvurdering(
                    inntekter = inntektperioderForSammenligningsgrunnlag {
                        1.januar(2017) til 1.desember(2017) inntekter {
                            a1 inntekt INNTEKT
                        }
                        1.januar(2017) til 1.desember(2017) inntekter {
                            a2 inntekt INNTEKT
                        }
                    },
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList()),
                medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Nei
            )
        )
        historikk(a1)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)

        a1.inspektør.also {
            assertTrue(it.personLogg.hasWarningsOrWorse())
            it.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)
            }
        }
        a2.inspektør.also {
            assertTrue(it.personLogg.hasWarningsOrWorse())
            it.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)
            }
        }

    }

    @Test
    fun `To arbeidsgivere med sykdom gir ikke warning for flere inntekter de siste tre månedene`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2020) til 1.desember(2020) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        assertNoWarnings(a2.inspektør)
        assertNoWarnings(a1.inspektør)
    }

    @Test
    fun `Tar hensyn til forkastede perioder ved beregning av maks dato`() {
        //fom       tom      Arb.   forbrukt Akkumulert     gjenstående
        //19/3/20	19/4/20	 AI     22	     22	            226
        //27/4/20	3/6/20	 AI     28	     50	            198
        //4/6/20	19/7/20	 AS     32	     82	            166	        166
        //10/8/20	21/9/20	 AI     31	     113	        135	        129 -- 20/7 - 27/7 ligger som ferie i speil men sykemelding i infotrygd
        //22/9/20	30/9/20	 AS     7	     120	        128	        122
        //10/11/20	1/2/21	 BI     60	     180	        68	        101
        //2/2/21	18/4/21	 BS     54	     234	        14	        47
        //3/5/21	20/5/21	 BS     14	     248	        0	        32

        val inntektshistorikkA1 = listOf(
            Inntektsopplysning(a1.toString(), 1.januar, INNTEKT, true),
        )
        val inntektshistorikkA2 = listOf(
            Inntektsopplysning(a1.toString(), 1.januar, INNTEKT, true),
            Inntektsopplysning(a2.toString(), 1.juli, INNTEKT, true),
        )
        val ITPeriodeA1 = 1.januar til 31.januar
        val ITPeriodeA2 = 1.juli til 31.juli

        val spleisPeriodeA1 = 1.februar til 28.februar
        val spleisPeriodeA2 = 1.august til 31.august
        val utbetalingerA1 = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1.toString(), ITPeriodeA1.start, ITPeriodeA1.endInclusive, 100.prosent, INNTEKT),
        )
        val utbetalingerA2 = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1.toString(), ITPeriodeA1.start, ITPeriodeA1.endInclusive, 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2.toString(), ITPeriodeA2.start, ITPeriodeA2.endInclusive, 100.prosent, INNTEKT),
        )

        håndterSykmelding(Sykmeldingsperiode(spleisPeriodeA1.start, spleisPeriodeA1.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(spleisPeriodeA1.start, spleisPeriodeA1.endInclusive, 100.prosent), orgnummer = a1)

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalingerA1, inntektshistorikk = inntektshistorikkA1, orgnummer = a1, besvart = LocalDateTime.MIN)
        håndterYtelser(1.vedtaksperiode, *utbetalingerA1, inntektshistorikk = inntektshistorikkA1, orgnummer = a1, besvart = LocalDateTime.MIN)

        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        person.invaliderAllePerioder(inspektør.personLogg, feilmelding = "Feil med vilje")

        håndterSykmelding(Sykmeldingsperiode(spleisPeriodeA2.start, spleisPeriodeA2.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(spleisPeriodeA2.start, spleisPeriodeA2.endInclusive, 100.prosent), orgnummer = a2)

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalingerA2, inntektshistorikk = inntektshistorikkA2, orgnummer = a2, besvart = LocalDateTime.MIN)
        håndterYtelser(1.vedtaksperiode, *utbetalingerA2, inntektshistorikk = inntektshistorikkA2, orgnummer = a2, besvart = LocalDateTime.MIN)

        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        assertEquals(88, inspektør(a2).forbrukteSykedager(0))
    }

    @Test
    fun `to AG - to perioder på hver - siste periode på første AG til godkjenning, siste periode på andre AG avventer første AG`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = 20000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = 20000.månedlig,
            orgnummer = a2
        )

        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 20000.månedlig
                    a2 inntekt 20000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)

        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = a1)
        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = a2)
        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = a1)

        håndterSimulering(2.vedtaksperiode, orgnummer = a1)

        inspektør(a1) {
            assertTilstander(
                1.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_GAP,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
                AVVENTER_ARBEIDSGIVERE,
                AVVENTER_HISTORIKK,
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
                MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
                AVVENTER_HISTORIKK,
                AVVENTER_ARBEIDSGIVERE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING
            )
            assertHasNoErrors()
            assertEquals(1, avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(0, ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(0, avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
            assertEquals(1, ikkeUtbetalteUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }

        inspektør(a2) {
            assertTilstander(
                1.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_GAP,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
                AVVENTER_ARBEIDSGIVERE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
                AVVENTER_HISTORIKK,
                AVVENTER_ARBEIDSGIVERE
            )
            assertHasNoErrors()
            assertEquals(1, avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(0, ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(0, avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
            assertEquals(1, ikkeUtbetalteUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }
    }

    @Test
    fun testyMc() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        inspektør(a1).utbetalinger.forEach {
            assertEquals(a1.toString(), it.inspektør.arbeidsgiverOppdrag.mottaker())
        }
        inspektør(a2).utbetalinger.forEach {
            assertEquals(a2.toString(), it.inspektør.arbeidsgiverOppdrag.mottaker())
        }
    }

    @Test
    fun `Beregning av utbetaling over flere arbeidsgivere hvor en arbeidsgiver ikke har utbetaling`() {
        // Oppretter en arbeidsgiverperiode tilbake i tid som ikke skal ha utbetaling
        håndterSykmelding(Sykmeldingsperiode(1.januar(2017), 16.januar(2017), 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar(2017), 16.januar(2017), 100.prosent), orgnummer = a2)

        assertDoesNotThrow { nyttVedtak(1.januar, 31.januar, orgnummer = a1) }
    }

    @Test
    fun `Går ikke direkte til AVVENTER_HISTORIKK dersom inntektsmelding kommer før søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_ARBEIDSGIVERE, orgnummer = a1)
    }

    @Test
    fun `Siste arbeidsgiver som går til AVVENTER_ARBEIDSGIVERE sparker første tilbake til AVVENTER_HISTORIKK når inntektsmelding kommer før søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_ARBEIDSGIVERE,
            AVVENTER_HISTORIKK,
            orgnummer = a1
        )
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)
    }

    @ForventetFeil("""
        Ønsket oppførsel: arbeidsgiverperiodedag må ha ekte grad (ikke 0%), da den teller med i beregning av total sykdomsgrad,
        som kan slå ut negativt ved flere arbeidsgivere. Skjer eksempelvis dersom man beregner totalgrad av arbeidsgiverperiodedag hos én
        arbeidsgiver og sykedag med 20% sykdom hos en annen arbeidsgiver
        """)
    @Test
    fun `Skal ikke ha noen avviste dager ved ulik startdato selv om arbeidsgiverperiodedag og navdag overlapper og begge har sykdomsgrad på 20 prosent eller høyere`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 20.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(17.januar, 16.februar, 20.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 20.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(17.januar, 16.februar, 20.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterInntektsmelding(listOf(17.januar til 1.februar), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.desember(2017) inntekter {
                        a1 inntekt INNTEKT
                        a2 inntekt INNTEKT
                    }
                }
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntektperioderForSykepengegrunnlag {
                    1.oktober(2017) til 1.desember(2017) inntekter {
                        a1 inntekt INNTEKT
                        a2 inntekt INNTEKT
                    }
                }
            , arbeidsforhold = emptyList())
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertEquals(0, a1.inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistDagTeller)
        assertEquals(0, a2.inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistDagTeller)
    }

    @ForventetFeil("https://trello.com/c/k21yUamv")
    @Test
    fun `Sykmelding og søknad kommer for to perioder før inntektsmelding kommer - skal fortsatt vilkårsprøve kun én gang`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 18.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 18.januar, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(20.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(20.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(22.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 20.januar, orgnummer = a1)
        // Sender med en annen inntekt enn i forrige IM for å kunne asserte på at det er denne vi bruker
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 32000.månedlig, førsteFraværsdag = 22.januar, orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 31000.månedlig,  orgnummer = a2)

        val sammenligningsgrunnlag = Inntektsvurdering(listOf(
            sammenligningsgrunnlag(a1, 20.januar, INNTEKT.repeat(12)),
            sammenligningsgrunnlag(a2, 20.januar, 32000.månedlig.repeat(12))
        ))
        val sykepengegrunnlag = InntektForSykepengegrunnlag(
            inntekter = listOf(
                grunnlag(a1, 20.januar, INNTEKT.repeat(12)),
                grunnlag(a2, 20.januar, INNTEKT.repeat(12))
            ), arbeidsforhold = emptyList()
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = sammenligningsgrunnlag, inntektsvurderingForSykepengegrunnlag = sykepengegrunnlag, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer=a1)
        assertTilstand(2.vedtaksperiode, AVVENTER_ARBEIDSGIVERE, orgnummer=a2)
    }

    @ForventetFeil("https://trello.com/c/ooU4rAQx")
    @Test
    fun `Burde ikke håndtere sykmelding dersom vi har forkastede vedtaksperioder i andre arbeidsforhold`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 1.februar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        assertTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD, orgnummer = a2)
    }
}
