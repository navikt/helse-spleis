package no.nav.helse.spleis.e2e.andre_ytelser

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.hendelser.InntekterForBeregning
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingstidslinje.Begrunnelse.MinimumSykdomsgrad
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
            håndterYtelser(1.vedtaksperiode, inntekterForBeregning = listOf(InntekterForBeregning.Inntektsperiode.AndelAvSykepengegrunnlag("PLEIEPENGERSYKTBARN", 20.januar til 30.januar, 30.prosent)))

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
            håndterYtelser(1.vedtaksperiode, inntekterForBeregning = listOf(InntekterForBeregning.Inntektsperiode.AndelAvSykepengegrunnlag("PLEIEPENGERSYKTBARN", 20.januar til 30.januar, 30.prosent)))
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1400, 2000, subset = 20.januar til 30.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 31.januar til 31.januar)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())

            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 533_000.årlig)
            assertInntektsgrunnlag(1.januar, 1) {
                assertInntektsgrunnlag(a1, 533_000.årlig)
                assertSykepengegrunnlag(533_000.årlig)
            }

            håndterYtelser(1.vedtaksperiode, inntekterForBeregning = listOf(InntekterForBeregning.Inntektsperiode.AndelAvSykepengegrunnlag("PLEIEPENGERSYKTBARN", 20.januar til 30.januar, 30.prosent)))

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
            håndterYtelser(1.vedtaksperiode, inntekterForBeregning = listOf(
                InntekterForBeregning.Inntektsperiode.AndelAvSykepengegrunnlag("FORELDREPENGER", 1.januar til 29.januar, 50.prosent),
                InntekterForBeregning.Inntektsperiode.Beløp(a2, 1.januar til 30.januar, 250.daglig)
            ))

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
    fun `så mye foreldrepenger at du havner under 20 prosent sykepenger`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = 520_000.årlig)
            assertInntektsgrunnlag(1.januar, 1) {
                assertInntektsgrunnlag(a1, 520_000.årlig)
                assertSykepengegrunnlag(520_000.årlig)
            }
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 17.januar til 31.januar)
            håndterInntektsendringer(20.januar)
            håndterYtelser(1.vedtaksperiode, inntekterForBeregning = listOf(
                InntekterForBeregning.Inntektsperiode.AndelAvSykepengegrunnlag("FORELDREPENGER", januar, 81.prosent),
            ))

            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 2000, subset = 17.januar til 31.januar)

            with(inspektør(a1).utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager) {
                assertEquals(11, size)
                assertTrue(all { it.begrunnelser == listOf(MinimumSykdomsgrad)})
            }

            assertVarsler(1.vedtaksperiode, Varselkode.RV_UT_23, Varselkode.RV_VV_4)
        }
    }
}
