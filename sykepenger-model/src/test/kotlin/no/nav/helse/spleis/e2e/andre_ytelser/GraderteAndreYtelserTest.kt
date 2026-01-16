package no.nav.helse.spleis.e2e.andre_ytelser

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.hendelser.AndreYtelser.PeriodeMedAnnenYtelse
import no.nav.helse.hendelser.InntekterForBeregning
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GraderteAndreYtelserTest: AbstractDslTest() {

    @Test
    fun `pleiepenger sykt barn`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = 520_000.årlig)
            assertInntektsgrunnlag(1.januar, 1) {
                assertInntektsgrunnlag(a1, 520_000.årlig)
                assertSykepengegrunnlag(520_000.årlig)
            }
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 17.januar til 31.januar)
            håndterInntektsendringer(20.januar)
            håndterYtelser(1.vedtaksperiode, andreYtelser = listOf(PeriodeMedAnnenYtelse("PLEIEPENGERSYKTBARN", 20.januar til 30.januar, 30.prosent)))

            // Jeg er bare et regnestykke for å vise hvorfor det blir 1400,-
            assertEquals(1400.0, (2000 - (520_000 * 0.3) /260))

            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1400, 2000, subset = 20.januar til 30.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 31.januar til 31.januar)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
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
            håndterInntektsendringer(20.januar)
            håndterYtelser(1.vedtaksperiode, andreYtelser = listOf(PeriodeMedAnnenYtelse("PLEIEPENGERSYKTBARN", 20.januar til 30.januar, 30.prosent)))
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1400, 2000, subset = 20.januar til 30.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 31.januar til 31.januar)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())

            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 533_000.årlig)
            assertInntektsgrunnlag(1.januar, 1) {
                assertInntektsgrunnlag(a1, 533_000.årlig)
                assertSykepengegrunnlag(533_000.årlig)
            }

            håndterYtelser(1.vedtaksperiode, andreYtelser = listOf(PeriodeMedAnnenYtelse("PLEIEPENGERSYKTBARN", 20.januar til 30.januar, 30.prosent)))

            // Jeg er bare et regnestykke for å vise hvorfor det blir 1435,-
            assertEquals(1435.0, (2050 - (533_000 * 0.3) /260))
            assertUtbetalingsbeløp(1.vedtaksperiode, 2050, 2050, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1435, 2050, subset = 20.januar til 30.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 2050, 2050, subset = 31.januar til 31.januar)

            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
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
            håndterInntektsendringer(20.januar)
            håndterYtelser(1.vedtaksperiode,
                inntekterForBeregning = listOf(InntekterForBeregning.Inntektsperiode(a2, 1.januar til 30.januar, 250.daglig)),
                andreYtelser = listOf(PeriodeMedAnnenYtelse("FORELDREPENGER", 1.januar til 29.januar, 50.prosent),)
            )

            // Jeg er bare et regnestykke for å vise hvorfor det blir 750,-
            assertEquals(750.0, (2000 - (520_000 * 0.5) /260) - 250)

            // Jeg er bare et regnestykke for å vise hvorfor det blir 1750,-
            assertEquals(1750, (2000 - 250))

            assertUtbetalingsbeløp(1.vedtaksperiode, 750, 2000, subset = 17.januar til 29.januar) // begge
            assertUtbetalingsbeløp(1.vedtaksperiode, 1750, 2000, subset = 30.januar til 30.januar) // tilkommen
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 31.januar til 31.januar) // ingen
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `så mye foreldrepenger at du havner under 20 prosent avslås ikke på totalgrad, men utbetalingen reduseres`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = 520_000.årlig)
            assertInntektsgrunnlag(1.januar, 1) {
                assertInntektsgrunnlag(a1, 520_000.årlig)
                assertSykepengegrunnlag(520_000.årlig)
            }
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 17.januar til 31.januar)
            håndterInntektsendringer(20.januar)
            håndterYtelser(1.vedtaksperiode, andreYtelser = listOf(
                PeriodeMedAnnenYtelse("FORELDREPENGER", januar, 81.prosent),
            ))

            // 2000 * 0.19 = 380
            assertUtbetalingsbeløp(1.vedtaksperiode, 380, 2000, subset = 17.januar til 31.januar)

            assertEquals(0, inspektør(a1).utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)

            assertVarsler(1.vedtaksperiode, Varselkode.RV_UT_23)
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

            håndterInntektsendringer(1.juni(2025))
            håndterYtelser(1.vedtaksperiode, andreYtelser = listOf(
                PeriodeMedAnnenYtelse("FORELDREPENGER", 1.juni(2025) til 30.juni(2025), 50.prosent),
            ))

            // Her er det "plass" til 50% foreldrepenger uten at det går utover sykepengene
            assertUtbetalingsbeløp(1.vedtaksperiode, 1502, 7692, subset = 17.juni(2025) til 30.juni(2025))
        }
    }

    @Test
    fun `mange andre ytelser som overlapper litt om hverandre`() {
        a1 {
            nyttVedtak(januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)
            håndterInntektsendringer(1.januar)
            håndterYtelser(1.vedtaksperiode, andreYtelser = listOf(
                PeriodeMedAnnenYtelse("FORELDREPENGER", 1.januar til 28.januar, 20.prosent),
                PeriodeMedAnnenYtelse("PLEIEPENGER_SYKT_BARN", 17.januar til 22.januar, 20.prosent),
                PeriodeMedAnnenYtelse("OMSORGSPENGER", 22.januar.somPeriode(), 15.prosent),
                PeriodeMedAnnenYtelse("PLEIEPENGER_NÆRSTÅENDE", 22.januar til 30.januar, 20.prosent),
            ))
            assertUtbetalingsbeløp(1.vedtaksperiode, 858, 1431, subset = 17.januar til 21.januar) // 1431 * 0,60 =  858
            assertUtbetalingsbeløp(1.vedtaksperiode, 358, 1431, subset = 22.januar.somPeriode())  // 1431 * 0,25 =  358
            assertUtbetalingsbeløp(1.vedtaksperiode, 858, 1431, subset = 23.januar til 28.januar) // 1431 * 0,60 =  858
            assertUtbetalingsbeløp(1.vedtaksperiode, 1145, 1431, subset = 29.januar til 30.januar)// 1431 * 0,80 = 1145
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 31.januar.somPeriode()) // 1431 * 1,00 = 1431

            assertVarsler(1.vedtaksperiode, Varselkode.RV_UT_23)
        }
    }
}
