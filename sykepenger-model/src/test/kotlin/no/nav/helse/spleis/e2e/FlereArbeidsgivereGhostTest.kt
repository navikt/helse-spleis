package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.person.Inntektskilde
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FlereArbeidsgivereGhostTest : AbstractEndToEndTest() {
    private companion object {
        private const val a1 = "arbeidsgiver 1"
        private const val a2 = "arbeidsgiver 2"
    }

    @Test
    fun `ghost n stuff`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 1.januar,
                orgnummer = a1,
                refusjon = Refusjon(null, 31000.månedlig, emptyList())
            )

            val inntekter = listOf(
                grunnlag(
                    a1, finnSkjæringstidspunkt(
                        a1, 1.vedtaksperiode(
                            a1
                        )), 31000.månedlig.repeat(3)),
                grunnlag(
                    a2, finnSkjæringstidspunkt(
                        a1, 1.vedtaksperiode(
                            a1
                        )), 32000.månedlig.repeat(3))
            )

            val arbeidsforhold = listOf(
                Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Arbeidsforhold(a2, LocalDate.EPOCH, null)
            )

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1, arbeidsforhold = arbeidsforhold)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(
                            a1, finnSkjæringstidspunkt(
                                a1, 1.vedtaksperiode(
                                    a1
                                )), 31000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(
                            a2, finnSkjæringstidspunkt(
                                a1, 1.vedtaksperiode(
                                    a1
                                )), 32000.månedlig.repeat(12))
                    )
                ), orgnummer = a1
            )
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

            val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
            Assertions.assertEquals(17.januar, a1Linje.fom)
            Assertions.assertEquals(15.mars, a1Linje.tom)
            Assertions.assertEquals(1063, a1Linje.beløp)
            Assertions.assertEquals(
                Inntektskilde.FLERE_ARBEIDSGIVERE,
                inspektør(a1).inntektskilde(1.vedtaksperiode(a1))
            )
        }
    }

    @Test
    fun `Beregner ikke ghost utenfor toggle`() {
        Toggles.FlereArbeidsgivereUlikFom.disable {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 1.januar,
                orgnummer = a1,
                refusjon = Refusjon(null, 60000.månedlig, emptyList())
            )

            val inntekter = listOf(
                grunnlag(
                    a1, finnSkjæringstidspunkt(
                        a1, 1.vedtaksperiode(
                            a1
                        )), 60000.månedlig.repeat(3)),
                grunnlag(
                    a2, finnSkjæringstidspunkt(
                        a1, 1.vedtaksperiode(
                            a1
                        )), 1000.månedlig.repeat(3))
            )

            val arbeidsforhold = listOf(
                Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Arbeidsforhold(a2, LocalDate.EPOCH, null)
            )

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1, arbeidsforhold = arbeidsforhold)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(
                            a1, finnSkjæringstidspunkt(
                                a1, 1.vedtaksperiode(
                                    a1
                                )), 60000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(
                            a2, finnSkjæringstidspunkt(
                                a1, 1.vedtaksperiode(
                                    a1
                                )), 1000.månedlig.repeat(12))
                    )
                ), orgnummer = a1
            )
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

            val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
            Assertions.assertEquals(17.januar, a1Linje.fom)
            Assertions.assertEquals(15.mars, a1Linje.tom)
            Assertions.assertEquals(2161, a1Linje.beløp)

            Assertions.assertFalse(inspektør(a1).warnings.contains("Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold"))
            Assertions.assertFalse(inspektør(a1).warnings.contains("Den sykmeldte har skiftet arbeidsgiver, og det er beregnet at den nye arbeidsgiveren mottar refusjon lik forrige. Kontroller at dagsatsen blir riktig."))
            Assertions.assertEquals(0, inspektør(a2).sykdomshistorikk.size)
        }
    }

    @Test
    fun `Førstegangsbehandling med ghost - skal få warning om flere arbeidsforhold med ulikt sykefravær`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterInntektsmelding(
                listOf(1.mars til 16.mars),
                førsteFraværsdag = 1.mars,
                orgnummer = a1,
                refusjon = Refusjon(null, 10000.månedlig, emptyList())
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
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1, arbeidsforhold = arbeidsforhold)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(
                            a1, finnSkjæringstidspunkt(
                                a1, 1.vedtaksperiode(
                                    a1
                                )), 10000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(
                            a2, finnSkjæringstidspunkt(
                                a1, 1.vedtaksperiode(
                                    a1
                                )), 20000.månedlig.repeat(12))
                    )
                ), orgnummer = a1
            )
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

            assertWarnings(inspektør(a1))
            assertTrue(inspektør(a1).warnings.contains("Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold"))
            assertFalse(inspektør(a1).warnings.contains("Den sykmeldte har skiftet arbeidsgiver, og det er beregnet at den nye arbeidsgiveren mottar refusjon lik forrige. Kontroller at dagsatsen blir riktig."))
            assertEquals(
                Inntektskilde.FLERE_ARBEIDSGIVERE,
                inspektør(a1).inntektskilde(1.vedtaksperiode(a1))
            )
        }
    }

    @Test
    fun `En førstegangsbehandling og et arbeidsforhold som starter etter skjæringstidspunktet - ghostn't (inaktive arbeidsforholdet) skal ikke påvirke beregningen`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 1.januar,
                orgnummer = a1,
                refusjon = Refusjon(null, 31000.månedlig, emptyList())
            )

            val inntekter = listOf(
                grunnlag(
                    a1, finnSkjæringstidspunkt(
                        a1, 1.vedtaksperiode(
                            a1
                        )), 31000.månedlig.repeat(3))
            )

            val arbeidsforhold = listOf(
                Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Arbeidsforhold(a2, 2.januar, null)
            )

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1, arbeidsforhold = arbeidsforhold)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(
                            a1, finnSkjæringstidspunkt(
                                a1, 1.vedtaksperiode(
                                    a1
                                )), 31000.månedlig.repeat(12))
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
            assertEquals(1431, a1Linje.beløp)
            assertEquals(Inntektskilde.EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(1.vedtaksperiode(a1)))

        }
    }

    @Test
    fun `En førstegangsbehandling og et arbeidsforhold som slutter før skjæringstidspunktet - ghostn't (inaktive arbeidsforholdet) skal ikke påvirke beregningen`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 1.januar,
                orgnummer = a1,
                refusjon = Refusjon(null, 31000.månedlig, emptyList())
            )

            val inntekter = listOf(
                grunnlag(
                    a1, finnSkjæringstidspunkt(
                        a1, 1.vedtaksperiode(
                            a1
                        )), 31000.månedlig.repeat(3)),
                grunnlag(
                    a2, finnSkjæringstidspunkt(
                        a1, 1.vedtaksperiode(
                            a1
                        )), 32000.månedlig.repeat(1))
            )

            val arbeidsforhold = listOf(
                Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Arbeidsforhold(a2, 1.desember(2017), 31.desember(2017))
            )

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1, arbeidsforhold = arbeidsforhold)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(
                            a1, finnSkjæringstidspunkt(
                                a1, 1.vedtaksperiode(
                                    a1
                                )), 31000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(
                            a2, finnSkjæringstidspunkt(
                                a1, 1.vedtaksperiode(
                                    a1
                                )), 32000.månedlig.repeat(1))
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
            assertEquals(1431, a1Linje.beløp)
            assertEquals(Inntektskilde.EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(1.vedtaksperiode(a1)))
        }
    }
    @Test
    fun `Ghosts har ikke ubetalinger, men er med i beregningen for utbetaling av arbeidsgiver med sykdom`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

            håndterInntektsmelding(
                listOf(1.mars til 16.mars),
                førsteFraværsdag = 1.mars,
                orgnummer = a1,
                refusjon = Refusjon(null, 30000.månedlig, emptyList())
            )

            val inntekter = listOf(
                grunnlag(
                    a1, finnSkjæringstidspunkt(
                        a1, 1.vedtaksperiode(
                            a1
                        )), 30000.månedlig.repeat(3)),
                grunnlag(
                    a2, finnSkjæringstidspunkt(
                        a1, 1.vedtaksperiode(
                            a1
                        )), 35000.månedlig.repeat(3))
            )

            val arbeidsforhold = listOf(
                Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null),
                Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = null)
            )

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1, arbeidsforhold = arbeidsforhold)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(
                            a1, finnSkjæringstidspunkt(
                                a1, 1.vedtaksperiode(
                                    a1
                                )), 30000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(
                            a2, finnSkjæringstidspunkt(
                                a1, 1.vedtaksperiode(
                                    a1
                                )), 35000.månedlig.repeat(12))
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

            assertTrue(inspektør(a2).utbetalinger.isEmpty())
            assertEquals(Inntektskilde.FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode(
                a1
            )))

        }
    }

    @Test
    fun `spøkelse med varierende grad`() = Toggles.FlereArbeidsgivereUlikFom.enable {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 50.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 50.prosent), orgnummer = a1)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a1,
            refusjon = Refusjon(null, 30000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 35000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null),
            Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = null)
        )

        håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1, arbeidsforhold = arbeidsforhold)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(
                        a1, finnSkjæringstidspunkt(
                            a1, 1.vedtaksperiode(
                                a1
                            )), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(
                        a2, finnSkjæringstidspunkt(
                            a1, 1.vedtaksperiode(
                                a1
                            )), 35000.månedlig.repeat(12))
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
        assertEquals(499, a1Linje.beløp)

        assertTrue(inspektør(a2).utbetalinger.isEmpty())
    }

    @Test
    fun `en forlengelse av et ghost tilfelle vil fortsatt bruke arbeidsdagene for forrige periode`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

            håndterInntektsmelding(
                listOf(1.mars til 16.mars),
                førsteFraværsdag = 1.mars,
                orgnummer = a1,
                refusjon = Refusjon(null, 30000.månedlig, emptyList())
            )

            val inntekter = listOf(
                grunnlag(
                    a1, finnSkjæringstidspunkt(
                        a1, 1.vedtaksperiode(
                            a1
                        )), 30000.månedlig.repeat(3)),
                grunnlag(
                    a2, finnSkjæringstidspunkt(
                        a1, 1.vedtaksperiode(
                            a1
                        )), 35000.månedlig.repeat(3))
            )

            val arbeidsforhold = listOf(
                Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null),
                Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = null)
            )

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1, arbeidsforhold = arbeidsforhold)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(
                            a1, finnSkjæringstidspunkt(
                                a1, 1.vedtaksperiode(
                                    a1
                                )), 30000.månedlig.repeat(12)),
                        sammenligningsgrunnlag(
                            a2, finnSkjæringstidspunkt(
                                a1, 1.vedtaksperiode(
                                    a1
                                )), 35000.månedlig.repeat(12))
                    )
                ), orgnummer = a1
            )

            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

            håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.april, 30.april, 100.prosent), orgnummer = a1)

            håndterUtbetalingsgrunnlag(2.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1, arbeidsforhold = arbeidsforhold)
            håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(2.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(2.vedtaksperiode(a1), orgnummer = a1)

            val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
            assertEquals(17.mars, a1Linje.fom)
            assertEquals(30.april, a1Linje.tom)
            assertEquals(997, a1Linje.beløp)

            assertTrue(inspektør(a2).utbetalinger.isEmpty())
            assertEquals(Inntektskilde.FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode(
                a1
            )))
        }
    }

    @Test
    fun `En ghost som er registrert i Aa-reg mer enn 3 måneder før skjæringstidspunkt og ikke har hatt inntekt de siste 3 månedene før skjæringstidspunktet skal ikke markeres som FLERE_ARBEIDSGIVERE`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterInntektsmelding(
                listOf(1.mars til 16.mars),
                førsteFraværsdag = 1.mars,
                orgnummer = a1,
                refusjon = Refusjon(null, 10000.månedlig, emptyList())
            )
            val inntekter = listOf(grunnlag(
                a1, finnSkjæringstidspunkt(
                    a1, 1.vedtaksperiode(
                        a1
                    )), 10000.månedlig.repeat(3)))
            val arbeidsforhold = listOf(
                Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null),
                Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = null)
            )
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1, arbeidsforhold = arbeidsforhold)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1),
                inntektsvurdering = Inntektsvurdering(listOf(sammenligningsgrunnlag(
                    a1, finnSkjæringstidspunkt(
                        a1, 1.vedtaksperiode(a1)), 10000.månedlig.repeat(12)))),
                orgnummer = a1
            )
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

            assertEquals(Inntektskilde.EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(1.vedtaksperiode(a1)))
        }
    }

    @Test
    fun `bruker har fyllt inn ANDRE_ARBEIDSFORHOLD uten sykmelding i søknad`() = Toggles.FlereArbeidsgivereUlikFom.enable {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent),
            andreInntektskilder = listOf(Søknad.Inntektskilde(false, "ANDRE_ARBEIDSFORHOLD")),
            orgnummer = a1
        )

        assertNoErrors(inspektør(a1))
    }

    @Test
    fun `bruker har fyllt inn ANDRE_ARBEIDSFORHOLD uten sykmelding i søknad uten toggle`() = Toggles.FlereArbeidsgivereUlikFom.disable {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent),
            andreInntektskilder = listOf(Søknad.Inntektskilde(false, "ANDRE_ARBEIDSFORHOLD")),
            orgnummer = a1
        )

        assertErrors(inspektør(a1))
    }

    @Test
    fun `bruker har fyllt inn ANDRE_ARBEIDSFORHOLD med sykmelding i søknad`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)

            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), andreInntektskilder = listOf(Søknad.Inntektskilde(true, "ANDRE_ARBEIDSFORHOLD")), orgnummer = a1)

            assertErrors(inspektør(a1))
        }
    }
}
