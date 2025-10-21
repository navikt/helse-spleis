package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.collections.filter
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.a3
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Yrkesaktivitet
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

internal class ArbeidsgiverberegningTest {

    private fun sykdomstidslinje(periode: Periode): Sykdomstidslinje {
        return resetSeed(periode.start) { periode.count().S }
    }

    @Test
    fun `en arbeidsgiver - inngår i sykengegrunnlag`() {
        val yrkesaktiviteter = ArbeidsgiverberegningBuilder()
            .fastsattÅrsinntekt(Yrkesaktivitet.Arbeidstaker(a1), INNTEKT)
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a1), UUID.randomUUID(), sykdomstidslinje(januar), arbeidstaker())
            .build()

        assertEquals(1, yrkesaktiviteter.size)
        assertEquals(Yrkesaktivitet.Arbeidstaker(a1), yrkesaktiviteter.single().yrkesaktivitet)
        assertEquals(1, yrkesaktiviteter.single().vedtaksperioder.size)
        assertEquals(0, yrkesaktiviteter.single().ghostOgAndreInntektskilder.size)
        yrkesaktiviteter.single().vedtaksperioder.single().also { beregningsperiode ->
            assertEquals(januar, beregningsperiode.periode)
            assertTrue(beregningsperiode.utbetalingstidslinje.all { it.økonomi.inspektør.aktuellDagsinntekt == INNTEKT })
            assertTrue(beregningsperiode.utbetalingstidslinje.all { it.økonomi.inspektør.inntektjustering == Inntekt.INGEN })
        }
    }

    @Test
    fun `en arbeidsgiver - inngår ikke i sykepengegrunnag`() {
        val yrkesaktiviteter = ArbeidsgiverberegningBuilder()
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a1), UUID.randomUUID(), sykdomstidslinje(januar), arbeidstaker())
            .build()

        assertEquals(1, yrkesaktiviteter.size)
        assertEquals(Yrkesaktivitet.Arbeidstaker(a1), yrkesaktiviteter.single().yrkesaktivitet)
        assertEquals(1, yrkesaktiviteter.single().vedtaksperioder.size)
        assertEquals(0, yrkesaktiviteter.single().ghostOgAndreInntektskilder.size)
        yrkesaktiviteter.single().vedtaksperioder.single().also { beregningsperiode ->
            assertEquals(januar, beregningsperiode.periode)
            assertTrue(beregningsperiode.utbetalingstidslinje.all { it.økonomi.inspektør.aktuellDagsinntekt == Inntekt.INGEN })
            assertTrue(beregningsperiode.utbetalingstidslinje.all { it.økonomi.inspektør.inntektjustering == Inntekt.INGEN })
        }
    }

    @Test
    fun `flere arbeidsgiver - med ghostperioder og tilkommet inntekt`() {
        val yrkesaktiviteter = ArbeidsgiverberegningBuilder()
            .fastsattÅrsinntekt(Yrkesaktivitet.Arbeidstaker(a1), INNTEKT)
            .fastsattÅrsinntekt(Yrkesaktivitet.Arbeidstaker(a2), INNTEKT * 2)
            .inntektsjusteringer(Yrkesaktivitet.Arbeidstaker(a3), 10.januar, 14.januar, 1500.daglig)
            .inntektsjusteringer(Yrkesaktivitet.Arbeidstaker(a3), 25.januar, 31.januar, 1500.daglig)
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a1), UUID.randomUUID(), sykdomstidslinje(1.januar til 20.januar), arbeidstaker())
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a1), UUID.randomUUID(), sykdomstidslinje(21.januar til 25.januar), arbeidstaker())
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a1), UUID.randomUUID(), sykdomstidslinje(29.januar til 31.januar), arbeidstaker())
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a2), UUID.randomUUID(), sykdomstidslinje(1.januar til 21.januar), arbeidstaker())
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a2), UUID.randomUUID(), sykdomstidslinje(25.januar til 30.januar), arbeidstaker())
            .build()

        assertEquals(3, yrkesaktiviteter.size)
        yrkesaktiviteter[0].also { yrkesaktivitet ->
            assertEquals(Yrkesaktivitet.Arbeidstaker(a1), yrkesaktivitet.yrkesaktivitet)
            assertEquals(3, yrkesaktivitet.vedtaksperioder.size)
            assertEquals(1, yrkesaktivitet.ghostOgAndreInntektskilder.size)
            yrkesaktivitet.vedtaksperioder[0].also { beregningsperiode ->
                assertEquals(1.januar til 20.januar, beregningsperiode.periode)
                assertTrue(beregningsperiode.utbetalingstidslinje.all { it.økonomi.inspektør.aktuellDagsinntekt == INNTEKT })
                assertTrue(beregningsperiode.utbetalingstidslinje.all { it.økonomi.inspektør.inntektjustering == Inntekt.INGEN })
            }
            yrkesaktivitet.vedtaksperioder[1].also { beregningsperiode ->
                assertEquals(21.januar til 25.januar, beregningsperiode.periode)
                assertTrue(beregningsperiode.utbetalingstidslinje.all { it.økonomi.inspektør.aktuellDagsinntekt == INNTEKT })
                assertTrue(beregningsperiode.utbetalingstidslinje.all { it.økonomi.inspektør.inntektjustering == Inntekt.INGEN })
            }
            yrkesaktivitet.vedtaksperioder[2].also { beregningsperiode ->
                assertEquals(29.januar til 31.januar, beregningsperiode.periode)
                assertTrue(beregningsperiode.utbetalingstidslinje.all { it.økonomi.inspektør.aktuellDagsinntekt == INNTEKT })
                assertTrue(beregningsperiode.utbetalingstidslinje.all { it.økonomi.inspektør.inntektjustering == Inntekt.INGEN })
            }
            yrkesaktivitet.ghostOgAndreInntektskilder[0].also { beregningsperiode ->
                assertEquals(26.januar til 28.januar, beregningsperiode.periode())
                assertTrue(beregningsperiode.filter { it.dato.erHelg() }.all { it.økonomi.inspektør.aktuellDagsinntekt == Inntekt.INGEN })
                assertTrue(beregningsperiode.filter { !it.dato.erHelg() }.all { it.økonomi.inspektør.aktuellDagsinntekt == INNTEKT })
                assertTrue(beregningsperiode.filter { !it.dato.erHelg() }.all { it.økonomi.inspektør.inntektjustering == Inntekt.INGEN })
            }
        }
        yrkesaktiviteter[1].also { yrkesaktivitet ->
            assertEquals(Yrkesaktivitet.Arbeidstaker(a2), yrkesaktivitet.yrkesaktivitet)
            assertEquals(2, yrkesaktivitet.vedtaksperioder.size)
            assertEquals(2, yrkesaktivitet.ghostOgAndreInntektskilder.size)
            yrkesaktivitet.vedtaksperioder[0].also { beregningsperiode ->
                assertEquals(1.januar til 21.januar, beregningsperiode.periode)
            }
            yrkesaktivitet.vedtaksperioder[1].also { beregningsperiode ->
                assertEquals(25.januar til 30.januar, beregningsperiode.periode)
                assertTrue(beregningsperiode.utbetalingstidslinje.all { it.økonomi.inspektør.aktuellDagsinntekt == INNTEKT * 2 })
                assertTrue(beregningsperiode.utbetalingstidslinje.all { it.økonomi.inspektør.inntektjustering == Inntekt.INGEN })
            }

            yrkesaktivitet.ghostOgAndreInntektskilder[0].also { beregningsperiode ->
                assertEquals(22.januar til 24.januar, beregningsperiode.periode())
                assertTrue(beregningsperiode.filter { it.dato.erHelg() }.all { it.økonomi.inspektør.aktuellDagsinntekt == Inntekt.INGEN })
                assertTrue(beregningsperiode.filter { !it.dato.erHelg() }.all { it.økonomi.inspektør.aktuellDagsinntekt == INNTEKT * 2 })
                assertTrue(beregningsperiode.filter { !it.dato.erHelg() }.all { it.økonomi.inspektør.inntektjustering == Inntekt.INGEN })
            }
            yrkesaktivitet.ghostOgAndreInntektskilder[1].also { beregningsperiode ->
                assertEquals(31.januar til 31.januar, beregningsperiode.periode())
                assertTrue(beregningsperiode.filter { it.dato.erHelg() }.all { it.økonomi.inspektør.aktuellDagsinntekt == Inntekt.INGEN })
                assertTrue(beregningsperiode.filter { !it.dato.erHelg() }.all { it.økonomi.inspektør.aktuellDagsinntekt == INNTEKT * 2 })
                assertTrue(beregningsperiode.filter { !it.dato.erHelg() }.all { it.økonomi.inspektør.inntektjustering == Inntekt.INGEN })
            }
        }

        yrkesaktiviteter[2].also { yrkesaktivitet ->
            assertEquals(Yrkesaktivitet.Arbeidstaker(a3), yrkesaktivitet.yrkesaktivitet)
            assertEquals(0, yrkesaktivitet.vedtaksperioder.size)
            assertEquals(2, yrkesaktivitet.ghostOgAndreInntektskilder.size)
            yrkesaktivitet.ghostOgAndreInntektskilder[0].also { beregningsperiode ->
                assertEquals(10.januar til 14.januar, beregningsperiode.periode())
                assertTrue(beregningsperiode.all { it.økonomi.inspektør.aktuellDagsinntekt == Inntekt.INGEN })
                assertTrue(beregningsperiode.filter { !it.dato.erHelg() }.all { it.økonomi.inspektør.inntektjustering == 1500.daglig })
            }
            yrkesaktivitet.ghostOgAndreInntektskilder[1].also { beregningsperiode ->
                assertEquals(25.januar til 31.januar, beregningsperiode.periode())
                assertTrue(beregningsperiode.all { it.økonomi.inspektør.aktuellDagsinntekt == Inntekt.INGEN })
                assertTrue(beregningsperiode.filter { !it.dato.erHelg() }.all { it.økonomi.inspektør.inntektjustering == 1500.daglig })
            }
        }
    }

    @Test
    fun `tilkommet inntekt uten vedtaksperioder`() {
        val yrkesaktiviteter = ArbeidsgiverberegningBuilder()
            .fastsattÅrsinntekt(Yrkesaktivitet.Arbeidstaker(a1), INNTEKT)
            .inntektsjusteringer(Yrkesaktivitet.Arbeidstaker(a3), 1.januar, 10.januar, 1500.daglig)
            .inntektsjusteringer(Yrkesaktivitet.Arbeidstaker(a3), 15.januar, 20.januar, 1500.daglig)
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a1), UUID.randomUUID(), sykdomstidslinje(1.januar til 20.januar), arbeidstaker())
            .build()

        assertEquals(2, yrkesaktiviteter.size)
        yrkesaktiviteter[0].also { yrkesaktivitet ->
            assertEquals(Yrkesaktivitet.Arbeidstaker(a1), yrkesaktivitet.yrkesaktivitet)
            assertEquals(1, yrkesaktivitet.vedtaksperioder.size)
            assertEquals(0, yrkesaktivitet.ghostOgAndreInntektskilder.size)
            yrkesaktivitet.vedtaksperioder[0].also { beregningsperiode ->
                assertEquals(1.januar til 20.januar, beregningsperiode.periode)
            }
        }
        yrkesaktiviteter[1].also { yrkesaktivitet ->
            assertEquals(Yrkesaktivitet.Arbeidstaker(a3), yrkesaktivitet.yrkesaktivitet)
            assertEquals(0, yrkesaktivitet.vedtaksperioder.size)
            assertEquals(2, yrkesaktivitet.ghostOgAndreInntektskilder.size)
            yrkesaktivitet.ghostOgAndreInntektskilder[0].also { beregningsperiode ->
                assertEquals(1.januar til 10.januar, beregningsperiode.periode())
                assertTrue(beregningsperiode.all { it.økonomi.inspektør.aktuellDagsinntekt == Inntekt.INGEN })
                assertTrue(beregningsperiode.filter { !it.dato.erHelg() }.all { it.økonomi.inspektør.inntektjustering == 1500.daglig })
            }
            yrkesaktivitet.ghostOgAndreInntektskilder[1].also { beregningsperiode ->
                assertEquals(15.januar til 20.januar, beregningsperiode.periode())
                assertTrue(beregningsperiode.all { it.økonomi.inspektør.aktuellDagsinntekt == Inntekt.INGEN })
                assertTrue(beregningsperiode.filter { !it.dato.erHelg() }.all { it.økonomi.inspektør.inntektjustering == 1500.daglig })
            }
        }
    }

    @Test
    fun `tilkommet inntekt med vedtaksperioder`() {
        val yrkesaktiviteter = ArbeidsgiverberegningBuilder()
            .fastsattÅrsinntekt(Yrkesaktivitet.Arbeidstaker(a1), INNTEKT)
            .inntektsjusteringer(Yrkesaktivitet.Arbeidstaker(a1), 1.januar, 20.januar, 1500.daglig)
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a1), UUID.randomUUID(), sykdomstidslinje(1.januar til 20.januar), arbeidstaker())
            .build()

        assertEquals(1, yrkesaktiviteter.size)
        yrkesaktiviteter[0].also { yrkesaktivitet ->
            assertEquals(Yrkesaktivitet.Arbeidstaker(a1), yrkesaktivitet.yrkesaktivitet)
            assertEquals(1, yrkesaktivitet.vedtaksperioder.size)
            assertEquals(0, yrkesaktivitet.ghostOgAndreInntektskilder.size)
            yrkesaktivitet.vedtaksperioder[0].also { beregningsperiode ->
                assertEquals(1.januar til 20.januar, beregningsperiode.periode)
                assertTrue(beregningsperiode.utbetalingstidslinje.all { it.økonomi.inspektør.aktuellDagsinntekt == INNTEKT })
                assertTrue(beregningsperiode.utbetalingstidslinje.all { it.økonomi.inspektør.inntektjustering == 1500.daglig })
            }
        }
    }

    @Test
    fun `tilkommet inntekt med egne vedtaksperioder`() {
        val yrkesaktiviteter = ArbeidsgiverberegningBuilder()
            .fastsattÅrsinntekt(Yrkesaktivitet.Arbeidstaker(a1), INNTEKT)
            .inntektsjusteringer(Yrkesaktivitet.Arbeidstaker(a3), 1.januar, 20.januar, 1500.daglig)
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a1), UUID.randomUUID(), sykdomstidslinje(1.januar til 20.januar), arbeidstaker())
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a3), UUID.randomUUID(), sykdomstidslinje(1.januar til 10.januar), arbeidstaker())
            .build()

        assertEquals(2, yrkesaktiviteter.size)
        yrkesaktiviteter[0].also { yrkesaktivitet ->
            assertEquals(Yrkesaktivitet.Arbeidstaker(a1), yrkesaktivitet.yrkesaktivitet)
            assertEquals(1, yrkesaktivitet.vedtaksperioder.size)
            assertEquals(0, yrkesaktivitet.ghostOgAndreInntektskilder.size)
            yrkesaktivitet.vedtaksperioder[0].also { beregningsperiode ->
                assertEquals(1.januar til 20.januar, beregningsperiode.periode)
            }
        }
        yrkesaktiviteter[1].also { yrkesaktivitet ->
            assertEquals(Yrkesaktivitet.Arbeidstaker(a3), yrkesaktivitet.yrkesaktivitet)
            assertEquals(1, yrkesaktivitet.vedtaksperioder.size)
            assertEquals(1, yrkesaktivitet.ghostOgAndreInntektskilder.size)
            yrkesaktivitet.vedtaksperioder[0].also { beregningsperiode ->
                assertEquals(1.januar til 10.januar, beregningsperiode.periode)
                assertTrue(beregningsperiode.utbetalingstidslinje.all { it.økonomi.inspektør.aktuellDagsinntekt == Inntekt.INGEN })
                assertTrue(beregningsperiode.utbetalingstidslinje.all { it.økonomi.inspektør.inntektjustering == 1500.daglig })
            }
            yrkesaktivitet.ghostOgAndreInntektskilder[0].also { beregningsperiode ->
                assertEquals(11.januar til 20.januar, beregningsperiode.periode())
                assertTrue(beregningsperiode.all { it.økonomi.inspektør.aktuellDagsinntekt == Inntekt.INGEN })
                assertTrue(beregningsperiode.filter { !it.dato.erHelg() }.all { it.økonomi.inspektør.inntektjustering == 1500.daglig })
            }
        }
    }

    private fun arbeidstaker(): UtbetalingstidslinjeBuilder {
        return ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode(
            arbeidsgiverperiode = listOf(1.januar til 16.januar),
            dagerNavOvertarAnsvar = emptyList(),
            refusjonstidslinje = Beløpstidslinje.fra(
                januar, INNTEKT,
                Kilde(MeldingsreferanseId(UUID.randomUUID()), Avsender.ARBEIDSGIVER, LocalDateTime.now())
            )
        )
    }

    private fun ArbeidsgiverberegningBuilder.inntektsjusteringer(yrkesaktivitet: Yrkesaktivitet, fom: LocalDate, tom: LocalDate, inntekt: Inntekt) = apply {
        inntektsjusteringer(yrkesaktivitet, Beløpstidslinje.fra(fom til tom, inntekt, Kilde(MeldingsreferanseId(UUID.randomUUID()), Avsender.SYSTEM, LocalDateTime.now())))
    }
}
