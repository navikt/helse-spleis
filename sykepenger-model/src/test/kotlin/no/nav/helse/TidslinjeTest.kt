package no.nav.helse

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows

internal class TidslinjeTest {

    private class TestTidslinje(vararg perioder: Pair<Periode, Int>): Tidslinje<Int, TestTidslinje>(*perioder) {
        override fun opprett(vararg perioder: Pair<Periode, Int>) = TestTidslinje(*perioder)
    }

    @Test
    fun `en tidslinje med tall`() {
        val tidslinje = TestTidslinje(2.januar.somPeriode() to 1) + TestTidslinje(10.januar til 1.februar to 50)

        assertNull(tidslinje[1.januar])

        assertEquals(1, tidslinje[2.januar])

        (3.januar til 9.januar).forEach { dato -> assertNull(tidslinje[dato]) }

        (10.januar til 1.februar).forEach { dato -> assertEquals(50, tidslinje[dato]) }

        assertNull(tidslinje[2.februar])

        val enLikTidslinje =
           TestTidslinje(2.januar til 2.januar to 1) + TestTidslinje(10.januar til 1.februar to 50)

        assertEquals(tidslinje, enLikTidslinje)

        val sammeDatoerMenAndreProsenter =
           TestTidslinje(2.januar til 2.januar to 1) + TestTidslinje(10.januar til 1.februar to 51)

        Assertions.assertNotEquals(tidslinje, sammeDatoerMenAndreProsenter)
    }

    @Test
    fun `plus med default regler`() {
        val tidslinje1 = TestTidslinje(februar to 100)
        val tidslinje2 = TestTidslinje(31.januar til 1.mars to 12)
        assertEquals(tidslinje2, tidslinje1.plus(tidslinje2))
    }

    @Test
    fun `plus med egne regler`() {
        class PlussendeTalltidslinje(vararg perioder: Pair<Periode, Int>): Tidslinje<Int, PlussendeTalltidslinje>(*perioder) {
            override fun opprett(vararg perioder: Pair<Periode, Int>) = PlussendeTalltidslinje(*perioder)
            override fun pluss(eksisterendeVerdi: Int, nyVerdi: Int) = eksisterendeVerdi + nyVerdi
        }
        val tidslinje1 = PlussendeTalltidslinje(februar to 100)
        val tidslinje2 = PlussendeTalltidslinje(31.januar til 1.mars to 12)
        val forventet = PlussendeTalltidslinje(31.januar.somPeriode() to 12) + PlussendeTalltidslinje(februar to 112) + PlussendeTalltidslinje(1.mars.somPeriode() to 12)
        assertEquals(forventet, tidslinje1 + tidslinje2)
        assertEquals(forventet, tidslinje2 + tidslinje1)
    }

    @Test
    fun `gruppér med default regler`() {
        val februarTidslinje = februar.fold(TestTidslinje()) { sammenslått, dato -> sammenslått + TestTidslinje(dato.somPeriode() to 1) }
        val marsTidslinje = TestTidslinje(mars to 1)
        val haleMaiSnuteJuniTidslinje = TestTidslinje(30.mai til 1.juni to 2)
        val juniTidslinje = TestTidslinje(2.juni til 15.juni to 1)

        val sammenslått = februarTidslinje + marsTidslinje + haleMaiSnuteJuniTidslinje + juniTidslinje

        assertEquals(
            mapOf(
                1.februar til 31.mars to 1,
                30.mai til 1.juni to 2,
                2.juni til 15.juni to 1
            ), sammenslått.gruppér()
        )
    }

    @Test
    fun `gruppér med egne regler`() {
        class IngentallErLikeTalltidslinje(vararg perioder: Pair<Periode, Int>): Tidslinje<Int, IngentallErLikeTalltidslinje>(*perioder) {
            override fun opprett(vararg perioder: Pair<Periode, Int>) = IngentallErLikeTalltidslinje(*perioder)
            override fun erLike(a: Int, b: Int) = false
        }

        val februarTidslinje: IngentallErLikeTalltidslinje = februar.fold(IngentallErLikeTalltidslinje()) { sammenslått, dato ->
            sammenslått + IngentallErLikeTalltidslinje(dato.somPeriode() to 1)
        }
        val marsTidslinje = IngentallErLikeTalltidslinje(mars to 1)
        val haleMaiSnuteJuniTidslinje = IngentallErLikeTalltidslinje(30.mai til 1.juni to 1)
        val juniTidslinje = IngentallErLikeTalltidslinje(2.juni til 15.juni to 1)

        val sammenslått = februarTidslinje + marsTidslinje + haleMaiSnuteJuniTidslinje + juniTidslinje
        val forventet =
            februar.associate { it.somPeriode() to 1 } +
            mars.associate { it.somPeriode() to 1 } +
            (30.mai til 1.juni).associate { it.somPeriode() to 1 } +
            (2.juni til 15.juni).associate { it.somPeriode() to 1 }

        assertEquals(forventet, sammenslått.gruppér())
    }

    @Test
    fun subset() {
        val tidslinje = TestTidslinje(februar to 4)
        assertEquals(
            TestTidslinje(5.februar til 25.februar to 4),
            tidslinje.subset(5.februar til 25.februar)
        )
        assertEquals(TestTidslinje(), tidslinje.subset(januar))
        assertEquals(TestTidslinje(), tidslinje.subset(april))
    }

    @Test
    fun `fraOgMed & tilOgMed`() {
        val tidslinje = TestTidslinje(januar to 1) + TestTidslinje(mars to 3)
        assertEquals(TestTidslinje(5.januar til 31.januar to 1, mars to 3), tidslinje.fraOgMed(5.januar))
        assertEquals(TestTidslinje(januar to 1) + TestTidslinje(1.mars til 25.mars to 3), tidslinje.tilOgMed(25.mars))
    }

    @Test
    fun `ugyldig initialisering av tidslinje`() {
        val exception = assertThrows<IllegalArgumentException> { TestTidslinje(januar to 5, 10.januar.somPeriode() to 7) }
        assertEquals("Datoen 2018-01-10 er oppgitt fler ganger i samme tidslinje.", exception.message)
    }

    @Test
    fun `iterere tidslinje med hull`() {
        val tidslinje = TestTidslinje(januar to 1, mars to 3)
        assertEquals((31 + 31), tidslinje.count())
        assertEquals((31 + 28 + 31), tidslinje.iterator().asSequence().count())
        tidslinje.forEach { tidslinjedag ->
            when (tidslinjedag.dato) {
                in januar -> assertEquals(1, tidslinjedag.verdi)
                in februar -> assertNull(tidslinjedag.verdi)
                in mars -> assertEquals(3, tidslinjedag.verdi)
                else -> error("Uventet dato ${tidslinjedag.dato}")
            }
        }
    }
}
