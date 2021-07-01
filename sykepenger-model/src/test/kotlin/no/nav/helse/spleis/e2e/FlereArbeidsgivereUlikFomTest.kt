package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class FlereArbeidsgivereUlikFomTest : AbstractEndToEndTest() {
    private companion object {
        private const val a1 = "arbeidsgiver 1"
        private const val a2 = "arbeidsgiver 2"
        private const val a3 = "arbeidsgiver 3"
        private const val a4 = "arbeidsgiver 4"
    }

    @Test
    fun `To førstegangsbehandlinger med ulik fom - skal bruke skatteinntekter for arbeidsgiver med senest fom`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars, 100.prosent), orgnummer = a2)

            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

            håndterInntektsmelding(listOf(1.mars til 16.mars), førsteFraværsdag = 1.mars, orgnummer = a1)
            håndterInntektsmelding(listOf(5.mars til 20.mars), førsteFraværsdag = 5.mars, orgnummer = a2, beregnetInntekt = INNTEKT)

            val inntekter = listOf(
                grunnlag(1.vedtaksperiode(a1), a1, 31000.månedlig.repeat(3)),
                grunnlag(1.vedtaksperiode(a2), a2, 20000.månedlig.repeat(3))
            )
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(1.vedtaksperiode(a1), a1, 31000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(1.vedtaksperiode(a2), a2, 20000.månedlig.repeat(12))
                    )
                ), orgnummer = a1
            )

            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a2), inntekter = inntekter, orgnummer = a2)
            håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)

            assertEquals(31000.månedlig, inspektør(a1).inntektInspektør.grunnlagForSykepengegrunnlag(1.mars))
            assertEquals(20000.månedlig, inspektør(a2).inntektInspektør.grunnlagForSykepengegrunnlag(1.mars))
        }
    }

    @Test
    fun `To førstegangsbehandlinger med lik fom - skal bruke inntektsmelding for begge arbeidsgivere`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
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
                grunnlag(1.vedtaksperiode(a1), a1, 31000.månedlig.repeat(3)),
                grunnlag(1.vedtaksperiode(a2), a2, 20000.månedlig.repeat(3))
            )
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(1.vedtaksperiode(a1), a1, 31000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(1.vedtaksperiode(a2), a2, 20000.månedlig.repeat(12))
                    )
                ), orgnummer = a1
            )

            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a2), inntekter = inntekter, orgnummer = a2)
            håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)

            assertEquals(30000.månedlig, inspektør(a1).inntektInspektør.grunnlagForSykepengegrunnlag(1.mars))
            assertEquals(18000.månedlig, inspektør(a2).inntektInspektør.grunnlagForSykepengegrunnlag(1.mars))

        }
    }

    @Test
    fun `bruker gjennomsnitt av skatteinntekter ved ulik fom`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars, 100.prosent), orgnummer = a2)

            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

            håndterInntektsmelding(listOf(1.mars til 16.mars), førsteFraværsdag = 1.mars, orgnummer = a1)
            håndterInntektsmelding(listOf(5.mars til 20.mars), førsteFraværsdag = 5.mars, orgnummer = a2, beregnetInntekt = INNTEKT)

            val inntekter = listOf(
                grunnlag(1.vedtaksperiode(a1), a1, 31000.månedlig.repeat(3)),
                grunnlag(1.vedtaksperiode(a2), a2, 20000.månedlig.repeat(2) + 23000.månedlig)
            )
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(1.vedtaksperiode(a1), a1, 31000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(1.vedtaksperiode(a2), a2, 20000.månedlig.repeat(11) + 23000.månedlig)
                    )
                ), orgnummer = a1
            )

            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a2), inntekter = inntekter, orgnummer = a2)
            håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)

            assertEquals(31000.månedlig, inspektør(a1).inntektInspektør.grunnlagForSykepengegrunnlag(1.mars))
            assertEquals(21000.månedlig, inspektør(a2).inntektInspektør.grunnlagForSykepengegrunnlag(1.mars))
        }
    }

    @Test
    fun `Ulik fom og ikke 6G-begrenset, utbetalinger beregnes riktig`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
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
                grunnlag(1.vedtaksperiode(a1), a1, 10000.månedlig.repeat(3)),
                grunnlag(1.vedtaksperiode(a2), a2, 20000.månedlig.repeat(3))
            )
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(1.vedtaksperiode(a1), a1, 10000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(1.vedtaksperiode(a2), a2, 20000.månedlig.repeat(12))
                    )
                ), orgnummer = a1
            )

            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

            val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
            assertEquals(17.mars, a1Linje.fom)
            assertEquals(31.mars, a1Linje.tom)
            assertEquals(10000.månedlig.reflection { _, _, _, dagligInt -> dagligInt }, a1Linje.beløp)

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a2), inntekter = inntekter, orgnummer = a2)
            håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)

            håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

            val a2Linje = inspektør(a2).utbetalinger.last().arbeidsgiverOppdrag().single()
            assertEquals(21.mars, a2Linje.fom)
            assertEquals(31.mars, a2Linje.tom)
            assertEquals(20000.månedlig.reflection { _, _, _, dagligInt -> dagligInt }, a2Linje.beløp)
        }
    }

    @Test
    fun `Ulik fom og 6G-begrenset, skal beregne utbetaling ut fra skatteinntekter for a2`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
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
                grunnlag(1.vedtaksperiode(a1), a1, 30000.månedlig.repeat(3)),
                grunnlag(1.vedtaksperiode(a2), a2, 35000.månedlig.repeat(3))
            )
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(1.vedtaksperiode(a1), a1, 30000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(1.vedtaksperiode(a2), a2, 35000.månedlig.repeat(12))
                    )
                ), orgnummer = a1
            )

            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

            val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
            assertEquals(17.mars, a1Linje.fom)
            assertEquals(31.mars, a1Linje.tom)
            assertEquals(997, a1Linje.beløp)

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a2), inntekter = inntekter, orgnummer = a2)
            håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)

            håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

            val a2Linje = inspektør(a2).utbetalinger.last().arbeidsgiverOppdrag().single()
            assertEquals(21.mars, a2Linje.fom)
            assertEquals(31.mars, a2Linje.tom)
            assertEquals(1164, a2Linje.beløp)
        }
    }

    @Test
    fun `Førstegangsbehandling med ulik fom og siste arbeidsgiver er 50% sykemeldt`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
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
                grunnlag(1.vedtaksperiode(a1), a1, 30000.månedlig.repeat(3)),
                grunnlag(1.vedtaksperiode(a2), a2, 35000.månedlig.repeat(3))
            )
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(1.vedtaksperiode(a1), a1, 30000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(1.vedtaksperiode(a2), a2, 35000.månedlig.repeat(12))
                    )
                ), orgnummer = a1
            )
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)


            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a2), inntekter = inntekter, orgnummer = a2)
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
    }

    @Test
    fun `Førstegangsbehandling med ulik fom og første arbeidsgiver er 50% sykemeldt`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
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
                grunnlag(1.vedtaksperiode(a1), a1, 30000.månedlig.repeat(3)),
                grunnlag(1.vedtaksperiode(a2), a2, 35000.månedlig.repeat(3))
            )
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(1.vedtaksperiode(a1), a1, 30000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(1.vedtaksperiode(a2), a2, 35000.månedlig.repeat(12))
                    )
                ), orgnummer = a1
            )
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)


            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a2), inntekter = inntekter, orgnummer = a2)
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
    }

    @Disabled("Ghost")
    @Test
    fun `Fire arbeidsgivere - overlappende perioder med ulik fom`(){
        Toggles.FlereArbeidsgivereUlikFom.enable {

            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

            håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars, 100.prosent), orgnummer = a2)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(2.januar, 15.mars, 100.prosent), orgnummer = a2)

            håndterSykmelding(Sykmeldingsperiode(3.januar, 28.februar, 100.prosent), orgnummer = a3)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.januar, 28.februar, 100.prosent), orgnummer = a3)

            håndterSykmelding(Sykmeldingsperiode(4.januar, 15.februar, 100.prosent), orgnummer = a4)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(4.januar, 15.februar, 100.prosent), orgnummer = a4)

            håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, orgnummer = a1, refusjon = Refusjon(null, 31000.månedlig, emptyList()))
            håndterInntektsmelding(listOf(2.januar til 17.januar), førsteFraværsdag = 2.januar, orgnummer = a2, refusjon = Refusjon(null, 32000.månedlig, emptyList()))
            håndterInntektsmelding(listOf(3.januar til 18.januar), førsteFraværsdag = 3.januar, orgnummer = a3, refusjon = Refusjon(null, 33000.månedlig, emptyList()))
            håndterInntektsmelding(listOf(4.januar til 19.januar), førsteFraværsdag = 4.januar, orgnummer = a4, refusjon = Refusjon(null, 34000.månedlig, emptyList()))

            val inntekter = listOf(
                grunnlag(1.vedtaksperiode(a1), a1, 31000.månedlig.repeat(3)),
                grunnlag(1.vedtaksperiode(a2), a2, 32000.månedlig.repeat(3)),
                grunnlag(1.vedtaksperiode(a3), a3, 33000.månedlig.repeat(3)),
                grunnlag(1.vedtaksperiode(a4), a4, 34000.månedlig.repeat(3))
            )
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(1.vedtaksperiode(a1), a1, 31000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(1.vedtaksperiode(a2), a2, 32000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(1.vedtaksperiode(a3), a3, 33000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(1.vedtaksperiode(a4), a4, 34000.månedlig.repeat(12))
                    )
                ), orgnummer = a1
            )
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a4), inntekter = inntekter, orgnummer = a4)
            håndterYtelser(1.vedtaksperiode(a4), orgnummer = a4,)
            håndterSimulering(1.vedtaksperiode(a4), orgnummer = a4)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a4), orgnummer = a4)
            håndterUtbetalt(1.vedtaksperiode(a4), orgnummer = a4)

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a3), inntekter = inntekter, orgnummer = a3)
            håndterYtelser(1.vedtaksperiode(a3), orgnummer = a3,)
            håndterSimulering(1.vedtaksperiode(a3), orgnummer = a3)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a3), orgnummer = a3)
            håndterUtbetalt(1.vedtaksperiode(a3), orgnummer = a3)

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a2), inntekter = inntekter, orgnummer = a2)
            håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2,)
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

            val a3Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
            assertEquals(19.januar, a3Linje.fom)
            assertEquals(28.februar, a3Linje.tom)
            assertEquals(274, a3Linje.beløp)

            val a4Linje = inspektør(a2).utbetalinger.last().arbeidsgiverOppdrag().single()
            assertEquals(20.januar, a4Linje.fom)
            assertEquals(15.februar, a4Linje.tom)
            assertEquals(565, a4Linje.beløp)


        }
    }

    @Test
    fun `Fire arbeidsgivere - overlappende perioder med ulik fom men lik slutt`(){
        Toggles.FlereArbeidsgivereUlikFom.enable {

            håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)

            håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars, 100.prosent), orgnummer = a2)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(2.januar, 15.mars, 100.prosent), orgnummer = a2)

            håndterSykmelding(Sykmeldingsperiode(3.januar, 15.mars, 100.prosent), orgnummer = a3)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.januar, 15.mars, 100.prosent), orgnummer = a3)

            håndterSykmelding(Sykmeldingsperiode(4.januar, 15.mars, 100.prosent), orgnummer = a4)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(4.januar, 15.mars, 100.prosent), orgnummer = a4)

            håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, orgnummer = a1, refusjon = Refusjon(null, 31000.månedlig, emptyList()))
            håndterInntektsmelding(listOf(2.januar til 17.januar), førsteFraværsdag = 2.januar, orgnummer = a2, refusjon = Refusjon(null, 32000.månedlig, emptyList()))
            håndterInntektsmelding(listOf(3.januar til 18.januar), førsteFraværsdag = 3.januar, orgnummer = a3, refusjon = Refusjon(null, 33000.månedlig, emptyList()))
            håndterInntektsmelding(listOf(4.januar til 19.januar), førsteFraværsdag = 4.januar, orgnummer = a4, refusjon = Refusjon(null, 34000.månedlig, emptyList()))

            val inntekter = listOf(
                grunnlag(1.vedtaksperiode(a1), a1, 31000.månedlig.repeat(3)),
                grunnlag(1.vedtaksperiode(a2), a2, 32000.månedlig.repeat(3)),
                grunnlag(1.vedtaksperiode(a3), a3, 33000.månedlig.repeat(3)),
                grunnlag(1.vedtaksperiode(a4), a4, 34000.månedlig.repeat(3))
            )
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(1.vedtaksperiode(a1), a1, 31000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(1.vedtaksperiode(a2), a2, 32000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(1.vedtaksperiode(a3), a3, 33000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(1.vedtaksperiode(a4), a4, 34000.månedlig.repeat(12))
                    )
                ), orgnummer = a1
            )
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a2), inntekter = inntekter, orgnummer = a2)
            håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2,)
            håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a3), inntekter = inntekter, orgnummer = a3)
            håndterYtelser(1.vedtaksperiode(a3), orgnummer = a3,)
            håndterSimulering(1.vedtaksperiode(a3), orgnummer = a3)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a3), orgnummer = a3)
            håndterUtbetalt(1.vedtaksperiode(a3), orgnummer = a3)

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a4), inntekter = inntekter, orgnummer = a4)
            håndterYtelser(1.vedtaksperiode(a4), orgnummer = a4,)
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
    }

    @Test
    fun `ghost n stuff`(){
            Toggles.FlereArbeidsgivereUlikFom.enable {

                håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
                håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
                håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, orgnummer = a1, refusjon = Refusjon(null, 31000.månedlig, emptyList()))

                val inntekter = listOf(
                    grunnlag(1.vedtaksperiode(a1), a1, 31000.månedlig.repeat(3)),
                    //grunnlag(1.vedtaksperiode(a2), a2, 32000.månedlig.repeat(3))
                )

                håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1)
                håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
                håndterVilkårsgrunnlag(
                    1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                        listOf(
                            sammenligningsgrunnlag(1.vedtaksperiode(a1), a1, 31000.månedlig.repeat(12)),
                            sammenligningsgrunnlag(1.vedtaksperiode(a2), a2, 32000.månedlig.repeat(12))
                        )
                    ), orgnummer = a1
                )
                håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
                håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
                håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
                håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

                val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
                assertEquals(17.januar, a1Linje.fom)
                assertEquals(15.mars, a1Linje.tom)
                assertEquals(1063, a1Linje.beløp)
            }
    }

    @Disabled("Ulik fom med to dager uten sykdom fra ghost")
    @Test
    fun `Ulik fom og 6G-begrenset, to dager med utbetaling hos første arbeidsgiver før andre arbeidsgiver blir syk skal fortsatt 6G-cappe mht begge AG`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
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
                grunnlag(1.vedtaksperiode(a1), a1, 30000.månedlig.repeat(3)),
                grunnlag(1.vedtaksperiode(a2), a2, 35000.månedlig.repeat(3))
            )
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(1.vedtaksperiode(a1), a1, 30000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(1.vedtaksperiode(a2), a2, 35000.månedlig.repeat(12))
                    )
                ), orgnummer = a1
            )

            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

            val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().last()
            assertEquals(17.mars, a1Linje.fom)
            assertEquals(31.mars, a1Linje.tom)
            assertEquals(997, a1Linje.beløp)

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a2), inntekter = inntekter, orgnummer = a2)
            håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)

            håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

            val a2Linje = inspektør(a2).utbetalinger.last().arbeidsgiverOppdrag().last()
            assertEquals(21.mars, a2Linje.fom)
            assertEquals(31.mars, a2Linje.tom)
            assertEquals(1164, a2Linje.beløp)
        }
    }

    @Disabled
    @Test
    fun `To førstegangsbehandlinger med ulik fom - skal få warning om at saksbehandler må sjekke varig lønnsendring`() {
        // TODO: check warning
    }
}
