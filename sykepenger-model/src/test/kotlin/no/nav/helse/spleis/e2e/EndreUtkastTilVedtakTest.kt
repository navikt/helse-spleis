package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class EndreUtkastTilVedtakTest : AbstractDslTest() {

    @Test
    fun `endrer inntekten etter at periodene er beregnet`() {
        a1 {
            nyPeriode(januar)
        }
        a2 {
            nyPeriode(januar)
        }
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(1.vedtaksperiode,
                Arbeidsgiveropplysning.OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)),
                Arbeidsgiveropplysning.OppgittInntekt(INNTEKT),
                Arbeidsgiveropplysning.OppgittRefusjon(INNTEKT, emptyList()))
        }
        a2 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(1.vedtaksperiode,
                Arbeidsgiveropplysning.OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)),
                Arbeidsgiveropplysning.OppgittInntekt(INNTEKT),
                Arbeidsgiveropplysning.OppgittRefusjon(INNTEKT, emptyList()))
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertUtbetalingsbeløp(1.vedtaksperiode, 1080, 1431, 0, subset = 17.januar til 31.januar)
        }
        a2 {
            assertUtbetalingsbeløp(1.vedtaksperiode, 1080, 1431, 0, subset = 17.januar til 31.januar)
        }

        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a2, INNTEKT / 2, emptyList())))

        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, 0, subset = 17.januar til 31.januar)
        }
        a2 {
            assertUtbetalingsbeløp(1.vedtaksperiode, 715, 1431, 0, subset = 17.januar til 31.januar)
        }
    }

    @Test
    fun `endrer sykdomstidslinjen etter at periodene er beregnet - endrer a1`() {
        a1 {
            nyPeriode(januar)
        }
        a2 {
            nyPeriode(januar)
        }
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(1.vedtaksperiode,
                Arbeidsgiveropplysning.OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)),
                Arbeidsgiveropplysning.OppgittInntekt(INNTEKT),
                Arbeidsgiveropplysning.OppgittRefusjon(INNTEKT, emptyList()))
        }
        a2 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(1.vedtaksperiode,
                Arbeidsgiveropplysning.OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)),
                Arbeidsgiveropplysning.OppgittInntekt(INNTEKT),
                Arbeidsgiveropplysning.OppgittRefusjon(INNTEKT, emptyList()))
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertUtbetalingsbeløp(1.vedtaksperiode, 1080, 1431, 0, subset = 17.januar til 31.januar)
            val utbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode)
            assertEquals(setOf(100.0), utbetalingstidslinje.map { it.økonomi.sykdomsgrad.toDouble() }.toSet())
            assertEquals(setOf(100.0), utbetalingstidslinje.map { it.økonomi.totalSykdomsgrad.toDouble() }.toSet())

        }
        a2 {
            assertUtbetalingsbeløp(1.vedtaksperiode, 1080, 1431, 0, subset = 17.januar til 31.januar)
            val utbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode)
            assertEquals(setOf(100.0), utbetalingstidslinje.map { it.økonomi.sykdomsgrad.toDouble() }.toSet())
            assertEquals(setOf(100.0), utbetalingstidslinje.map { it.økonomi.totalSykdomsgrad.toDouble() }.toSet())
        }

        a1 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
        }

        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, 0, subset = 1.januar til 17.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1080, 1431, 0, subset = 18.januar til 31.januar)
            val utbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode)
            assertEquals(setOf(100.0, 0.0), utbetalingstidslinje.map { it.økonomi.sykdomsgrad.toDouble() }.toSet())
            assertEquals(setOf(100.0, 50.0), utbetalingstidslinje.map { it.økonomi.totalSykdomsgrad.toDouble() }.toSet())
        }
        a2 {
            assertUtbetalingsbeløp(1.vedtaksperiode, 1080, 1431, 0, subset = 17.januar til 31.januar)
            val utbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode)
            assertEquals(setOf(100.0), utbetalingstidslinje.map { it.økonomi.sykdomsgrad.toDouble() }.toSet())
            assertEquals(setOf(100.0, 50.0), utbetalingstidslinje.map { it.økonomi.totalSykdomsgrad.toDouble() }.toSet())

        }
    }

    @Test
    fun `endrer sykdomstidslinjen etter at periodene er beregnet - endrer a2`() {
        a1 {
            nyPeriode(januar)
        }
        a2 {
            nyPeriode(januar)
        }
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(1.vedtaksperiode,
                Arbeidsgiveropplysning.OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)),
                Arbeidsgiveropplysning.OppgittInntekt(INNTEKT),
                Arbeidsgiveropplysning.OppgittRefusjon(INNTEKT, emptyList()))
        }
        a2 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(1.vedtaksperiode,
                Arbeidsgiveropplysning.OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)),
                Arbeidsgiveropplysning.OppgittInntekt(INNTEKT),
                Arbeidsgiveropplysning.OppgittRefusjon(INNTEKT, emptyList()))
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertUtbetalingsbeløp(1.vedtaksperiode, 1080, 1431, 0, subset = 17.januar til 31.januar)
            val utbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode)
            assertEquals(setOf(100.0), utbetalingstidslinje.map { it.økonomi.sykdomsgrad.toDouble() }.toSet())
            assertEquals(setOf(100.0), utbetalingstidslinje.map { it.økonomi.totalSykdomsgrad.toDouble() }.toSet())

        }
        a2 {
            assertUtbetalingsbeløp(1.vedtaksperiode, 1080, 1431, 0, subset = 17.januar til 31.januar)
            val utbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode)
            assertEquals(setOf(100.0), utbetalingstidslinje.map { it.økonomi.sykdomsgrad.toDouble() }.toSet())
            assertEquals(setOf(100.0), utbetalingstidslinje.map { it.økonomi.totalSykdomsgrad.toDouble() }.toSet())
        }

        a2 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
        }

        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1080, 1431, 0, subset = 17.januar til 31.januar)
            val utbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode)
            assertEquals(setOf(100.0), utbetalingstidslinje.map { it.økonomi.sykdomsgrad.toDouble() }.toSet())
            assertEquals(setOf(100.0, 50.0), utbetalingstidslinje.map { it.økonomi.totalSykdomsgrad.toDouble() }.toSet())
        }
        a2 {
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, 0, subset = 1.januar til 17.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1080, 1431, 0, subset = 18.januar til 31.januar)
            val utbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode)
            assertEquals(setOf(100.0, 0.0), utbetalingstidslinje.map { it.økonomi.sykdomsgrad.toDouble() }.toSet())
            assertEquals(setOf(100.0, 50.0), utbetalingstidslinje.map { it.økonomi.totalSykdomsgrad.toDouble() }.toSet())

        }
    }
}
