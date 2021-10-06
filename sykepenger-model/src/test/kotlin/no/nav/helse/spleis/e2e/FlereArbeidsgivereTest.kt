package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.SøknadArbeidsgiver.Sykdom
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.Inntektskilde.FLERE_ARBEIDSGIVERE
import no.nav.helse.person.Periodetype
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.reflection.castAsList
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class FlereArbeidsgivereTest : AbstractEndToEndTest() {
    private companion object {
        private const val a1 = "arbeidsgiver 1"
        private const val a2 = "arbeidsgiver 2"
        private const val a3 = "arbeidsgiver 3"
        private const val a4 = "arbeidsgiver 4"
    }

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
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList()) // TODO ok?
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
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList()) // TODO ok?
        ))

        assertEquals(282500.årlig, person.sammenligningsgrunnlag(2.februar))
    }

    @Test
    @Disabled("Trenger inntekt fra Inntektskomponenten før disse virker (§8-28)")
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
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList()) // TODO ok?
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
    @Disabled("Trenger inntekt fra Inntektskomponenten før disse virker (§8-28)")
    fun `overlappende arbeidsgivere ikke sendt til infotrygd`() {
        gapPeriode(1.januar til 31.januar, a1)
        gapPeriode(15.januar til 15.februar, a2)
        assertNoErrors(a1.inspektør)
        assertNoErrors(a2.inspektør)

        historikk(a1)
        assertNoErrors(a1.inspektør)
        assertNoErrors(a2.inspektør)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_HISTORIKK)
    }

    @Test
    fun `vedtaksperioder atskilt med betydelig tid`() {
        prosessperiode(1.januar til 31.januar, a1)
        assertNoErrors(a1.inspektør)
        assertTilstand(a1, AVSLUTTET)

        prosessperiode(1.mars til 31.mars, a2)
        assertNoErrors(a2.inspektør)
        assertTilstand(a1, AVSLUTTET)
    }

    @Test
    @Disabled("Trenger inntekt fra Inntektskomponenten før disse virker (§8-28)")
    fun `Tre overlappende perioder med en ikke-overlappende periode`() {
        gapPeriode(1.januar til 31.januar, a1)
        gapPeriode(15.januar til 15.mars, a2)
        gapPeriode(1.februar til 28.februar, a3)
        gapPeriode(15.april til 15.mai, a4)

        historikk(a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        assertTilstand(a3, AVVENTER_HISTORIKK)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        historikk(a3)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        assertTilstand(a3, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        historikk(a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        assertTilstand(a3, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        historikk(a2)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a3, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        historikk(a1)
        assertTilstand(a1, AVVENTER_SIMULERING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a3, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        betale(a1)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a3, AVVENTER_HISTORIKK)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        historikk(a3)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a3, AVVENTER_SIMULERING)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        betale(a3)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        assertTilstand(a3, AVSLUTTET)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        historikk(a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_SIMULERING)
        assertTilstand(a3, AVSLUTTET)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        betale(a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
        assertTilstand(a3, AVSLUTTET)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        historikk(a4)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
        assertTilstand(a3, AVSLUTTET)
        assertTilstand(a4, AVVENTER_SIMULERING)

        betale(a4)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
        assertTilstand(a3, AVSLUTTET)
        assertTilstand(a4, AVSLUTTET)
    }

    @Test
    @Disabled("Trenger inntekt fra Inntektskomponenten før disse virker (§8-28)")
    fun `Tre paralelle perioder`() {
        gapPeriode(3.januar til 31.januar, a1)
        gapPeriode(1.januar til 31.januar, a2)
        gapPeriode(2.januar til 31.januar, a3)

        historikk(a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        assertTilstand(a3, AVVENTER_HISTORIKK)

        historikk(a2)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a3, AVVENTER_HISTORIKK)

        historikk(a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a3, AVVENTER_HISTORIKK)

        historikk(a3)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a3, AVVENTER_ARBEIDSGIVERE)

        historikk(a1)
        assertTilstand(a1, AVVENTER_SIMULERING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a3, AVVENTER_ARBEIDSGIVERE)

        betale(a1)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        assertTilstand(a3, AVVENTER_ARBEIDSGIVERE)

        historikk(a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_SIMULERING)
        assertTilstand(a3, AVVENTER_ARBEIDSGIVERE)

        betale(a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
        assertTilstand(a3, AVVENTER_HISTORIKK)

        historikk(a3)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
        assertTilstand(a3, AVVENTER_SIMULERING)

        betale(a3)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
        assertTilstand(a3, AVSLUTTET)
    }

    @Test
    fun `Tillater overgang fra infotrygd for flere arbeidsgivere - a1 kommer til AVVENTER_HISTORIKK først`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_HISTORIKK)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVVENTER_SIMULERING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVVENTER_GODKJENNING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)
        assertEquals("FLERE_ARBEIDSGIVERE", a1.inspektør.sisteBehov(1.vedtaksperiode).detaljer()["inntektskilde"])

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, TIL_UTBETALING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_HISTORIKK)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_SIMULERING)

        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_GODKJENNING)

        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)
        assertEquals("FLERE_ARBEIDSGIVERE", a2.inspektør.sisteBehov(1.vedtaksperiode).detaljer()["inntektskilde"])

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, TIL_UTBETALING)

        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
    }

    @Test
    fun `Tillater overgang fra infotrygd for flere arbeidsgivere - a2 kommer til AVVENTER_HISTORIKK først`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, AVVENTER_HISTORIKK)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVVENTER_SIMULERING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVVENTER_GODKJENNING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, TIL_UTBETALING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_HISTORIKK)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_SIMULERING)

        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_GODKJENNING)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, TIL_UTBETALING)

        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
    }

    @Test
    fun `Tillater ikke flere arbeidsgivere hvis ikke alle er overgang fra Infotrygd`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 25.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, TIL_INFOTRYGD)
        assertTilstand(a2, TIL_INFOTRYGD)
    }

    @Disabled("Det finnes ikke inntekt for skjæringstidspunktet (4. januar)")
    @Test
    fun `Tillater flere arbeidsgivere selv om ikke alle har samme periodetype`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 25.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

        håndterInntektsmelding(listOf(4.januar(2021) til 19.januar(2021)), førsteFraværsdag = 27.januar(2021), orgnummer = a2)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVVENTER_SIMULERING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVVENTER_GODKJENNING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, TIL_UTBETALING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_HISTORIKK)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_SIMULERING)

        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_GODKJENNING)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, TIL_UTBETALING)

        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
    }

    @Test
    fun `Tillater forlengelse av overgang fra infotrygd for flere arbeidsgivere - a1 kommer til AVVENTER_HISTORIKK først`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        //Her starter forlengelsen
        val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_HISTORIKK, 2)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, AVVENTER_HISTORIKK, 2)

        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE, 2)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE, 2)
        assertTilstand(a1, AVVENTER_HISTORIKK, 2)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVVENTER_SIMULERING, 2)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVVENTER_GODKJENNING, 2)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE, 2)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE, 2)

        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, TIL_UTBETALING, 2)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

        håndterUtbetalt(2.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVVENTER_HISTORIKK, 2)

        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVVENTER_SIMULERING, 2)

        håndterSimulering(2.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVVENTER_GODKJENNING, 2)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE, 2)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE, 2)

        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, TIL_UTBETALING, 2)

        håndterUtbetalt(2.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVSLUTTET, 2)
    }

    @Test
    fun `Tillater ikke forlengelse av overgang fra infotrygd for flere arbeidsgivere hvis sykmelding og søknad for en arbeidsgiver kommer før sykmelding for den andre arbeidsgiveren`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        //Her starter forlengelsen
        val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, TIL_INFOTRYGD, 2)

        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, TIL_INFOTRYGD, 2)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, TIL_INFOTRYGD, 2)
        assertTilstand(a2, TIL_INFOTRYGD, 2)
    }

    @Test
    fun `Tillater ikke forlengelse av overgang fra infotrygd for flere arbeidsgivere hvis sykmelding og søknad for en arbeidsgiver kommer før sykmelding for den andre arbeidsgiveren2`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_GODKJENNING, 1)
        assertTilstand(a1, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, 2)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, TIL_INFOTRYGD, 1)
        assertTilstand(a1, TIL_INFOTRYGD, 2)
        assertTilstand(a2, TIL_INFOTRYGD, 1)
    }

    @Test
    fun `Tillater ikke forlengelse av forlengelse av overgang fra infotrygd for flere arbeidsgivere hvis sykmelding og søknad for en arbeidsgiver kommer før sykmelding for den andre arbeidsgiveren`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        //Her starter forlengelsen
        val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        håndterSimulering(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a2)

        //Her starter forlengelsen av forlengelsen
        val forlengelsesforlengelseperiode = 11.februar(2021) til 20.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 3)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a1
        )
        assertTilstand(a1, TIL_INFOTRYGD, 3)

        håndterSykmelding(Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, TIL_INFOTRYGD, 3)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 3)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a2
        )
        assertTilstand(a1, TIL_INFOTRYGD, 3)
        assertTilstand(a2, TIL_INFOTRYGD, 3)
    }

    @Test
    fun `Kaster ut en plutselig tredje arbeidsgiver og passer på at senere forlengelser også forkastes`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        //Her starter forlengelsen
        val forlengelseperiode = 1.februar(2021) til 28.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        håndterSimulering(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a3)
        assertTilstand(a3, TIL_INFOTRYGD)

        //Her starter forlengelsen av forlengelsen
        val forlengelsesforlengelseperiode = 1.mars(2021) til 31.mars(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 3)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 3)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a1
        )
        assertTilstand(a1, TIL_INFOTRYGD, 3)
        assertTilstand(a2, TIL_INFOTRYGD, 3)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a2
        )
        assertTilstand(a1, TIL_INFOTRYGD, 3)
        assertTilstand(a2, TIL_INFOTRYGD, 3)
    }

    @Test
    fun `forlenger forkastet periode med flere arbeidsgivere hvor alle arbeidsgivernes perioder blir forlenget`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        //Her starter forlengelsen
        val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        håndterSimulering(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a3)
        assertTilstand(a3, TIL_INFOTRYGD)

        //Her starter forlengelsen av forlengelsen
        val forlengelsesforlengelseperiode = 11.februar(2021) til 20.februar(2021)
        håndterSykmelding(
            Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a1
        )
        håndterSykmelding(
            Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a2
        )
        håndterSykmelding(
            Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a3
        )
        assertTilstand(a1, TIL_INFOTRYGD, 3)
        assertTilstand(a2, TIL_INFOTRYGD, 3)
        assertTilstand(a3, TIL_INFOTRYGD, 2)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a1
        )
        assertTilstand(a1, TIL_INFOTRYGD, 3)
        assertTilstand(a2, TIL_INFOTRYGD, 3)
        assertTilstand(a3, TIL_INFOTRYGD, 2)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a2
        )
        assertTilstand(a1, TIL_INFOTRYGD, 3)
        assertTilstand(a2, TIL_INFOTRYGD, 3)
        assertTilstand(a3, TIL_INFOTRYGD, 2)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a3
        )
        assertTilstand(a1, TIL_INFOTRYGD, 3)
        assertTilstand(a2, TIL_INFOTRYGD, 3)
        assertTilstand(a3, TIL_INFOTRYGD, 2)
    }

    @Test
    fun `forlenger forkastet periode med flere arbeidsgivere hvor alle arbeidsgivernes perioder blir forlenget 2`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        //Her starter forlengelsen
        val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        håndterSimulering(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a3)
        assertTilstand(a3, TIL_INFOTRYGD)

        //Her starter forlengelsen av forlengelsen
        val forlengelsesforlengelseperiode = 11.februar(2021) til 20.februar(2021)
        håndterSykmelding(
            Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a1
        )
        håndterSykmelding(
            Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a2
        )
        håndterSykmelding(
            Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a3
        )
        assertTilstand(a1, TIL_INFOTRYGD, 3)
        assertTilstand(a2, TIL_INFOTRYGD, 3)
        assertTilstand(a3, TIL_INFOTRYGD, 2)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a1
        )
        assertTilstand(a1, TIL_INFOTRYGD, 3)
        assertTilstand(a2, TIL_INFOTRYGD, 3)
        assertTilstand(a3, TIL_INFOTRYGD, 2)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a2
        )
        assertTilstand(a1, TIL_INFOTRYGD, 3)
        assertTilstand(a2, TIL_INFOTRYGD, 3)
        assertTilstand(a3, TIL_INFOTRYGD, 2)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a3
        )
        assertTilstand(a1, TIL_INFOTRYGD, 3)
        assertTilstand(a2, TIL_INFOTRYGD, 3)
        assertTilstand(a3, TIL_INFOTRYGD, 2)
    }

    @Test
    fun `Håndterer søknader i ikke-kronologisk rekkefølge for flere arbeidsgivere`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)

        val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, 2)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, 2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVVENTER_HISTORIKK, 2)
        assertTilstand(a2, AVVENTER_HISTORIKK, 1)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, 2)

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, AVVENTER_HISTORIKK, 1)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, 2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, AVVENTER_SIMULERING, 1)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, 2)

        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, AVSLUTTET, 1)
        assertTilstand(a2, AVVENTER_HISTORIKK, 2)

        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVVENTER_HISTORIKK, 2)
        assertTilstand(a2, AVSLUTTET, 1)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVVENTER_SIMULERING, 2)
        assertTilstand(a2, AVSLUTTET, 1)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVSLUTTET, 1)
        assertTilstand(a2, AVVENTER_HISTORIKK, 2)

        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        håndterSimulering(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVSLUTTET, 1)
        assertTilstand(a2, AVSLUTTET, 2)

        assertAlleBehovBesvart()
    }

    @Test
    fun `vedtaksperioder i AVVENTER_ARBEIDSGIVERE får også utbetalingstidslinje`() {
        val periode = 1.februar til 28.februar
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, 25.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, 25.februar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar, 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 1.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(
                Inntektsopplysning(a1, 1.januar, INNTEKT, true),
                Inntektsopplysning(a2, 1.januar, INNTEKT, true)
            ),
            orgnummer = a1
        )
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar, 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 1.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(
                Inntektsopplysning(a1, 1.januar, INNTEKT, true),
                Inntektsopplysning(a2, 1.januar, INNTEKT, true)
            ),
            orgnummer = a2
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 1)
        assertTilstand(a2, AVVENTER_SIMULERING, 1)

        //Utbetalingstidslinjen blir like lang som den som går til AvventerGodkjenning
        assertEquals(25, a1.inspektør.utbetalingstidslinjer(1.vedtaksperiode).size)

        assertEquals(25, a2.inspektør.utbetalingstidslinjer(1.vedtaksperiode).size)
    }

    @Test
    fun `Bygger ikke utbetalingstidlinjer for arbeidsgivere med kun forkastede perioder`() {
        val periode = 1.januar til 31.januar
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, 1.januar(2017).atStartOfDay(), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 20.februar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(a2, 1.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(a2, 1.januar, INNTEKT, true)),
            orgnummer = a2
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertEquals(0, a1.inspektør.utbetalingstidslinjer(1.vedtaksperiode).size)
    }

    @Test
    fun `Kaster ut perioder hvis ikke alle forlengelser fra IT har fått sykmeldinger`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)

        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar, 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 1.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(
                Inntektsopplysning(a1, 1.januar, INNTEKT, true),
                Inntektsopplysning(a2, 1.januar, INNTEKT, true)
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD,
            orgnummer = a1
        )
    }

    @Test
    fun `Orgnummer, vedtaksperiodeIder og vedtaksperiodetyper ligger på godkjenningsbehov`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val aktiveVedtaksperioder = inspektør.sisteBehov(Behovtype.Godkjenning).detaljer()["aktiveVedtaksperioder"].castAsList<Map<Any, Any>>()
        assertTrue(
            aktiveVedtaksperioder.containsAll(
                listOf(
                    mapOf("orgnummer" to a1, "vedtaksperiodeId" to 1.vedtaksperiode(a1).toString(), "periodetype" to Periodetype.OVERGANG_FRA_IT.name),
                    mapOf("orgnummer" to a2, "vedtaksperiodeId" to 1.vedtaksperiode(a2).toString(), "periodetype" to Periodetype.OVERGANG_FRA_IT.name)
                )
            )
        )
    }

    @Test
    fun `forkast alle aktive vedtaksperioder for alle arbeidsgivere dersom en periode til godkjenning avises`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, false, a1)

        assertSisteForkastetPeriodeTilstand(a1, 1.vedtaksperiode, TIL_INFOTRYGD)
        assertSisteForkastetPeriodeTilstand(a2, 1.vedtaksperiode, TIL_INFOTRYGD)

    }

    @Test
    fun `forkast ettergølgende vedtaksperioder for alle arbeidsgivere dersom en periode til godkjenning avises`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 15.februar, 100.prosent), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, false, a2)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertSisteForkastetPeriodeTilstand(a2, 1.vedtaksperiode, TIL_INFOTRYGD)
        assertSisteForkastetPeriodeTilstand(a1, 2.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `forkast periode som overlapper med annen arbeidsgiver i infotrygd`() {
        val periode = 22.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)

        assertSisteForkastetPeriodeTilstand(a1, 1.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `Tillater førstegangsbehandling av flere arbeidsgivere der inntekt i inntektsmelding er på samme dato`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

        håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

        håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a2)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        assertTilstand(a1, AVVENTER_VILKÅRSPRØVING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2020) til 1.desember(2020) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INNTEKT
                }
            }
        ))
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        assertTilstand(a1, AVVENTER_SIMULERING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVVENTER_GODKJENNING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, a1)
        assertTilstand(a1, TIL_UTBETALING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_SIMULERING)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_GODKJENNING)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, TIL_UTBETALING)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
    }

    @Test
    fun `Tillater ikke førstegangsbehandling av flere arbeidsgivere der inntekt i inntektsmelding ikke er på samme dato`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
            førsteFraværsdag = 1.januar(2021),
            orgnummer = a1
        )
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar(2021) til 3.januar(2021), 6.januar(2021) til 18.januar(2021)),
            førsteFraværsdag = 6.januar(2021),
            beregnetInntekt = 1000.månedlig,
            orgnummer = a2
        )
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
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
        assertTilstand(a1, TIL_INFOTRYGD)
        assertTilstand(a2, TIL_INFOTRYGD)
    }

    @Test
    fun `Tillater ikke forlengelse av flere arbeidsgivere hvis sykmelding og søknad for en arbeidsgiver kommer før sykmelding for den andre arbeidsgiveren`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
            førsteFraværsdag = 1.januar(2021),
            orgnummer = a1
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
            førsteFraværsdag = 1.januar(2021),
            orgnummer = a2
        )
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
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        //Forlengelsen starter her
        val forlengelseperiode = 1.februar(2021) til 28.februar(2021)
        val sykmeldingHendelseId = UUID.randomUUID()
        val søknadHendelseId = UUID.randomUUID()
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1, id = sykmeldingHendelseId)
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a1,
            id = søknadHendelseId
        )
        assertTilstand(a1, TIL_INFOTRYGD, 2)
        assertHendelseIder(sykmeldingHendelseId, søknadHendelseId, orgnummer = a1)
    }

    @Test
    fun `Tillater forlengelse av flere arbeidsgivere`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
            førsteFraværsdag = 1.januar(2021),
            orgnummer = a1
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
            førsteFraværsdag = 1.januar(2021),
            orgnummer = a2
        )
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
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        //Forlengelsen starter her
        val forlengelseperiode = 1.februar(2021) til 28.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_HISTORIKK, 2)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)

        håndterYtelser(2.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, AVVENTER_HISTORIKK, 2)

        håndterYtelser(2.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)
        assertTilstand(a1, AVVENTER_HISTORIKK, 2)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

        håndterYtelser(2.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        assertTilstand(a1, AVVENTER_SIMULERING, 2)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVVENTER_GODKJENNING, 2)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true, a1)
        assertTilstand(a1, TIL_UTBETALING, 2)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a1)

        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVVENTER_HISTORIKK, 2)
        håndterYtelser(2.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)

        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVVENTER_SIMULERING, 2)
        håndterSimulering(2.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVVENTER_GODKJENNING, 2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true, a2)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, TIL_UTBETALING, 2)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVSLUTTET, 2)
    }

    @Test
    fun `Periode som forlenger annen arbeidsgiver, men ikke seg selv, forkastes`() {
        val periode = 1.februar(2021) til 28.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 1.januar(2021), INNTEKT, true)
        )
        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 1.januar(2021), 31.januar(2021), 100.prosent, INNTEKT)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        val periode2 = 1.mars(2021) til 31.mars(2021)
        håndterSykmelding(Sykmeldingsperiode(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.april(2021), 30.april(2021), 100.prosent), orgnummer = a2)
        assertTilstand(a1, TIL_INFOTRYGD, 2)
        assertTilstand(a2, TIL_INFOTRYGD, 1)
    }

    @Test
    fun `Tillater førstegangsbehandling hos annen arbeidsgiver, hvis gap til foregående`() {
        val periode = 1.februar(2021) til 28.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 1.januar(2021), INNTEKT, true)
        )
        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 1.januar(2021), 31.januar(2021), 100.prosent, INNTEKT)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1, inntektshistorikk = inntektshistorikk)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        val periode2 = 1.mars(2021) til 31.mars(2021)
        val a2Periode = 2.april(2021) til 30.april(2021)
        håndterSykmelding(Sykmeldingsperiode(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(a2Periode.start, a2Periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP, 1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1, inntektshistorikk = inntektshistorikk)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true, a1)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(a2Periode.start, a2Periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(2.april(2021) til 17.april(2021)),
            førsteFraværsdag = 2.april(2021),
            orgnummer = a2
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a2, inntektshistorikk = inntektshistorikk)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a2, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.april(2020) til 1.mars(2021) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INGEN
                }
            }
        ))
        assertNoWarnings(a2.inspektør)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2, inntektshistorikk = inntektshistorikk)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)
    }

    @Test
    fun `Lager ikke utbetalinger for vedtaksperioder hos andre arbeidsgivere som ligger senere i tid enn den som er først totalt sett`() {
        val periode = 1.februar(2021) til 28.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 1.januar(2021), INNTEKT, true)
        )
        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 1.januar(2021), 31.januar(2021), 100.prosent, INNTEKT)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1, inntektshistorikk = inntektshistorikk)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        val periode2 = 1.mars(2021) til 31.mars(2021)
        val a2Periode = 2.april(2021) til 30.april(2021)
        håndterSykmelding(Sykmeldingsperiode(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(a2Periode.start, a2Periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP, 1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1, inntektshistorikk = inntektshistorikk)

        assertEquals(0, inspektør(a2).ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
        assertEquals(0, inspektør(a2).avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
    }

    @Test
    fun `tillater to arbeidsgivere med korte perioder, og forlengelse av disse`() {
        val periode = 1.januar(2021) til 14.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknadArbeidsgiver(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknadArbeidsgiver(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)

        val forlengelseperiode = 15.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
            førsteFraværsdag = 1.januar(2021),
            orgnummer = a1
        )

        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, 2)

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
            førsteFraværsdag = 1.januar(2021),
            orgnummer = a2
        )

        assertTilstand(a1, AVVENTER_HISTORIKK, 2)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

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
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true, a1)

        håndterUtbetalt(2.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVVENTER_HISTORIKK, 2)

        håndterYtelser(2.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)
        håndterSimulering(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true, a2)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a2)

        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVSLUTTET, 2)
    }

    @Test
    fun `går ikke i loop mellom AVVENTER_ARBEIDSGIVERE og AVVENTER_HISTORIKK`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterPåminnelse(1.vedtaksperiode, orgnummer = a1, påminnetTilstand = AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVVENTER_SIMULERING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVVENTER_GODKJENNING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)
        assertEquals("FLERE_ARBEIDSGIVERE", a1.inspektør.sisteBehov(1.vedtaksperiode).detaljer()["inntektskilde"])

        håndterPåminnelse(1.vedtaksperiode, orgnummer = a2, påminnetTilstand = AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a1, AVVENTER_GODKJENNING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, TIL_UTBETALING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_HISTORIKK)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_SIMULERING)

        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_GODKJENNING)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)
        assertEquals("FLERE_ARBEIDSGIVERE", a2.inspektør.sisteBehov(1.vedtaksperiode).detaljer()["inntektskilde"])

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, TIL_UTBETALING)

        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
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
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
                medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Nei
            )
        )
        historikk(a1)

        assertTilstand(a1, AVVENTER_GODKJENNING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        a1.inspektør.also {
            assertTrue(it.personLogg.hasWarningsOrWorse())
            TestTidslinjeInspektør(it.utbetalingstidslinjer(1.vedtaksperiode)).also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.dagtelling[Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag::class])
                assertEquals(4, tidslinjeInspektør.dagtelling[Utbetalingstidslinje.Utbetalingsdag.NavHelgDag::class])
                assertEquals(11, tidslinjeInspektør.dagtelling[Utbetalingstidslinje.Utbetalingsdag.AvvistDag::class])
            }
        }
        a2.inspektør.also {
            assertTrue(it.personLogg.hasWarningsOrWorse())
            TestTidslinjeInspektør(it.utbetalingstidslinjer(1.vedtaksperiode)).also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.dagtelling[Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag::class])
                assertEquals(4, tidslinjeInspektør.dagtelling[Utbetalingstidslinje.Utbetalingsdag.NavHelgDag::class])
                assertEquals(11, tidslinjeInspektør.dagtelling[Utbetalingstidslinje.Utbetalingsdag.AvvistDag::class])
            }
        }

    }

    @Test
    fun `To arbeidsgivere med sykdom gir ikke warning for flere inntekter de siste tre månedene`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
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
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        assertNoWarnings(a2.inspektør)
        assertNoWarnings(a1.inspektør)
    }

    @Test
    fun `En arbeidsgiver får ikke warning hvis vi finner inntekter for flere arbeidsgivere de siste tre månedene`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a1)
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
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        assertNoWarnings(a1.inspektør)
    }

    @Test
    fun `Første arbeidsgiver blir ferdig behandlet før vi mottar sykmelding på neste arbeidsgiver`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2020) til 1.desember(2020) inntekter {
                        a1 inntekt INNTEKT
                        a2 inntekt 1000.månedlig
                    }
                }
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar(2021), INNTEKT.repeat(3)),
                    grunnlag(a2, 1.januar(2021), 1000.månedlig.repeat(3))
                )
            ),
            arbeidsforhold = listOf(Arbeidsforhold(a1, LocalDate.EPOCH, null), Arbeidsforhold(a2, LocalDate.EPOCH, null))
        )
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        assertWarningTekst(
            a1.inspektør,
            "Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold"
        )

        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        assertWarningTekst(
            a1.inspektør,
            "Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold",
            "Denne personen har en utbetaling for samme periode for en annen arbeidsgiver. Kontroller at beregningene for begge arbeidsgiverne er korrekte."
        )
    }

    @Test
    fun `Første arbeidsgiver har blitt sendt til godkjenning før vi mottar sykmelding på neste arbeidsgiver`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2020) til 1.desember(2020) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 1000.månedlig
                }
            }
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar(2021), INNTEKT.repeat(3)),
                    grunnlag(a2, 1.januar(2021), 1000.månedlig.repeat(3))
                )
            ),
            arbeidsforhold = listOf(Arbeidsforhold(a1, LocalDate.EPOCH, null), Arbeidsforhold(a2, LocalDate.EPOCH, null))
        )
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        assertWarningTekst(
            a1.inspektør,
            "Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold"
        )

        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a2)

        assertWarningTekst(
            a1.inspektør,
            "Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold",
            "Denne personen har en utbetaling for samme periode for en annen arbeidsgiver. Kontroller at beregningene for begge arbeidsgiverne er korrekte."
        )
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
            Inntektsopplysning(a1, 1.januar, INNTEKT, true),
        )
        val inntektshistorikkA2 = listOf(
            Inntektsopplysning(a1, 1.januar, INNTEKT, true),
            Inntektsopplysning(a2, 1.juli, INNTEKT, true),
        )
        val ITPeriodeA1 = 1.januar til 31.januar
        val ITPeriodeA2 = 1.juli til 31.juli

        val spleisPeriodeA1 = 1.februar til 28.februar
        val spleisPeriodeA2 = 1.august til 31.august
        val utbetalingerA1 = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, ITPeriodeA1.start, ITPeriodeA1.endInclusive, 100.prosent, INNTEKT),
        )
        val utbetalingerA2 = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, ITPeriodeA1.start, ITPeriodeA1.endInclusive, 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, ITPeriodeA2.start, ITPeriodeA2.endInclusive, 100.prosent, INNTEKT),
        )

        håndterSykmelding(Sykmeldingsperiode(spleisPeriodeA1.start, spleisPeriodeA1.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(spleisPeriodeA1.start, spleisPeriodeA1.endInclusive, 100.prosent), orgnummer = a1)

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalingerA1, inntektshistorikk = inntektshistorikkA1, orgnummer = a1, besvart = LocalDateTime.MIN)
        håndterYtelser(1.vedtaksperiode, *utbetalingerA1, inntektshistorikk = inntektshistorikkA1, orgnummer = a1, besvart = LocalDateTime.MIN)

        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        person.invaliderAllePerioder(inspektør.personLogg, feilmelding = "Feil med vilje")

        håndterSykmelding(Sykmeldingsperiode(spleisPeriodeA2.start, spleisPeriodeA2.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(spleisPeriodeA2.start, spleisPeriodeA2.endInclusive, 100.prosent), orgnummer = a2)

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
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)
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
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)

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
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)
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
            assertEquals(a1, it.arbeidsgiverOppdrag().mottaker())
        }
        inspektør(a2).utbetalinger.forEach {
            assertEquals(a2, it.arbeidsgiverOppdrag().mottaker())
        }
    }

    @Test
    fun `sykepengegrunnlag settes per arbeidsgiver i vedtakfattet-event`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), 1000.00.månedlig, true)
        )

        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 20.januar(2021), 26.januar(2021), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 20.januar(2021), 26.januar(2021), 100.prosent, 1000.00.månedlig)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        assertEquals(31000.00 * 12, observatør.vedtakFattetEvent[1.vedtaksperiode(a1)]?.grunnlagForSykepengegrunnlagPerArbeidsgiver?.get(a1))
        assertEquals(12000.00, observatør.vedtakFattetEvent[1.vedtaksperiode(a1)]?.grunnlagForSykepengegrunnlagPerArbeidsgiver?.get(a2))
    }
}
