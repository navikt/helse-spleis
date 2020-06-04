package no.nav.helse.økonomi

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.AVV
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ØkonomiDagTest {

    @Test
    fun `Beløp er ikke 6G-begrenset`() {
        val a = tidslinjeOf(2.NAV(500))
        val b = tidslinjeOf(2.NAV(500))
        val c = tidslinjeOf(2.NAV(500))
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg()).betal()
        assertØkonomi(a, 500)
        assertØkonomi(b, 500)
        assertØkonomi(c, 500)
    }

    @Test
    fun `Beløp er 6G-begrenset`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.NAV(1200))
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg()).betal()
        assertØkonomi(a, 721)
        assertØkonomi(b, 720)
        assertØkonomi(c, 720)
    }

    @Test
    fun `Beløp med arbeidsdag`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.ARB(1200))
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg()).betal()
        assertØkonomi(a, 721)
        assertØkonomi(b, 720)
        assertØkonomi(c, 0)
    }

    @Test
    fun `Beløp medNavDag som har blitt avvist`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.NAV(1200))
            .onEach { (it as NavDag).avvistDag(Begrunnelse.MinimumInntekt) }
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg()).betal()
        assertØkonomi(a, 721)
        assertØkonomi(b, 720)
        assertØkonomi(c, 0)
    }

    @Test
    fun `Beløp med avvistdager`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.AVV(1200, 100))
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg()).betal()
        assertØkonomi(a, 721)
        assertØkonomi(b, 720)
        assertØkonomi(c, 0)
    }

    @Test
    fun `Beløp med avvistdager som er låst opp`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.AVV(1200, 100))
            .onEach { (it as AvvistDag).navDag() }
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg()).betal()
        assertØkonomi(a, 721)
        assertØkonomi(b, 720)
        assertØkonomi(c, 720)
    }

    private fun assertØkonomi(tidslinje: Utbetalingstidslinje, arbeidsgiverbeløp: Int, personbeløp: Int = 0) {
        tidslinje.forEach {
            it.økonomi.toMap().also { map ->
                assertEquals(arbeidsgiverbeløp, map["arbeidsgiverbeløp"])
                assertEquals(personbeløp, map["personbeløp"])
            }
        }
    }
}
