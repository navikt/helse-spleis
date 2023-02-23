package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.utbetalingslinjer.Fagområde.Sykepenger
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype.UTBETALING
import no.nav.helse.utbetalingslinjer.Utbetalingkladd.Companion.finnKladd
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class UtbetalingkladdTest {

    @Test
    fun `kladd opphører tidligere periode dersom kladd slutter før`() {
        val kladd = Utbetalingkladd(
            periode = 5.januar til 20.januar,
            arbeidsgiveroppdrag = Oppdrag("orgnr", SykepengerRefusjon),
            personoppdrag = Oppdrag("fnr", Sykepenger)
        )

        assertTrue(kladd.opphørerHale(5.januar til 30.januar))
        assertTrue(kladd.opphørerHale(21.januar til 30.januar))
        assertFalse(kladd.opphørerHale(5.januar til 20.januar))
        assertFalse(kladd.opphørerHale(1.januar til 4.januar))
    }

    @Test
    fun `begrenses til periodens siste dato`() {
        val kladd = Utbetalingkladd(
            periode = 5.januar til 31.januar,
            arbeidsgiveroppdrag = Oppdrag("orgnr", SykepengerRefusjon),
            personoppdrag = Oppdrag("fnr", Sykepenger)
        )
        val utbetaling1 = kladd.begrensTil(5.januar til 25.januar).utbetaling.inspektør
        assertEquals(5.januar til 25.januar, utbetaling1.periode)
        val utbetaling2 = kladd.begrensTil(5.januar til 14.januar).utbetaling.inspektør
        assertEquals(5.januar til 14.januar, utbetaling2.periode)
    }

    @Test
    fun `begrenses til periodens første dato`() {
        val kladd = Utbetalingkladd(
            periode = 5.januar til 31.januar,
            arbeidsgiveroppdrag = Oppdrag("orgnr", SykepengerRefusjon),
            personoppdrag = Oppdrag("fnr", Sykepenger)
        )
        val utbetaling1 = kladd.begrensTil(6.januar til 31.januar).utbetaling.inspektør
        assertEquals(5.januar til 31.januar, utbetaling1.periode)
        val utbetaling2 = kladd.begrensTil(1.januar til 31.januar).utbetaling.inspektør
        assertEquals(1.januar til 31.januar, utbetaling2.periode)
    }

    @Test
    fun `finner første overlappende hvis ingen av kladdene har utbetaling`() {
        val kladd1 = Utbetalingkladd(
            periode = 1.januar(2017) til 15.januar,
            arbeidsgiveroppdrag = Oppdrag("orgnr", SykepengerRefusjon),
            personoppdrag = Oppdrag("fnr", Sykepenger)
        )
        val kladd2 = Utbetalingkladd(
            periode = 16.januar til 31.januar,
            arbeidsgiveroppdrag = Oppdrag("orgnr", SykepengerRefusjon),
            personoppdrag = Oppdrag("fnr", Sykepenger)
        )
        val result = listOf(kladd1, kladd2).finnKladd(1.januar til 31.januar)
        assertSame(kladd1, result.single())
    }

    @Test
    fun `finner kladd med utbetaling`() {
        val kladd1 = Utbetalingkladd(
            periode = 1.januar(2017) til 15.januar,
            arbeidsgiveroppdrag = Oppdrag("orgnr", SykepengerRefusjon),
            personoppdrag = Oppdrag("fnr", Sykepenger)
        )
        val kladd2 = Utbetalingkladd(
            periode = 16.januar til 31.januar,
            arbeidsgiveroppdrag = Oppdrag("orgnr", SykepengerRefusjon, listOf(
                Utbetalingslinje(17.januar, 31.januar, beløp = 500, aktuellDagsinntekt = null, grad = 100)
            )),
            personoppdrag = Oppdrag("fnr", Sykepenger)
        )
        val result = listOf(kladd1, kladd2).finnKladd(1.januar til 31.januar)
        assertSame(kladd2, result.single())
    }

    @Test
    fun `blant kladder med utbetaling foretrekkes den som overlapper på oppdrag`() {
        val kladd1 = Utbetalingkladd(
            periode = 1.januar(2017) til 15.januar,
            arbeidsgiveroppdrag = Oppdrag("orgnr", SykepengerRefusjon, listOf(
                Utbetalingslinje(17.januar(2017), 31.januar(2017), beløp = 500, aktuellDagsinntekt = null, grad = 100)
            )),
            personoppdrag = Oppdrag("fnr", Sykepenger)
        )
        val kladd2 = Utbetalingkladd(
            periode = 16.januar til 31.januar,
            arbeidsgiveroppdrag = Oppdrag("orgnr", SykepengerRefusjon, listOf(
                Utbetalingslinje(17.januar, 31.januar, beløp = 500, aktuellDagsinntekt = null, grad = 100)
            )),
            personoppdrag = Oppdrag("fnr", Sykepenger)
        )
        val result = listOf(kladd1, kladd2).finnKladd(1.januar til 31.januar)
        assertSame(kladd2, result.single())
    }

    @Test
    fun `flere overlappende oppdrag`() {
        val kladd1 = Utbetalingkladd(
            periode = 1.januar(2017) til 15.januar,
            arbeidsgiveroppdrag = Oppdrag("orgnr", SykepengerRefusjon, listOf(
                Utbetalingslinje(17.januar(2017), 15.januar, beløp = 500, aktuellDagsinntekt = null, grad = 100)
            )),
            personoppdrag = Oppdrag("fnr", Sykepenger)
        )
        val kladd2 = Utbetalingkladd(
            periode = 16.januar til 31.januar,
            arbeidsgiveroppdrag = Oppdrag("orgnr", SykepengerRefusjon, listOf(
                Utbetalingslinje(17.januar, 31.januar, beløp = 500, aktuellDagsinntekt = null, grad = 100)
            )),
            personoppdrag = Oppdrag("fnr", Sykepenger)
        )
        val result = listOf(kladd1, kladd2).finnKladd(1.januar til 31.januar)
        assertEquals(2, result.size)
    }

    private val Utbetalingkladd.utbetaling get() = utbetaling(Utbetalingstidslinje())
    private fun Utbetalingkladd.utbetaling(utbetalingstidslinje: Utbetalingstidslinje) = lagUtbetaling(
        type = UTBETALING,
        korrelerendeUtbetaling = null,
        beregningId = UUID.randomUUID(),
        utbetalingstidslinje = utbetalingstidslinje,
        maksdato = LocalDate.MAX,
        forbrukteSykedager = 0,
        gjenståendeSykedager = 0
    )
}