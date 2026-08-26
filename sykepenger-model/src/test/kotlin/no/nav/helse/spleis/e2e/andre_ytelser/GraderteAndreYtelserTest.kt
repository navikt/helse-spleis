package no.nav.helse.spleis.e2e.andre_ytelser

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.GraderteAndreYtelserForBeregning
import no.nav.helse.hendelser.GraderteAndreYtelserForBeregning.GraderteAndreYtelserForBeregningPeriode
import no.nav.helse.hendelser.GraderteAndreYtelserType
import no.nav.helse.hendelser.InntekterForBeregning
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GraderteAndreYtelserTest : AbstractDslTest() {

    //@OpenInSpanner
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
                graderteAndreYtelser = listOf(
                    GraderteAndreYtelserForBeregning(
                        graderteAndreYtelserForBeregningPeriodeList = listOf(GraderteAndreYtelserForBeregningPeriode(20.januar, 30.januar, 30)),
                        graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER
                    )
                )
            )

            // Jeg er bare et regnestykke for å vise hvorfor det blir 1400,-
            assertEquals(1400.0, (2000 - (520_000 * 0.3) / 260))

            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1400, 2000, subset = 20.januar til 30.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 31.januar til 31.januar)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertEquals(70.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar))
            assertEquals(70.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar))
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
                graderteAndreYtelser = listOf(
                    GraderteAndreYtelserForBeregning(
                        graderteAndreYtelserForBeregningPeriodeList = listOf(GraderteAndreYtelserForBeregningPeriode(20.januar, 30.januar, 30)),
                        graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER

                    )
                )
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
                graderteAndreYtelser = listOf(
                    GraderteAndreYtelserForBeregning(
                        graderteAndreYtelserForBeregningPeriodeList = listOf(GraderteAndreYtelserForBeregningPeriode(20.januar, 30.januar, 30)),
                        graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER

                    )
                )
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
        val gradertePleiepenger = listOf(
            GraderteAndreYtelserForBeregning(
                graderteAndreYtelserForBeregningPeriodeList = listOf(GraderteAndreYtelserForBeregningPeriode(20.januar, 30.januar, 30)),
                graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER
            )
        )

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
            assertEquals(70.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar))
            assertEquals(70.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar))
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
            assertEquals(70.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar))
            assertEquals(70.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar))
        }
    }

    @Test
    fun `annen ytelse for person med to arbeidsforhold, og 6G-begrenset til sammen`() {
        val inntektA1 = 400_000.årlig
        val inntektA2 = 600_000.årlig
        val gradertePleiepenger = listOf(
            GraderteAndreYtelserForBeregning(
                graderteAndreYtelserForBeregningPeriodeList = listOf(GraderteAndreYtelserForBeregningPeriode(20.januar, 30.januar, 30)),
                graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER
            )
        )

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
                skatteinntekter = listOf(a1 to inntektA1, a2 to inntektA2)
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
            assertEquals(70.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar))
            assertEquals(70.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar))
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
            assertEquals(70.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar))
            assertEquals(70.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar))
        }
    }

    @Test
    fun `foreldrepenger og tilkommen inntekt`() {
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
                inntekterForBeregning = listOf(InntekterForBeregning.Inntektsperiode(a2, 1.januar til 30.januar, 250.daglig)),
                graderteAndreYtelser = listOf(
                    GraderteAndreYtelserForBeregning(
                        graderteAndreYtelserForBeregningPeriodeList = listOf(GraderteAndreYtelserForBeregningPeriode(1.januar, 29.januar, 50)),
                        graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER

                    ),
                )
            )

            // Jeg er bare et regnestykke for å vise hvorfor det blir 750,-
            assertEquals(750.0, (2000 - (520_000 * 0.5) / 260) - 250)

            // Jeg er bare et regnestykke for å vise hvorfor det blir 1750,-
            assertEquals(1750, (2000 - 250))

            assertUtbetalingsbeløp(1.vedtaksperiode, 750, 2000, subset = 17.januar til 29.januar) // begge
            assertUtbetalingsbeløp(1.vedtaksperiode, 1750, 2000, subset = 30.januar til 30.januar) // tilkommen
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 31.januar til 31.januar) // ingen

            assertEquals(37.5.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar))
            assertEquals(37.5.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar)) // begge

            assertEquals(87.5.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(30.januar))
            assertEquals(87.5.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(30.januar)) // tilkommen

            assertEquals(100.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(31.januar))
            assertEquals(100.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(31.januar)) // ingen

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
                graderteAndreYtelser = listOf(
                    GraderteAndreYtelserForBeregning(
                        graderteAndreYtelserForBeregningPeriodeList = listOf(GraderteAndreYtelserForBeregningPeriode(1.januar, 31.januar, 81)),
                        graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER
                    )
                )
            )

            // Alt er avvist, under 20 prosent
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 2000, subset = 17.januar til 31.januar)

            assertEquals(11, inspektør(a1).utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)

            assertEquals(0.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.utbetalingsgrad(22.januar))
            assertEquals(19.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.totalSykdomsgrad(22.januar))

            assertVarsler(1.vedtaksperiode, Varselkode.RV_UT_23, Varselkode.RV_VV_4)
        }
    }

    @Test
    fun `gradert sykmeldt, graderte foreldrepenger og 6G-begrenset`() {
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
                graderteAndreYtelser = listOf(
                    GraderteAndreYtelserForBeregning(
                        graderteAndreYtelserForBeregningPeriodeList = listOf(GraderteAndreYtelserForBeregningPeriode(1.juni(2025), 30.juni(2025), 50)),
                        graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER
                    )
                )
            )

            // Her er det "plass" til 50% foreldrepenger uten at det går utover sykepengene
            assertUtbetalingsbeløp(1.vedtaksperiode, 1502, 7692, subset = 17.juni(2025) til 30.juni(2025))
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
                graderteAndreYtelser = listOf(
                    GraderteAndreYtelserForBeregning(
                        graderteAndreYtelserForBeregningPeriodeList = listOf(GraderteAndreYtelserForBeregningPeriode(1.januar, 28.januar, 20)),
                        graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER
                    ), GraderteAndreYtelserForBeregning(
                    graderteAndreYtelserForBeregningPeriodeList = listOf(GraderteAndreYtelserForBeregningPeriode(17.januar, 22.januar, 20)),
                    graderteAndreYtelserType = GraderteAndreYtelserType.OMSORGSPENGER

                ), GraderteAndreYtelserForBeregning(
                    graderteAndreYtelserForBeregningPeriodeList = listOf(GraderteAndreYtelserForBeregningPeriode(22.januar, 22.januar, 15)),
                    graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER

                ), GraderteAndreYtelserForBeregning(
                    graderteAndreYtelserForBeregningPeriodeList = listOf(GraderteAndreYtelserForBeregningPeriode(22.januar, 30.januar, 20)),
                    graderteAndreYtelserType = GraderteAndreYtelserType.OPPLARINGSPENGER

                )

                )
            )

            assertUtbetalingsbeløp(1.vedtaksperiode, 858, 1431, subset = 17.januar til 21.januar) // 1431 * 0,60 =  858
            assertUtbetalingsbeløp(1.vedtaksperiode, 358, 1431, subset = 22.januar.somPeriode())  // 1431 * 0,25 =  358
            assertUtbetalingsbeløp(1.vedtaksperiode, 858, 1431, subset = 23.januar til 28.januar) // 1431 * 0,60 =  858
            assertUtbetalingsbeløp(1.vedtaksperiode, 1145, 1431, subset = 29.januar til 30.januar)// 1431 * 0,80 = 1145
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 31.januar.somPeriode()) // 1431 * 1,00 = 1431

            assertVarsler(1.vedtaksperiode, Varselkode.RV_UT_23)
        }
    }
}
