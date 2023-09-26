package no.nav.helse.utbetalingstidslinje

import no.nav.helse.desember
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.februar
import no.nav.helse.fredag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.lørdag
import no.nav.helse.mandag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.søndag
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode.Companion.finn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ArbeidsgiverperiodeTest {

    @Test
    fun `itererer over periode`() {
        val periode1 = 1.januar til 5.januar
        val periode2 = 10.januar til 18.januar
        val arbeidsgiverperiode = agp(periode1, periode2)
        assertEquals(periode1.toList() + periode2.toList(), arbeidsgiverperiode.toList())
    }

    @Test
    fun `arbeidsgiverperiode er den samme hvis første dag er lik`() {
        assertEquals(agp(1.januar til 5.januar), agp(1.januar til 16.januar))
        assertNotEquals(agp(1.januar til 5.januar), agp(2.januar til 17.januar))
    }

    @Test
    fun `Nav betaler arbeidsgiverperioden`() {
        val agp = agp(1.januar til 16.januar).utbetalingsdag(1.januar)
        assertTrue(agp.forventerInntekt(1.januar til 16.januar, Sykdomstidslinje(), SubsumsjonObserver.NullObserver))
        assertTrue(agp.forventerOpplysninger(1.januar til 16.januar))
        assertFalse(agp.forventerOpplysninger(20.desember(2017) til 31.desember(2017)))
        assertTrue(agp.forventerOpplysninger(20.desember(2017) til 1.januar))
        assertFalse(agp.forventerOpplysninger(2.januar til 16.januar))
    }

    @Test
    fun `arbeidsgiverperiode anses som like om de slutter på samme dag`() {
        assertTrue(agp(1.januar til 16.januar).sammenlign(listOf(16.januar til 16.januar)))
        assertFalse(agp(1.januar til 16.januar).apply { kjentDag(17.januar) }.sammenlign(listOf(16.januar til 17.januar)))
        assertFalse(agp(1.januar til 16.januar).sammenlign(listOf(15.januar til 15.januar)))
    }

    @Test
    fun `arbeidsgiverperiode anses som like om de slutter på fredag eller helg`() {
        assertTrue(agp(12.januar til 27.januar).sammenlign(listOf(11.januar til 26.januar)))
        assertTrue(agp(12.januar til 27.januar).sammenlign(listOf(13.januar til 28.januar)))
        assertFalse(agp(12.januar til 27.januar).apply { kjentDag(29.januar) }.sammenlign(listOf(14.januar til 29.januar)))
        assertTrue(agp(12.januar til 27.januar).sammenlign(listOf(14.januar til 29.januar)))

        assertTrue(agp(11.januar til 26.januar).sammenlign(listOf(12.januar til 27.januar)))
        assertTrue(agp(13.januar til 28.januar).sammenlign(listOf(12.januar til 27.januar)))
        assertFalse(agp(14.januar til 29.januar).sammenlign(listOf(12.januar til 27.januar)))
        assertFalse(agp(14.januar til 29.januar).sammenlign(emptyList()))
    }

    @Test
    fun `lita hale`() {
        assertTrue(agp(1.januar til 15.januar).sammenlign(listOf(1.januar til 16.januar)))
    }

    @Test
    fun `har betalt`() {
        assertFalse(agp(1.januar til 16.januar).erFørsteUtbetalingsdagFørEllerLik(1.januar til 17.januar))
        assertTrue(agp(1.januar til 16.januar).utbetalingsdag(17.januar).erFørsteUtbetalingsdagFørEllerLik(1.januar til 17.januar))
        assertTrue(agp(1.januar til 16.januar).utbetalingsdag(17.januar).erFørsteUtbetalingsdagFørEllerLik(1.januar til 18.januar))
        assertFalse(agp(1.januar til 16.januar).utbetalingsdag(18.januar).erFørsteUtbetalingsdagFørEllerLik(1.januar til 17.januar))
    }

    @Test
    fun `helg regnes ikke som betalt`() {
        assertFalse(agp(1.januar til 16.januar).utbetalingsdag(20.januar).erFørsteUtbetalingsdagFørEllerLik(1.januar til 20.januar))
        assertFalse(Arbeidsgiverperiode.fiktiv(20.januar).erFørsteUtbetalingsdagFørEllerLik(1.januar til 20.januar))
    }

    @Test
    fun `ingen utbetaling dersom perioden er innenfor arbeidsgiverperioden eller før det utbetales noe`() {
        agp(1.januar til 16.januar).utbetalingsdag(23.januar).also { agp ->
            assertFalse(agp.forventerInntekt(31.desember(2017) til 5.januar, Sykdomstidslinje(), SubsumsjonObserver.NullObserver))
            assertFalse(agp.forventerInntekt(15.januar til 22.januar, Sykdomstidslinje(), SubsumsjonObserver.NullObserver))
            assertTrue(agp.forventerInntekt(15.januar til 23.januar, Sykdomstidslinje(), SubsumsjonObserver.NullObserver))
            assertFalse(agp.forventerOpplysninger(31.desember(2017) til 5.januar))
            assertFalse(agp.forventerOpplysninger(15.januar til 22.januar))
            assertTrue(agp.forventerOpplysninger(15.januar til 23.januar))
        }
    }

    @Test
    fun `forventer opplysninger fra arbeidsgiver etter periode med opphold`() {
        agp(1.januar til 16.januar).utbetalingsdag(17.januar).oppholdsdag(18.januar).utbetalingsdag(fredag den 19.januar).utbetalingsdag(mandag den 22.januar).also { agp ->
            assertFalse(agp.forventerOpplysninger(18.januar.somPeriode()))
            assertTrue(agp.forventerOpplysninger(17.januar.somPeriode()))
            assertTrue(agp.forventerOpplysninger(18.januar til 19.januar))
            assertTrue(agp.forventerOpplysninger(19.januar til 20.januar))
            assertFalse(agp.forventerOpplysninger(20.januar til 21.januar))
            assertFalse(agp.forventerOpplysninger(mandag den 22.januar til 23.januar))
        }
    }

    @Test
    fun `forventer opplysninger fra arbeidsgiver etter periode med opphold i helg`() {
        agp(1.januar til 16.januar).utbetalingsdag(17.januar).utbetalingsdag(18.januar).utbetalingsdag(fredag den 19.januar).oppholdsdag(lørdag den 20.januar).utbetalingsdag(mandag den 22.januar).also { agp ->
            assertTrue(agp.forventerOpplysninger(17.januar til 20.januar))
            assertFalse(agp.forventerInntekt(20.januar til 21.januar, Sykdomstidslinje(), SubsumsjonObserver.NullObserver))
            assertFalse(agp.forventerOpplysninger(20.januar til 21.januar))
            assertTrue(agp.forventerOpplysninger(22.januar til 23.januar))
        }
    }

    @Test
    fun `forventer ikke opplysninger fra arbeidsgiver etter periode med kun helg`() {
        agp(1.januar til 16.januar).utbetalingsdag(17.januar).utbetalingsdag(18.januar).utbetalingsdag(fredag den 19.januar).utbetalingsdag(mandag den 22.januar).also { agp ->
            assertTrue(agp.forventerOpplysninger(17.januar til 20.januar))
            assertTrue(agp.forventerInntekt(lørdag den 20.januar til (søndag den 21.januar), Sykdomstidslinje(), SubsumsjonObserver.NullObserver))
            assertFalse(agp.forventerOpplysninger(lørdag den 20.januar til (søndag den 21.januar)))
            assertFalse(agp.forventerOpplysninger(20.januar til 21.januar))
            assertFalse(agp.forventerOpplysninger(22.januar til 23.januar))
            assertFalse(agp.forventerOpplysninger(lørdag den 20.januar til (mandag den 22.januar)))
        }
    }

    @Test
    fun `inneholder dager`() {
        val periode = 2.januar til 5.januar
        val arbeidsgiverperiode = agp(periode)
        assertFalse(1.januar in arbeidsgiverperiode)
        assertTrue(2.januar in arbeidsgiverperiode)
        assertTrue(5.januar in arbeidsgiverperiode)
        assertFalse(6.januar in arbeidsgiverperiode) // lørdag
        assertFalse(7.januar in arbeidsgiverperiode) // søndag
        assertFalse(8.januar in arbeidsgiverperiode) // søndag
    }

    @Test
    fun `dekker hele perioden`() {
        val periode = 2.januar til 5.januar
        val arbeidsgiverperiode = agp(periode)
        assertTrue(arbeidsgiverperiode.dekkesAvArbeidsgiver(periode))
        assertTrue(arbeidsgiverperiode.dekkesAvArbeidsgiver(3.januar til 4.januar))
        assertTrue(arbeidsgiverperiode.dekkesAvArbeidsgiver(2.januar til 6.januar))
        assertFalse(arbeidsgiverperiode.dekkesAvArbeidsgiver(2.januar til 8.januar))
        assertTrue(arbeidsgiverperiode.dekkesAvArbeidsgiver(1.januar til 5.januar))
        assertTrue(arbeidsgiverperiode.dekkesAvArbeidsgiver(6.januar til 7.januar))
        assertTrue(arbeidsgiverperiode.dekkesAvArbeidsgiver(1.januar til 6.januar))
        assertFalse(arbeidsgiverperiode.dekkesAvArbeidsgiver(1.januar til 8.januar))
        assertFalse(arbeidsgiverperiode.dekkesAvArbeidsgiver(1.januar til 1.januar))
    }

    @Test
    fun `fiktiv periode dekker ingenting fordi arbeidsgiverperioden er ukjent`() {
        val arbeidsgiverperiode = Arbeidsgiverperiode.fiktiv(2.januar)
        assertFalse(arbeidsgiverperiode.dekkesAvArbeidsgiver(2.januar til 2.januar))
        assertFalse(arbeidsgiverperiode.dekkesAvArbeidsgiver(1.januar til 1.januar))
    }

    @Test
    fun `hører til`() {
        val periode = 2.januar til 5.januar
        val arbeidsgiverperiode = agp(periode)
        assertTrue(arbeidsgiverperiode.hørerTil(periode))
        assertTrue(arbeidsgiverperiode.hørerTil(1.januar til 2.januar))
        assertFalse(arbeidsgiverperiode.hørerTil(6.januar til 9.januar))
        assertFalse(arbeidsgiverperiode.hørerTil(1.januar til 1.januar))
        arbeidsgiverperiode.kjentDag(6.januar)
        assertTrue(arbeidsgiverperiode.hørerTil(6.januar til 9.januar))
        arbeidsgiverperiode.kjentDag(10.januar)
        assertTrue(arbeidsgiverperiode.hørerTil(7.januar til 9.januar))
        assertFalse(arbeidsgiverperiode.hørerTil(11.januar til 12.januar))
    }

    @Test
    fun `hører til sist kjente`() {
        val periode = 2.januar til 5.januar
        val arbeidsgiverperiode = agp(periode)
        assertTrue(arbeidsgiverperiode.hørerTil(periode, 5.januar))
        assertTrue(arbeidsgiverperiode.hørerTil(1.januar til 2.januar, 5.januar))
        assertFalse(arbeidsgiverperiode.hørerTil(6.januar til 9.januar, 5.januar))
        assertFalse(arbeidsgiverperiode.hørerTil(1.januar til 1.januar, 5.januar))
        assertTrue(arbeidsgiverperiode.hørerTil(6.januar til 9.januar, 6.januar))
        assertTrue(arbeidsgiverperiode.hørerTil(6.januar til 9.januar, 10.januar))
        assertFalse(arbeidsgiverperiode.hørerTil(11.januar til 12.januar, 10.januar))
    }

    @Test
    fun `inneholder periode`() {
        val periode = 2.januar til 5.januar
        val arbeidsgiverperiode = agp(periode)
        assertTrue(periode in arbeidsgiverperiode)
        assertTrue(3.januar til 4.januar in arbeidsgiverperiode)
        assertTrue(2.januar til 6.januar in arbeidsgiverperiode)
        assertTrue(1.januar til 5.januar in arbeidsgiverperiode)
        assertTrue(1.januar til 6.januar in arbeidsgiverperiode)
        assertFalse(1.januar til 1.januar in arbeidsgiverperiode)
        assertFalse(6.januar til 7.januar in arbeidsgiverperiode)
    }

    @Test
    fun `finner riktig arbeidsgiverperiode`() {
        val første = Arbeidsgiverperiode(listOf(1.januar til 16.januar))
        val andre = Arbeidsgiverperiode.fiktiv(1.februar)
        val perioder = listOf(første.also { it.kjentDag(17.januar) }, andre)

        assertEquals(første, perioder.finn(1.januar til 16.januar))
        assertEquals(første, perioder.finn(17.januar til 18.januar))
        assertNull(perioder.finn(18.januar til 31.januar))
        assertEquals(andre, perioder.finn(1.februar til 18.februar))
        assertNull(perioder.finn(2.februar til 18.februar))
    }

    private fun agp(vararg periode: Periode) = Arbeidsgiverperiode(periode.toList())
}
