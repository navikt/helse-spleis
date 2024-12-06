package no.nav.helse.utbetalingstidslinje

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ArbeidsgiverperiodetellerTest {

    @Test
    fun `ingen arbeidsgiverperiode`() {
        arbeidsgiverperiodeteller = Arbeidsgiverperiodeteller.IngenArbeidsgiverperiode
        arbeidsgiverperiodeteller.observer(observatør)
        repeat(15) { arbeidsgiverperiodeteller.inc() }
        assertEquals(0, observatør.arbeidsgiverperiodedager)
        assertEquals(15, observatør.sykedager)
        assertFalse(observatør.arbeidsgiverperiodeFerdig)
    }

    @Test
    fun `ingen arbeidsgiverperiode med reset`() {
        arbeidsgiverperiodeteller = Arbeidsgiverperiodeteller.IngenArbeidsgiverperiode
        arbeidsgiverperiodeteller.observer(observatør)
        repeat(15) { arbeidsgiverperiodeteller.inc() }
        repeat(16) { arbeidsgiverperiodeteller.dec() }
        repeat(15) { arbeidsgiverperiodeteller.inc() }
        assertEquals(0, observatør.arbeidsgiverperiodedager)
        assertEquals(30, observatør.sykedager)
        assertFalse(observatør.arbeidsgiverperiodeFerdig)
    }

    @Test
    fun `arbeidsgiverperiode er fullført allerede`() {
        arbeidsgiverperiodeteller.fullfør()
        repeat(31) { arbeidsgiverperiodeteller.inc() }
        assertEquals(0, observatør.arbeidsgiverperiodedager)
        assertEquals(31, observatør.sykedager)
        assertEquals(0, observatør.arbeidsgiverperioder)
        assertFalse(observatør.arbeidsgiverperiodeFerdig)
    }

    @Test
    fun `arbeidsgiverperiode er fullført mens vi er midt i telling`() {
        repeat(10) { arbeidsgiverperiodeteller.inc() }
        repeat(10) { arbeidsgiverperiodeteller.dec() }
        arbeidsgiverperiodeteller.fullfør()
        repeat(31) { arbeidsgiverperiodeteller.inc() }
        assertEquals(10, observatør.arbeidsgiverperiodedager)
        assertEquals(31, observatør.sykedager)
        assertEquals(0, observatør.arbeidsgiverperioder)
        assertFalse(observatør.arbeidsgiverperiodeFerdig)
    }

    @Test
    fun `nesten ferdig`() {
        repeat(15) { arbeidsgiverperiodeteller.inc() }
        assertEquals(15, observatør.arbeidsgiverperiodedager)
        assertEquals(0, observatør.sykedager)
        assertFalse(observatør.arbeidsgiverperiodeFerdig)
    }

    @Test
    fun `akkurat nok til å bli ferdig`() {
        repeat(16) { arbeidsgiverperiodeteller.inc() }
        assertEquals(16, observatør.arbeidsgiverperiodedager)
        assertEquals(0, observatør.sykedager)
        assertTrue(observatør.arbeidsgiverperiodeFerdig)
    }

    @Test
    fun `mer enn nok`() {
        repeat(31) { arbeidsgiverperiodeteller.inc() }
        assertEquals(16, observatør.arbeidsgiverperiodedager)
        assertEquals(15, observatør.sykedager)
        assertTrue(observatør.arbeidsgiverperiodeFerdig)
    }

    @Test
    fun `ikke nok oppholdsdager`() {
        repeat(15) { arbeidsgiverperiodeteller.inc() }
        repeat(15) { arbeidsgiverperiodeteller.dec() }
        assertEquals(15, observatør.arbeidsgiverperiodedager)
        assertEquals(0, observatør.sykedager)
        assertFalse(observatør.arbeidsgiverperiodeFerdig)
    }

    @Test
    fun `ferdig etter nesten nok oppholdsdager`() {
        repeat(15) { arbeidsgiverperiodeteller.inc() }
        repeat(15) { arbeidsgiverperiodeteller.dec() }
        arbeidsgiverperiodeteller.inc()
        assertEquals(16, observatør.arbeidsgiverperiodedager)
        assertEquals(0, observatør.sykedager)
        assertEquals(1, observatør.arbeidsgiverperioder)
        assertFalse(observatør.arbeidsgiverperiodeAvbrutt)
        assertTrue(observatør.arbeidsgiverperiodeFerdig)
    }

    @Test
    fun `nok oppholdsdager`() {
        repeat(15) { arbeidsgiverperiodeteller.inc() }
        repeat(16) { arbeidsgiverperiodeteller.dec() }
        arbeidsgiverperiodeteller.inc()
        assertEquals(16, observatør.arbeidsgiverperiodedager)
        assertEquals(0, observatør.sykedager)
        assertTrue(observatør.arbeidsgiverperiodeAvbrutt)
        assertFalse(observatør.arbeidsgiverperiodeFerdig)
    }

    @Test
    fun `ikke nok oppholdsdager med sykedager etterpå`() {
        repeat(16) { arbeidsgiverperiodeteller.inc() }
        repeat(15) { arbeidsgiverperiodeteller.dec() }
        repeat(15) { arbeidsgiverperiodeteller.inc() }
        assertEquals(16, observatør.arbeidsgiverperiodedager)
        assertEquals(15, observatør.sykedager)
        assertEquals(1, observatør.arbeidsgiverperioder)
        assertFalse(observatør.arbeidsgiverperiodeAvbrutt)
        assertTrue(observatør.arbeidsgiverperiodeFerdig)
    }

    @Test
    fun `nok oppholdsdager etter ferdig`() {
        repeat(16) { arbeidsgiverperiodeteller.inc() }
        repeat(16) { arbeidsgiverperiodeteller.dec() }
        repeat(16) { arbeidsgiverperiodeteller.inc() }
        assertEquals(32, observatør.arbeidsgiverperiodedager)
        assertEquals(0, observatør.sykedager)
        assertEquals(2, observatør.arbeidsgiverperioder)
        assertEquals(1, observatør.avbrutteArbeidsgiverperioder)
        assertTrue(observatør.arbeidsgiverperiodeFerdig)
    }

    private lateinit var arbeidsgiverperiodeteller: Arbeidsgiverperiodeteller
    private lateinit var observatør: Observatør

    @BeforeEach
    fun setup() {
        observatør = Observatør()
        arbeidsgiverperiodeteller = Arbeidsgiverperiodeteller.NormalArbeidstaker
        arbeidsgiverperiodeteller.observer(observatør)
    }

    private class Observatør : Arbeidsgiverperiodeteller.Observatør {
        var arbeidsgiverperiodeFerdig = false
        var arbeidsgiverperiodeAvbrutt = false
        var arbeidsgiverperioder = 0
        var avbrutteArbeidsgiverperioder = 0
        var arbeidsgiverperiodedager = 0
        var sykedager = 0

        override fun arbeidsgiverperiodeFerdig() {
            arbeidsgiverperiodeFerdig = true
            arbeidsgiverperioder += 1
        }

        override fun arbeidsgiverperiodeAvbrutt() {
            arbeidsgiverperiodeAvbrutt = true
            avbrutteArbeidsgiverperioder += 1
        }

        override fun arbeidsgiverperiodedag() {
            arbeidsgiverperiodedager += 1
        }

        override fun sykedag() {
            sykedager += 1
        }
    }
}

