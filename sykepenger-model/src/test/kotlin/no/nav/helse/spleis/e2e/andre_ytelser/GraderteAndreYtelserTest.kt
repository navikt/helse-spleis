package no.nav.helse.spleis.e2e.andre_ytelser

import no.nav.helse.dsl.*
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.GraderteAndreYtelserForBeregning.GraderteAndreYtelserForBeregningPeriode
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Arbeidstaker
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Selvstendig
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

internal class GraderteAndreYtelserTest : AbstractDslTest() {
    // @OpenInSpanner
    @Test
    fun `pleiepenger sykt barn`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = 520_000.årlig)
            assertInntektsgrunnlag(1.januar, 1) {
                assertInntektsgrunnlag(a1, 520_000.årlig)
                assertSykepengegrunnlag(520_000.årlig)
            }
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 17.januar til 31.januar)
            håndterGraderteAndreYtelserEndret(20.januar)
            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        20.januar,
                                        30.januar,
                                        30,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                    ),
            )

            // Jeg er bare et regnestykke for å vise hvorfor det blir 1400,-
            assertEquals(1400.0, (2000 - (520_000 * 0.3) / 260))

            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1400, 2000, subset = 20.januar til 30.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 31.januar til 31.januar)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertEquals(
                70.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar),
            )
            assertEquals(
                70.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar),
            )
        }
    }

    @Test
    fun `pleiepenger sykt barn, også øker sykepengegrunnlaget`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = 520_000.årlig)
            assertInntektsgrunnlag(1.januar, 1) {
                assertInntektsgrunnlag(a1, 520_000.årlig)
                assertSykepengegrunnlag(520_000.årlig)
            }
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 17.januar til 31.januar)
            håndterGraderteAndreYtelserEndret(20.januar)

            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        20.januar,
                                        30.januar,
                                        30,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                    ),
            )

            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1400, 2000, subset = 20.januar til 30.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 31.januar til 31.januar)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())

            håndterKorrigerteArbeidsgiveropplysninger(listOf(1.januar til 16.januar), beregnetInntekt = 533_000.årlig)
            assertInntektsgrunnlag(1.januar, 1) {
                assertInntektsgrunnlag(a1, 533_000.årlig)
                assertSykepengegrunnlag(533_000.årlig)
            }

            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        20.januar,
                                        30.januar,
                                        30,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                    ),
            )

            // Jeg er bare et regnestykke for å vise hvorfor det blir 1435,-
            assertEquals(1435.0, (2050 - (533_000 * 0.3) / 260))
            assertUtbetalingsbeløp(1.vedtaksperiode, 2050, 2050, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1435, 2050, subset = 20.januar til 30.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 2050, 2050, subset = 31.januar til 31.januar)

            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `annen ytelse for person med to arbeidsforhold, men under 6G til sammen`() {
        val inntektPerArbeidsgiver = 260_000.årlig
        val gradertePleiepenger =
            listOf(a1, a2).map { orgnr ->
                GraderteAndreYtelserForBeregning(
                    graderteAndreYtelserForBeregningPeriodeList =
                        listOf(
                            GraderteAndreYtelserForBeregningPeriode(
                                20.januar,
                                30.januar,
                                30,
                            ),
                        ),
                    graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
                    yrkesaktivitet = Arbeidstaker(orgnr),
                )
            }

        listOf(a1, a2).nyeVedtak(januar, inntekt = inntektPerArbeidsgiver)

        a1 {
            assertInntektsgrunnlag(1.januar, 2) {
                assertInntektsgrunnlag(a1, inntektPerArbeidsgiver)
                assertInntektsgrunnlag(a2, inntektPerArbeidsgiver)
                assertSykepengegrunnlag(520_000.årlig)
            }
            assertUtbetalingsbeløp(1.vedtaksperiode, 1000, 1000, subset = 17.januar til 31.januar)
        }
        a2 {
            assertUtbetalingsbeløp(1.vedtaksperiode, 1000, 1000, subset = 17.januar til 31.januar)
        }

        a1 {
            håndterGraderteAndreYtelserEndret(20.januar)
            håndterYtelser(1.vedtaksperiode, graderteAndreYtelser = gradertePleiepenger)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt(Oppdragstatus.AKSEPTERT)
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)

            assertEquals(700.0, (1000 - (260_000 * 0.3) / 260))
            assertUtbetalingsbeløp(1.vedtaksperiode, 1000, 1000, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 700, 1000, subset = 20.januar til 30.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1000, 1000, subset = 31.januar til 31.januar)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertEquals(100, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.grad(22.januar))
            assertEquals(
                70.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar),
            )
            assertEquals(
                70.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar),
            )
        }
        a2 {
            håndterYtelser(1.vedtaksperiode, graderteAndreYtelser = gradertePleiepenger)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt(Oppdragstatus.AKSEPTERT)
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)

            assertUtbetalingsbeløp(1.vedtaksperiode, 1000, 1000, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 700, 1000, subset = 20.januar til 30.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1000, 1000, subset = 31.januar til 31.januar)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertEquals(100, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.grad(22.januar))
            assertEquals(
                70.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar),
            )
            assertEquals(
                70.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar),
            )
        }
    }

    @Test
    fun `annen ytelse for person med to arbeidsforhold, og 6G-begrenset til sammen`() {
        val inntektA1 = 400_000.årlig
        val inntektA2 = 600_000.årlig
        val gradertePleiepenger =
            listOf(a1, a2).map { orgnr ->
                GraderteAndreYtelserForBeregning(
                    graderteAndreYtelserForBeregningPeriodeList =
                        listOf(
                            GraderteAndreYtelserForBeregningPeriode(
                                20.januar,
                                30.januar,
                                30,
                            ),
                        ),
                    graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
                    yrkesaktivitet = Arbeidstaker(orgnr),
                )
            }

        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), beregnetInntekt = inntektA1)
        }
        a2 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), beregnetInntekt = inntektA2)
        }

        a1 {
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to inntektA1, a2 to inntektA2),
            )
            assertInntektsgrunnlag(1.januar, 2) {
                assertInntektsgrunnlag(a1, inntektA1)
                assertInntektsgrunnlag(a2, inntektA2)
                assertSykepengegrunnlag(561_804.årlig)
            }
        }

        a1 {
            håndterGraderteAndreYtelserEndret(20.januar)
            håndterYtelser(1.vedtaksperiode, graderteAndreYtelser = gradertePleiepenger)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt(Oppdragstatus.AKSEPTERT)
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)

            assertUtbetalingsbeløp(1.vedtaksperiode, 864, 1538, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 605, 1538, subset = 20.januar til 30.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 864, 1538, subset = 31.januar til 31.januar)
            assertEquals(100, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.grad(22.januar))
            assertEquals(
                70.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar),
            )
            assertEquals(
                70.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar),
            )
        }
        a2 {
            håndterYtelser(1.vedtaksperiode, graderteAndreYtelser = gradertePleiepenger)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt(Oppdragstatus.AKSEPTERT)
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)

            assertUtbetalingsbeløp(1.vedtaksperiode, 1296, 2308, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 908, 2308, subset = 20.januar til 30.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1296, 2308, subset = 31.januar til 31.januar)
            assertEquals(100, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.grad(22.januar))
            assertEquals(
                70.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar),
            )
            assertEquals(
                70.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar),
            )
        }
    }

    @Test
    fun `ett arbeidsforhold, foreldrepenger og tilkommen inntekt`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = 520_000.årlig)
            assertInntektsgrunnlag(1.januar, 1) {
                assertInntektsgrunnlag(a1, 520_000.årlig)
                assertSykepengegrunnlag(520_000.årlig)
            }
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 17.januar til 31.januar)
            håndterGraderteAndreYtelserEndret(20.januar)

            håndterYtelser(
                1.vedtaksperiode,
                inntekterForBeregning =
                    listOf(
                        InntekterForBeregning.Inntektsperiode(
                            a2,
                            1.januar til 30.januar,
                            250.daglig,
                        ),
                    ),
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        1.januar,
                                        29.januar,
                                        50,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                    ),
            )

            // Jeg er bare et regnestykke for å vise hvorfor det blir 750,-
            assertEquals(750.0, (2000 - (520_000 * 0.5) / 260) - 250)

            // Jeg er bare et regnestykke for å vise hvorfor det blir 1750,-
            assertEquals(1750, (2000 - 250))

            assertUtbetalingsbeløp(1.vedtaksperiode, 750, 2000, subset = 17.januar til 29.januar) // begge
            assertUtbetalingsbeløp(1.vedtaksperiode, 1750, 2000, subset = 30.januar til 30.januar) // tilkommen
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 31.januar til 31.januar) // ingen

            assertEquals(
                37.5.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar),
            ) // begge

            assertEquals(
                87.5.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(30.januar),
            ) // tilkommen

            assertEquals(
                100.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(31.januar),
            ) // ingen

            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `så mye foreldrepenger at du havner under 20 prosent avslår vi på total grad`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = 520_000.årlig)
            assertInntektsgrunnlag(1.januar, 1) {
                assertInntektsgrunnlag(a1, 520_000.årlig)
                assertSykepengegrunnlag(520_000.årlig)
            }
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 17.januar til 31.januar)
            håndterGraderteAndreYtelserEndret(20.januar)

            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        1.januar,
                                        31.januar,
                                        81,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                    ),
            )

            // Alt er avvist, under 20 prosent
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 2000, subset = 17.januar til 31.januar)

            assertEquals(
                11,
                inspektør(a1)
                    .utbetalingstidslinjer(1.vedtaksperiode)
                    .inspektør.avvistedager.size,
            )

            assertEquals(
                0.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar),
            )
            assertEquals(
                19.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar),
            )

            assertVarsler(1.vedtaksperiode, Varselkode.RV_UT_23, Varselkode.RV_VV_4)
        }
    }

    @Test
    fun `gradert sykmeldt og graderte foreldrepenger trekkes direkte fra sykdomsgraden`() {
        a1 {
            nyttVedtak(1.juni(2025) til 30.juni(2025), beregnetInntekt = 2_000_000.årlig, grad = 50.prosent)
            assertInntektsgrunnlag(1.juni(2025), 1) {
                assertInntektsgrunnlag(a1, 2_000_000.årlig)
                assertSykepengegrunnlag(780_960.årlig)
            }
            assertUtbetalingsbeløp(1.vedtaksperiode, 1502, 7692, subset = 17.juni(2025) til 30.juni(2025))

            håndterGraderteAndreYtelserEndret(1.juni(2025))
            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        1.juni(2025),
                                        30.juni(2025),
                                        50,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                    ),
            )

            // Graderingen trekkes direkte fra sykdomsgraden, uten hensyn til at personen er i arbeid de resterende 50 %: 50 % - 50 % = 0 %
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 7692, subset = 17.juni(2025) til 30.juni(2025))
            assertEquals(
                0.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(17.juni(2025)),
            )
            assertEquals(
                10,
                inspektør
                    .utbetalingstidslinjer(1.vedtaksperiode)
                    .inspektør.avvistedager.size,
            )
            assertVarsler(1.vedtaksperiode, Varselkode.RV_UT_23, Varselkode.RV_VV_4)
        }
    }

    @Test
    fun `mange andre ytelser som overlapper litt om hverandre`() {
        a1 {
            nyttVedtak(januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)
            håndterGraderteAndreYtelserEndret(1.januar)

            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        1.januar,
                                        28.januar,
                                        20,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        17.januar,
                                        22.januar,
                                        20,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.OMSORGSPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        22.januar,
                                        22.januar,
                                        15,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        22.januar,
                                        30.januar,
                                        20,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.OPPLARINGSPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                    ),
            )

            assertUtbetalingsbeløp(1.vedtaksperiode, 858, 1431, subset = 17.januar til 21.januar) // 1431 * 0,60 =  858
            assertUtbetalingsbeløp(1.vedtaksperiode, 358, 1431, subset = 22.januar.somPeriode()) // 1431 * 0,25 =  358
            assertUtbetalingsbeløp(1.vedtaksperiode, 858, 1431, subset = 23.januar til 28.januar) // 1431 * 0,60 =  858
            assertUtbetalingsbeløp(1.vedtaksperiode, 1145, 1431, subset = 29.januar til 30.januar) // 1431 * 0,80 = 1145
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 31.januar.somPeriode()) // 1431 * 1,00 = 1431

            assertVarsler(1.vedtaksperiode, Varselkode.RV_UT_23)
        }
    }

    @Test
    fun `annen ytelse graderes kun mot yrkesaktiviteten den gjelder for`() {
        val inntektPerArbeidsgiver = 260_000.årlig
        val gradertePleiepengerMotA1 =
            listOf(
                GraderteAndreYtelserForBeregning(
                    graderteAndreYtelserForBeregningPeriodeList =
                        listOf(
                            GraderteAndreYtelserForBeregningPeriode(
                                20.januar,
                                30.januar,
                                30,
                            ),
                        ),
                    graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
                    yrkesaktivitet = Arbeidstaker(a1),
                ),
            )

        listOf(a1, a2).nyeVedtak(januar, inntekt = inntektPerArbeidsgiver)

        a1 {
            håndterGraderteAndreYtelserEndret(20.januar)
            håndterYtelser(1.vedtaksperiode, graderteAndreYtelser = gradertePleiepengerMotA1)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt(Oppdragstatus.AKSEPTERT)
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)

            // a1AndelAvSykepengegrunnlaget: 50 %
            // a2AndelAvSykepengegrunnlaget: 50 %
            // a1: 100 % - 30 % = 70 %
            // a2: 100 %
            // total sykdomsgrad: (70 % * a1AndelAvTotalgrad + 100 % * a2AndelAvTotalgrad) = 85 %
            assertUtbetalingsbeløp(1.vedtaksperiode, 1000, 1000, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 700, 1000, subset = 20.januar til 30.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1000, 1000, subset = 31.januar til 31.januar)
            assertEquals(100, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.grad(22.januar))
            assertEquals(
                85.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar),
            )
            assertEquals(
                70.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar),
            )
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        }

        a2 {
            håndterYtelser(1.vedtaksperiode, graderteAndreYtelser = gradertePleiepengerMotA1)
//            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
//            håndterUtbetalt(Oppdragstatus.AKSEPTERT)
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)

            assertUtbetalingsbeløp(1.vedtaksperiode, 1000, 1000, subset = 17.januar til 31.januar)
            assertEquals(100, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.grad(22.januar))
            assertEquals(
                85.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar),
            )
            assertEquals(
                100.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar),
            )
        }
    }

    @Test
    fun `to arbeidsforhold, begge gradert likt med annen ytelse`() {
        val inntektPerArbeidsgiver = 260_000.årlig
        val gradertePleiepengerMotA1 =
            listOf(
                GraderteAndreYtelserForBeregning(
                    graderteAndreYtelserForBeregningPeriodeList =
                        listOf(
                            GraderteAndreYtelserForBeregningPeriode(
                                20.januar,
                                30.januar,
                                15,
                            ),
                        ),
                    graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
                    yrkesaktivitet = Arbeidstaker(a1),
                ),
                GraderteAndreYtelserForBeregning(
                    graderteAndreYtelserForBeregningPeriodeList =
                        listOf(
                            GraderteAndreYtelserForBeregningPeriode(
                                20.januar,
                                30.januar,
                                15,
                            ),
                        ),
                    graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
                    yrkesaktivitet = Arbeidstaker(a2),
                ),
            )

        listOf(a1, a2).nyeVedtak(januar, inntekt = inntektPerArbeidsgiver)

        a1 {
            håndterGraderteAndreYtelserEndret(20.januar)
            håndterYtelser(1.vedtaksperiode, graderteAndreYtelser = gradertePleiepengerMotA1)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt(Oppdragstatus.AKSEPTERT)
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)

            // a1AndelAvTotalgrad: 50 %
            // a2AndelAvTotalgrad: 50 %
            // a1: 100 % - 15 % = 85 %
            // a2: 100 % - 15 % = 85 %
            // total sykdomsgrad: (85 % * a1AndelAvTotalgrad + 85 % * a2AndelAvTotalgrad) => 85 % / 100 % = 85 %
            assertUtbetalingsbeløp(1.vedtaksperiode, 1000, 1000, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 850, 1000, subset = 20.januar til 30.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1000, 1000, subset = 31.januar til 31.januar)
            assertEquals(100, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.grad(22.januar))
            assertEquals(
                85.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar),
            )
            assertEquals(
                85.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar),
            )
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        }
        a2 {
            håndterYtelser(1.vedtaksperiode, graderteAndreYtelser = gradertePleiepengerMotA1)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt(Oppdragstatus.AKSEPTERT)
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)

            assertUtbetalingsbeløp(1.vedtaksperiode, 1000, 1000, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 850, 1000, subset = 20.januar til 30.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1000, 1000, subset = 31.januar til 31.januar)
            assertEquals(100, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.grad(22.januar))
            assertEquals(
                85.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar),
            )
            assertEquals(
                85.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar),
            )
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `annen ytelse mot yrkesaktivitet som ikke er med i beregningen ignoreres`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = 520_000.årlig)
            håndterGraderteAndreYtelserEndret(20.januar)
            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        20.januar,
                                        30.januar,
                                        30,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
                            yrkesaktivitet = Arbeidstaker(a2),
                        ),
                    ),
            )

            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 17.januar til 31.januar)
            assertEquals(
                100.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar),
            )
        }
    }

    @Test
    fun `annen ytelse graderes mot selvstendig næringsdrivende`() {
        // TODO: Utvide med beløp
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        1.januar,
                                        31.januar,
                                        40,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
                            yrkesaktivitet = Selvstendig,
                        ),
                    ),
            )

            assertEquals(
                60.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar),
            )
            assertEquals(
                60.prosent,
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar),
            )
        }
    }

    @Test
    fun `fordeling av refusjon`() {
        // TODO: Test med coacher
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        }

        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 500.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 500.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
                refusjon = Inntektsmelding.Refusjon(Inntekt.INGEN, null),
            )
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterGraderteAndreYtelserEndret(17.januar)
            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        17.januar,
                                        22.januar,
                                        50,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.OMSORGSPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                    ),
            )
            assertUtbetalingsbeløp(1.vedtaksperiode, 250, 500, 0, subset = 17.januar til 22.januar)

            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a2 {
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 0, 500, subset = 17.januar til 22.januar)
        }
    }

    @Test
    fun `fordeling av delvis refusjon - arbeidsgivere burde altid etterspørre full refusjon`() {
        // TODO: Test med coacher
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        }

        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 500.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
                refusjon = Inntektsmelding.Refusjon(250.daglig, null),
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 500.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
                refusjon = Inntektsmelding.Refusjon(Inntekt.INGEN, null),
            )
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterGraderteAndreYtelserEndret(17.januar)
            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        17.januar,
                                        22.januar,
                                        50,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.OMSORGSPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                    ),
            )
            assertUtbetalingsbeløp(1.vedtaksperiode, 125, 250, 125, subset = 17.januar til 22.januar)

            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a2 {
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 0, 500, subset = 17.januar til 22.januar)
        }
    }

    @Test
    fun `fordeling av refusjon over 6G`() {
        // TODO: Test med coacher
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        }

        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 1000.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 1500.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterGraderteAndreYtelserEndret(17.januar)
            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        17.januar,
                                        22.januar,
                                        50,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.OMSORGSPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                    ),
            )
            // 2160 =~ 6G.daglig
            // 1000 + 1500 = 2500
            // a1 andel av totale inntekter på skjæringstidspunktet: 1000/2500 = 0.4
            // a2 andel av totale inntekter på skjæringstidspunktet: 1500/2500 = 0.6
            // a1: 2160*0.4 = 864 => med gradering på 50% 2160*0.4*(1-0.5) => 432
            // a2: 2160*0.6 = 1296
            // arbeidsgiverbeløp_n = minOf(6G.daglig,totale_inntker) * (inntekt_n / totale_inntker) * (sykdomsgrad-gradertGrad)

            assertUtbetalingsbeløp(1.vedtaksperiode, 432, 1000, 0, subset = 17.januar til 22.januar)
        }

        a2 {
            assertUtbetalingsbeløp(1.vedtaksperiode, 1296, 1500, 0, subset = 17.januar til 22.januar)
        }
    }

    @Test
    fun `fordeling av refusjon under 6G og begge har graderte andre ytelser`() {
        // TODO: Test med coacher
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        }

        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 500.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 1000.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterGraderteAndreYtelserEndret(17.januar)
            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        17.januar,
                                        22.januar,
                                        60,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.OMSORGSPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        17.januar,
                                        22.januar,
                                        60,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
                            yrkesaktivitet = Arbeidstaker(a2),
                        ),
                    ),
            )
            // 2160 =~ 6G.daglig
            // 500 + 1000 = 1500
            // a1 andel av totale inntekter på skjæringstidspunktet: 500/1500 = 1/3
            // a2 andel av totale inntekter på skjæringstidspunktet: 1000/1500 = 2/3
            // a1: 1500*(1/3)*(1-0.6) = 200
            // a2: 1500*(2/3)*(1-0.6) = 400
            // arbeidsgiverbeløp_n = minOf(6G.daglig,totale_inntker) * (inntekt_n / totale_inntker) * (sykdomsgrad-gradertGrad)

            assertUtbetalingsbeløp(1.vedtaksperiode, 200, 500, 0, subset = 17.januar til 22.januar)
        }

        a2 {
            assertUtbetalingsbeløp(1.vedtaksperiode, 400, 1000, 0, subset = 17.januar til 22.januar)
        }
    }

    @Test
    fun `fordeling av refusjon over 6G og begge har graderte andre ytelser`() {
        // TODO: Test med coacher
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        }

        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 1000.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 1500.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterGraderteAndreYtelserEndret(17.januar)
            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        17.januar,
                                        22.januar,
                                        60,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.OMSORGSPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        17.januar,
                                        22.januar,
                                        60,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
                            yrkesaktivitet = Arbeidstaker(a2),
                        ),
                    ),
            )
            // 2160 =~ 6G.daglig
            // 500 + 1000 = 1500
            // a1 andel av totale inntekter på skjæringstidspunktet: 1000/2500 = 2/5
            // a2 andel av totale inntekter på skjæringstidspunktet: 1500/2500 = 3/5
            // a1: 2160*(2/5)*(1-0.6) = 345.6
            // a2: 2160*(3/5)*(1-0.6) = 518.4
            // arbeidsgiverbeløp_n = minOf(6G.daglig,totale_inntker) * (inntekt_n / totale_inntker) * (sykdomsgrad-gradertGrad)

            assertUtbetalingsbeløp(1.vedtaksperiode, 346, 1000, 0, subset = 17.januar til 22.januar)
        }

        a2 {
            assertUtbetalingsbeløp(1.vedtaksperiode, 519, 1500, 0, subset = 17.januar til 22.januar)
        }
    }

    @Test
    fun `fordeling av refusjon under 6G og begge har graderte andre ytelser + tilkommen`() {
        // TODO: Test med coacher
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        }

        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 500.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 1000.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterGraderteAndreYtelserEndret(17.januar)
            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        17.januar,
                                        22.januar,
                                        60,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.OMSORGSPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        17.januar,
                                        22.januar,
                                        60,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
                            yrkesaktivitet = Arbeidstaker(a2),
                        ),
                    ),
                inntekterForBeregning =
                    listOf(
                        InntekterForBeregning.Inntektsperiode(
                            "a3",
                            januar,
                            200.daglig,
                        ),
                    ),
            )
            // 2160 =~ 6G.daglig
            // 500 + 1000 = 1500
            // a1 andel av totale inntekter på skjæringstidspunktet: 500/1500 = 1/3
            // a2 andel av totale inntekter på skjæringstidspunktet: 1000/1500 = 2/3
            // andelTilkommenInntekt: 200/1500
            // a1: 1500*(1/3)*(1-0.6-200/1500) = 133.33 =~ 133
            // a2: 1500*(2/3)*(1-0.6-200/1500) = 266.66 =~ 267
            // arbeidsgiverbeløp_n = minOf(6G.daglig,totale_inntker) * (inntekt_n / totale_inntker) * (sykdomsgrad - gradertGrad - tilkommeninntekt/totalInntekter)

            assertUtbetalingsbeløp(1.vedtaksperiode, 133, 500, 0, subset = 17.januar til 22.januar)
        }

        a2 {
            assertUtbetalingsbeløp(1.vedtaksperiode, 267, 1000, 0, subset = 17.januar til 22.januar)
        }
    }

    @Test
    fun `fordeling av refusjon over 6G og begge har graderte andre ytelser + tilkommen inntekt`() {
        // TODO: Test med coacher
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        }

        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 1000.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 1500.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterGraderteAndreYtelserEndret(17.januar)
            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        17.januar,
                                        22.januar,
                                        60,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.OMSORGSPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        17.januar,
                                        22.januar,
                                        60,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
                            yrkesaktivitet = Arbeidstaker(a2),
                        ),
                    ),
                inntekterForBeregning =
                    listOf(
                        InntekterForBeregning.Inntektsperiode(
                            "a3",
                            januar,
                            200.daglig,
                        ),
                    ),
            )
            // 2160 =~ 6G.daglig
            // 500 + 1000 = 1500
            // a1 andel av totale inntekter på skjæringstidspunktet: 1000/2500 = 2/5
            // a2 andel av totale inntekter på skjæringstidspunktet: 1500/2500 = 3/5
            // andel tilkommenInntekt: 200 / 2500
            // a1: 2160*(2/5)*(1-0.6-200/2500) = 276.48
            // a2: 2160*(3/5)*(1-0.6-200/2500) = 414.72
            // arbeidsgiverbeløp_n = minOf(6G.daglig,totale_inntker) * (inntekt_n / totale_inntker) * (sykdomsgrad - gradertGrad - tilkommeninntekt/totalInntekter)

            assertUtbetalingsbeløp(1.vedtaksperiode, 277, 1000, 0, subset = 17.januar til 22.januar)
        }

        a2 {
            assertUtbetalingsbeløp(1.vedtaksperiode, 415, 1500, 0, subset = 17.januar til 22.januar)
        }
    }

    @Test
    fun `graderte, andre ytelser og tilkommen inntekt spiser opp sykdomsgrad`() {
        // TODO: Test med coacher
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        }

        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 500.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 1000.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterGraderteAndreYtelserEndret(17.januar)
            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        17.januar,
                                        22.januar,
                                        60,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.OMSORGSPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                    ),
                inntekterForBeregning =
                    listOf(
                        InntekterForBeregning.Inntektsperiode(
                            "a3",
                            januar,
                            1100.daglig,
                        ),
                    ),
            )
            // 2160 =~ 6G.daglig
            // beregningsgrunnlag: 500 + 1000 = 1500
            // a1 andel av totale inntekter på skjæringstidspunktet: 500/1500 = 1/3
            // a2 andel av totale inntekter på skjæringstidspunktet: 1000/1500 = 2/3
            // andel tilkommenInntekt: 1100 / 1500 = 11/15
            // a1: 1500*(1/3)*(1-0.6-1100/1500) = -166.6666666667 => 0
            // a2: 1500*(2/3)*(1-1100/1500) = 240
            // arbeidsgiverbeløp_n = minOf(6G.daglig,totale_inntker) * (inntekt_n / totale_inntker) * (sykdomsgrad - gradertGrad - tilkommeninntekt/totalInntekter)

            // Dette er vel kanskje riktig? Det gir kanskje mening å få avslag fra Nav, fordi du har dekket hele inntektstapet ditt gjennom tilkommen inntekt?

            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 500, 0, subset = 17.januar til 22.januar)
            inspektør.utbetalingstidslinjer(1.vedtaksperiode)[17.januar].also {
                assertIs<Utbetalingsdag.AvvistDag>(it)
            }
            assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter())
        }

        a2 {
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1000, 0, subset = 17.januar til 22.januar)
            inspektør.utbetalingstidslinjer(1.vedtaksperiode)[17.januar].also {
                assertIs<Utbetalingsdag.AvvistDag>(it)
            }
        }
    }

    @Test
    fun `gradert, annen ytelse spiser opp sykdomsgraden - en arbeidsgiver`() {
        // TODO: Test med coacher
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 50.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 500.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterGraderteAndreYtelserEndret(17.januar)
            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        17.januar,
                                        22.januar,
                                        50,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.OMSORGSPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                    ),
            )
            // 2160 =~ 6G.daglig
            // beregningsgrunnlag: 500 + 1000 = 1500
            // a1 andel av totale inntekter på skjæringstidspunktet: 500/1500 = 1/3
            // a1: 1500*(1)*(0.5-0.5) = 0
            // arbeidsgiverbeløp_n = minOf(6G.daglig,totale_inntker) * (inntekt_n / totale_inntker) * (sykdomsgrad - gradertGrad)

            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 500, 0, subset = 17.januar til 22.januar)
            inspektør.utbetalingstidslinjer(1.vedtaksperiode)[17.januar].also {
                assertIs<Utbetalingsdag.AvvistDag>(it)
            }
            assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `gradert, annen ytelse spiser opp sykdomsgraden - flere arbeidsgivere`() {
        // TODO: Test med coacher
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 50.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 50.prosent))
        }

        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 500.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 1000.daglig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterGraderteAndreYtelserEndret(17.januar)
            håndterYtelser(
                1.vedtaksperiode,
                graderteAndreYtelser =
                    listOf(
                        GraderteAndreYtelserForBeregning(
                            graderteAndreYtelserForBeregningPeriodeList =
                                listOf(
                                    GraderteAndreYtelserForBeregningPeriode(
                                        17.januar,
                                        22.januar,
                                        50,
                                    ),
                                ),
                            graderteAndreYtelserType = GraderteAndreYtelserType.OMSORGSPENGER,
                            yrkesaktivitet = Arbeidstaker(a1),
                        ),
                    ),
            )
            // 2160 =~ 6G.daglig
            // beregningsgrunnlag: 500 + 1000 = 1500
            // a1 andel av totale inntekter på skjæringstidspunktet: 500/1500 = 1/3
            // a2 andel av totale inntekter på skjæringstidspunktet: 1000/1500 = 2/3
            // a1: 1500*(1/3)*(0.5-0.5) = 0
            // a2: 1500*(2/3)*(0.5) = 500
            // arbeidsgiverbeløp_n = minOf(6G.daglig,totale_inntker) * (inntekt_n / totale_inntker) * (sykdomsgrad - gradertGrad)

            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 500, 0, subset = 17.januar til 22.januar)
            inspektør.utbetalingstidslinjer(1.vedtaksperiode)[17.januar].also {
                // TODO: Vi vet ikke om det skal være avslagsdag eller ikke her. Dagen er 0 %, men totalgraden er over 20 %.
                // TODO: Avslagsdag her vil vel bety at dagen ikke teller som forbrukt dag.
//                assertIs<Utbetalingsdag.AvvistDag>(it)
            }
//            assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter())
        }

        a2 {
            assertUtbetalingsbeløp(1.vedtaksperiode, 500, 1000, 0, subset = 17.januar til 22.januar)
            inspektør.utbetalingstidslinjer(1.vedtaksperiode)[17.januar].also {
                assertIs<Utbetalingsdag.NavDag>(it)
            }
        }
    }
}
