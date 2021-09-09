package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FlereArbeidsgivereUlikFomTest : AbstractEndToEndTest() {
    private companion object {
        private const val a1 = "arbeidsgiver 1"
        private const val a2 = "arbeidsgiver 2"
        private const val a3 = "arbeidsgiver 3"
        private const val a4 = "arbeidsgiver 4"
    }

    @Test
    fun `To førstegangsbehandlinger med ulik fom - skal bruke skatteinntekter for arbeidsgiver med senest fom`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.mars til 16.mars), førsteFraværsdag = 1.mars, orgnummer = a1)
        håndterInntektsmelding(listOf(5.mars til 20.mars), førsteFraværsdag = 5.mars, orgnummer = a2, beregnetInntekt = INNTEKT)

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 20000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 20000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)

        assertEquals(31000.månedlig, inspektør(a1).inntektInspektør.grunnlagForSykepengegrunnlag(1.mars))
        assertEquals(20000.månedlig, inspektør(a2).inntektInspektør.grunnlagForSykepengegrunnlag(1.mars))

    }

    @Test
    fun `To førstegangsbehandlinger med lik fom - skal bruke inntektsmelding for begge arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a1,
            refusjon = Refusjon(null, 30000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a2,
            refusjon = Refusjon(null, 18000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 20000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 20000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)

        assertEquals(30000.månedlig, inspektør(a1).inntektInspektør.grunnlagForSykepengegrunnlag(1.mars))
        assertEquals(18000.månedlig, inspektør(a2).inntektInspektør.grunnlagForSykepengegrunnlag(1.mars))

    }


    @Test
    fun `bruker gjennomsnitt av skatteinntekter ved ulik fom`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.mars til 16.mars), førsteFraværsdag = 1.mars, orgnummer = a1)
        håndterInntektsmelding(listOf(5.mars til 20.mars), førsteFraværsdag = 5.mars, orgnummer = a2, beregnetInntekt = INNTEKT)

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 20000.månedlig.repeat(2) + 23000.månedlig)
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 20000.månedlig.repeat(11) + 23000.månedlig)
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)

        assertEquals(31000.månedlig, inspektør(a1).inntektInspektør.grunnlagForSykepengegrunnlag(1.mars))
        assertEquals(21000.månedlig, inspektør(a2).inntektInspektør.grunnlagForSykepengegrunnlag(1.mars))
    }

    @Test
    fun `Ulik fom og ikke 6G-begrenset, utbetalinger beregnes riktig`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a1,
            refusjon = Refusjon(null, 10000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(5.mars til 20.mars),
            førsteFraværsdag = 5.mars,
            orgnummer = a2,
            refusjon = Refusjon(null, 20000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 10000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 20000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 10000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 20000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(17.mars, a1Linje.fom)
        assertEquals(31.mars, a1Linje.tom)
        assertEquals(10000.månedlig.reflection { _, _, _, dagligInt -> dagligInt }, a1Linje.beløp)

        håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)

        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        val a2Linje = inspektør(a2).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(21.mars, a2Linje.fom)
        assertEquals(31.mars, a2Linje.tom)
        assertEquals(20000.månedlig.reflection { _, _, _, dagligInt -> dagligInt }, a2Linje.beløp)

    }

    @Test
    fun `Ulik fom og 6G-begrenset, skal beregne utbetaling ut fra skatteinntekter for a2`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a1,
            refusjon = Refusjon(null, 30000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(5.mars til 20.mars),
            førsteFraværsdag = 5.mars,
            orgnummer = a2,
            refusjon = Refusjon(null, 40000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 35000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 35000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(17.mars, a1Linje.fom)
        assertEquals(31.mars, a1Linje.tom)
        assertEquals(997, a1Linje.beløp)

        håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)

        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        val a2Linje = inspektør(a2).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(21.mars, a2Linje.fom)
        assertEquals(31.mars, a2Linje.tom)
        assertEquals(1164, a2Linje.beløp)

    }

    @Test
    fun `Førstegangsbehandling med ulik fom og siste arbeidsgiver er 50 prosent sykmeldt`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars, 50.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.mars, 31.mars, 50.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a1,
            refusjon = Refusjon(null, 30000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(5.mars til 20.mars),
            førsteFraværsdag = 5.mars,
            orgnummer = a2,
            refusjon = Refusjon(null, 40000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 35000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 35000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)


        håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(17.mars, a1Linje.fom)
        assertEquals(31.mars, a1Linje.tom)
        assertEquals(997, a1Linje.beløp)

        val a2Linje = inspektør(a2).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(21.mars, a2Linje.fom)
        assertEquals(31.mars, a2Linje.tom)
        assertEquals(582, a2Linje.beløp)

    }

    @Test
    fun `Førstegangsbehandling med ulik fom og første arbeidsgiver er 50 prosent sykmeldt`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 50.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 50.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a1,
            refusjon = Refusjon(null, 30000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(5.mars til 20.mars),
            førsteFraværsdag = 5.mars,
            orgnummer = a2,
            refusjon = Refusjon(null, 40000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 35000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 35000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)


        håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(17.mars, a1Linje.fom)
        assertEquals(31.mars, a1Linje.tom)
        assertEquals(499, a1Linje.beløp)

        val a2Linje = inspektør(a2).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(21.mars, a2Linje.fom)
        assertEquals(31.mars, a2Linje.tom)
        assertEquals(1163, a2Linje.beløp)

    }

    @Test
    fun `Fire arbeidsgivere - overlappende perioder med ulik fom`() {

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(2.januar, 15.mars, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(3.januar, 28.februar, 100.prosent), orgnummer = a3)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.januar, 28.februar, 100.prosent), orgnummer = a3)

        håndterSykmelding(Sykmeldingsperiode(4.januar, 15.februar, 100.prosent), orgnummer = a4)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(4.januar, 15.februar, 100.prosent), orgnummer = a4)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            orgnummer = a1,
            refusjon = Refusjon(null, 31000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(2.januar til 17.januar),
            førsteFraværsdag = 2.januar,
            orgnummer = a2,
            refusjon = Refusjon(null, 32000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(3.januar til 18.januar),
            førsteFraværsdag = 3.januar,
            orgnummer = a3,
            refusjon = Refusjon(null, 33000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(4.januar til 19.januar),
            førsteFraværsdag = 4.januar,
            orgnummer = a4,
            refusjon = Refusjon(null, 34000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 32000.månedlig.repeat(3)),
            grunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 33000.månedlig.repeat(3)),
            grunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 34000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode(a1),
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 32000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 33000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 34000.månedlig.repeat(12))
                ),
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        håndterYtelser(1.vedtaksperiode(a4), orgnummer = a4)
        håndterSimulering(1.vedtaksperiode(a4), orgnummer = a4)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a4), orgnummer = a4)
        håndterUtbetalt(1.vedtaksperiode(a4), orgnummer = a4)

        håndterYtelser(1.vedtaksperiode(a3), orgnummer = a3)
        håndterSimulering(1.vedtaksperiode(a3), orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a3), orgnummer = a3)
        håndterUtbetalt(1.vedtaksperiode(a3), orgnummer = a3)

        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(17.januar, a1Linje.fom)
        assertEquals(31.januar, a1Linje.tom)
        assertEquals(515, a1Linje.beløp)

        val a2Linje = inspektør(a2).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(18.januar, a2Linje.fom)
        assertEquals(15.mars, a2Linje.tom)
        assertEquals(532, a2Linje.beløp)

        val a3Linje = inspektør(a3).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(19.januar, a3Linje.fom)
        assertEquals(28.februar, a3Linje.tom)
        assertEquals(549, a3Linje.beløp)

        val a4Linje = inspektør(a4).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(20.januar, a4Linje.fom)
        assertEquals(15.februar, a4Linje.tom)
        assertEquals(565, a4Linje.beløp)

    }

    @Test
    fun `Fire arbeidsgivere - overlappende perioder med ulik fom men lik slutt`() {

        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(2.januar, 15.mars, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.mars, 100.prosent), orgnummer = a3)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.januar, 15.mars, 100.prosent), orgnummer = a3)

        håndterSykmelding(Sykmeldingsperiode(4.januar, 15.mars, 100.prosent), orgnummer = a4)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(4.januar, 15.mars, 100.prosent), orgnummer = a4)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            orgnummer = a1,
            refusjon = Refusjon(null, 31000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(2.januar til 17.januar),
            førsteFraværsdag = 2.januar,
            orgnummer = a2,
            refusjon = Refusjon(null, 32000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(3.januar til 18.januar),
            førsteFraværsdag = 3.januar,
            orgnummer = a3,
            refusjon = Refusjon(null, 33000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(4.januar til 19.januar),
            førsteFraværsdag = 4.januar,
            orgnummer = a4,
            refusjon = Refusjon(null, 34000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 32000.månedlig.repeat(3)),
            grunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 33000.månedlig.repeat(3)),
            grunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 34000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 32000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 33000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 34000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        håndterYtelser(1.vedtaksperiode(a3), orgnummer = a3)
        håndterSimulering(1.vedtaksperiode(a3), orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a3), orgnummer = a3)
        håndterUtbetalt(1.vedtaksperiode(a3), orgnummer = a3)

        håndterYtelser(1.vedtaksperiode(a4), orgnummer = a4)
        håndterSimulering(1.vedtaksperiode(a4), orgnummer = a4)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a4), orgnummer = a4)
        håndterUtbetalt(1.vedtaksperiode(a4), orgnummer = a4)

        val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(17.januar, a1Linje.fom)
        assertEquals(15.mars, a1Linje.tom)
        assertEquals(515, a1Linje.beløp)

        val a2Linje = inspektør(a2).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(18.januar, a2Linje.fom)
        assertEquals(15.mars, a2Linje.tom)
        assertEquals(532, a2Linje.beløp)

        val a3Linje = inspektør(a3).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(19.januar, a3Linje.fom)
        assertEquals(15.mars, a3Linje.tom)
        assertEquals(549, a3Linje.beløp)

        val a4Linje = inspektør(a4).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(20.januar, a4Linje.fom)
        assertEquals(15.mars, a4Linje.tom)
        assertEquals(565, a4Linje.beløp)

    }

    @Test
    fun `Fire arbeidsgivere - overlappende perioder med ulik fom men lik slutt, nå med gradert sykmelding!`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(2.januar, 15.mars, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.mars, 50.prosent), orgnummer = a3)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.januar, 15.mars, 50.prosent), orgnummer = a3)

        håndterSykmelding(Sykmeldingsperiode(4.januar, 15.mars, 100.prosent), orgnummer = a4)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(4.januar, 15.mars, 100.prosent), orgnummer = a4)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            orgnummer = a1,
            refusjon = Refusjon(null, 31000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(2.januar til 17.januar),
            førsteFraværsdag = 2.januar,
            orgnummer = a2,
            refusjon = Refusjon(null, 32000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(3.januar til 18.januar),
            førsteFraværsdag = 3.januar,
            orgnummer = a3,
            refusjon = Refusjon(null, 33000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(4.januar til 19.januar),
            førsteFraværsdag = 4.januar,
            orgnummer = a4,
            refusjon = Refusjon(null, 34000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 32000.månedlig.repeat(3)),
            grunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 33000.månedlig.repeat(3)),
            grunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 34000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 32000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 33000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 34000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        håndterYtelser(1.vedtaksperiode(a3), orgnummer = a3)
        håndterSimulering(1.vedtaksperiode(a3), orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a3), orgnummer = a3)
        håndterUtbetalt(1.vedtaksperiode(a3), orgnummer = a3)

        håndterYtelser(1.vedtaksperiode(a4), orgnummer = a4)
        håndterSimulering(1.vedtaksperiode(a4), orgnummer = a4)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a4), orgnummer = a4)
        håndterUtbetalt(1.vedtaksperiode(a4), orgnummer = a4)

        val a1Linjer = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag()
        assertEquals(17.januar, a1Linjer[0].fom)
        assertEquals(18.januar, a1Linjer[0].tom)
        assertEquals(515, a1Linjer[0].beløp)

        // Siden maksbeløpet blir større når vi får inn flere arbeidsgivere vil vi få en ekstra krone som blir med i kronerulleringen(avrunning), det fører
        // til at vi fordeler den til den arbeidsgiveren som tapte mest på avrunningen
        assertEquals(19.januar, a1Linjer[1].fom)
        assertEquals(15.mars, a1Linjer[1].tom)
        assertEquals(516, a1Linjer[1].beløp)

        val a2Linje = inspektør(a2).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(18.januar, a2Linje.fom)
        assertEquals(15.mars, a2Linje.tom)
        assertEquals(532, a2Linje.beløp)

        val a3Linje = inspektør(a3).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(19.januar, a3Linje.fom)
        assertEquals(15.mars, a3Linje.tom)
        assertEquals(274, a3Linje.beløp)

        val a4Linje = inspektør(a4).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(20.januar, a4Linje.fom)
        assertEquals(15.mars, a4Linje.tom)
        assertEquals(565, a4Linje.beløp)

    }

    @Test
    fun `Wow! Her var det mye greier!!`() {

        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 22.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 15.mars, 22.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars, 69.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(2.januar, 15.mars, 69.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.mars, 42.prosent), orgnummer = a3)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.januar, 15.mars, 42.prosent), orgnummer = a3)

        håndterSykmelding(Sykmeldingsperiode(4.januar, 15.mars, 37.prosent), orgnummer = a4)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(4.januar, 15.mars, 37.prosent), orgnummer = a4)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            orgnummer = a1,
            refusjon = Refusjon(null, 31000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(2.januar til 17.januar),
            førsteFraværsdag = 2.januar,
            orgnummer = a2,
            refusjon = Refusjon(null, 32000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(3.januar til 18.januar),
            førsteFraværsdag = 3.januar,
            orgnummer = a3,
            refusjon = Refusjon(null, 33000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(4.januar til 19.januar),
            førsteFraværsdag = 4.januar,
            orgnummer = a4,
            refusjon = Refusjon(null, 34000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 32000.månedlig.repeat(3)),
            grunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 33000.månedlig.repeat(3)),
            grunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 34000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 32000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 33000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 34000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        håndterYtelser(1.vedtaksperiode(a3), orgnummer = a3)
        håndterSimulering(1.vedtaksperiode(a3), orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a3), orgnummer = a3)
        håndterUtbetalt(1.vedtaksperiode(a3), orgnummer = a3)

        håndterYtelser(1.vedtaksperiode(a4), orgnummer = a4)
        håndterSimulering(1.vedtaksperiode(a4), orgnummer = a4)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a4), orgnummer = a4)
        håndterUtbetalt(1.vedtaksperiode(a4), orgnummer = a4)

        val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(18.januar, a1Linje.fom)
        assertEquals(15.mars, a1Linje.tom)
        assertEquals(113, a1Linje.beløp)

        val a2Linje = inspektør(a2).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(18.januar, a2Linje.fom)
        assertEquals(15.mars, a2Linje.tom)
        assertEquals(367, a2Linje.beløp)

        val a3Linje = inspektør(a3).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(19.januar, a3Linje.fom)
        assertEquals(15.mars, a3Linje.tom)
        assertEquals(231, a3Linje.beløp)

        val a4Linje = inspektør(a4).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(20.januar, a4Linje.fom)
        assertEquals(15.mars, a4Linje.tom)
        assertEquals(209, a4Linje.beløp)

    }

    @Test
    fun `Flere arbeidsgivere med ulik fom - skal få warning om flere arbeidsforhold med ulikt sykefravær`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(4.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(4.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a1,
            refusjon = Refusjon(null, 10000.månedlig, emptyList())
        )

        håndterInntektsmelding(
            listOf(4.mars til 19.mars),
            førsteFraværsdag = 4.mars,
            orgnummer = a2,
            refusjon = Refusjon(null, 19000.månedlig, emptyList())
        )
        val inntekter = listOf(
            grunnlag(
                a1, finnSkjæringstidspunkt(
                    a1, 1.vedtaksperiode(a1)
                ), 10000.månedlig.repeat(3)
            ),
            grunnlag(
                a2, finnSkjæringstidspunkt(
                    a1, 1.vedtaksperiode(a1)
                ), 20000.månedlig.repeat(3)
            )
        )
        val arbeidsforhold = listOf(
            Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null),
            Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = null)
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 10000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 20000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

        assertWarnings(inspektør(a1))
        assertTrue(inspektør(a1).warnings.contains("Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold"))
        assertFalse(inspektør(a1).warnings.contains("Den sykmeldte har skiftet arbeidsgiver, og det er beregnet at den nye arbeidsgiveren mottar refusjon lik forrige. Kontroller at dagsatsen blir riktig."))

    }

    @Test
    fun `Flere arbeidsgivere med lik fom - skal ikke få warning om flere arbeidsforhold med ulikt sykefravær`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a1,
            refusjon = Refusjon(null, 10000.månedlig, emptyList())
        )

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a2,
            refusjon = Refusjon(null, 19000.månedlig, emptyList())
        )
        val inntekter = listOf(
            grunnlag(
                a1, finnSkjæringstidspunkt(
                    a1, 1.vedtaksperiode(a1)
                ), 10000.månedlig.repeat(3)
            ),
            grunnlag(
                a2, finnSkjæringstidspunkt(
                    a1, 1.vedtaksperiode(a1)
                ), 20000.månedlig.repeat(3)
            )
        )
        val arbeidsforhold = listOf(
            Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null),
            Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = null)
        )

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode(a1),
            orgnummer = a1,
            arbeidsforhold = arbeidsforhold,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter)
        )

        assertNoWarnings(inspektør(a1))

    }

    @Test
    fun `Ulik fom og 6G-begrenset, to dager med utbetaling hos første arbeidsgiver før andre arbeidsgiver blir syk skal fortsatt 6G-cappe mht begge AG`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(20.mars, 25.april, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(20.mars, 25.april, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a1,
            refusjon = Refusjon(null, 30000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(20.mars til 4.april),
            førsteFraværsdag = 20.mars,
            orgnummer = a2,
            refusjon = Refusjon(null, 40000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 35000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 35000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().last()
        assertEquals(17.mars, a1Linje.fom)
        assertEquals(31.mars, a1Linje.tom)
        assertEquals(997, a1Linje.beløp)

        håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)

        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        val a2Linje = inspektør(a2).utbetalinger.last().arbeidsgiverOppdrag().last()
        assertEquals(5.april, a2Linje.fom)
        assertEquals(25.april, a2Linje.tom)
        assertEquals(1164, a2Linje.beløp)

    }

    @Test
    fun `Ulik fom og 6G-begrenset, to dager med utbetaling hos første arbeidsgiver før andre arbeidsgiver blir syk skal fortsatt 6G-cappe mht begge AG, nå med gradert sykmelding!`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 50.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 50.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(20.mars, 25.april, 70.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(20.mars, 25.april, 70.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a1,
            refusjon = Refusjon(null, 30000.månedlig, emptyList())
        )
        håndterInntektsmelding(
            listOf(20.mars til 4.april),
            førsteFraværsdag = 20.mars,
            orgnummer = a2,
            refusjon = Refusjon(null, 40000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 35000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 35000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().last()
        assertEquals(17.mars, a1Linje.fom)
        assertEquals(31.mars, a1Linje.tom)
        assertEquals(499, a1Linje.beløp)

        håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)

        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        val a2Linje = inspektør(a2).utbetalinger.last().arbeidsgiverOppdrag().last()
        assertEquals(5.april, a2Linje.fom)
        assertEquals(25.april, a2Linje.tom)
        assertEquals(815, a2Linje.beløp)
    }

}
