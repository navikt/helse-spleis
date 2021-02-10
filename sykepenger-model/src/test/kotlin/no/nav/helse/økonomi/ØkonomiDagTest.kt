package no.nav.helse.økonomi

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.*
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
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg(), 1.januar).betal()
        assertØkonomi(a, 500)
        assertØkonomi(b, 500)
        assertØkonomi(c, 500)
    }

    @Test
    fun `Dekningsgrunnlag med desimaler`() {
        val a = tidslinjeOf(2.NAV(1200.75, 50.0))
        MaksimumUtbetaling(listOf(a), Aktivitetslogg(), 1.januar).betal()
        assertØkonomi(a, 600)
    }

    @Test
    fun `Dekningsgrunnlag uten desimaler`() {
        val a = tidslinjeOf(2.NAV(1201, 50.0))
        MaksimumUtbetaling(listOf(a), Aktivitetslogg(), 1.januar).betal()
        assertØkonomi(a, 601)
    }

    @Test
    fun `Beløp er 6G-begrenset`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.NAV(1200))
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg(), 1.januar).betal()
        assertØkonomi(a, 721)
        assertØkonomi(b, 720)
        assertØkonomi(c, 720)
    }

    @Test
    fun `bruker riktig G-verdi ved 6G-begrensning`() {
        tidslinjeOf(2.NAV(3000)).let { tidslinje ->
            MaksimumUtbetaling(listOf(tidslinje), Aktivitetslogg(), 2.januar).betal()
            assertØkonomi(tidslinje, 2161)
        }
        tidslinjeOf(2.NAV(3000), startDato = 30.april).let { tidslinje ->
            MaksimumUtbetaling(listOf(tidslinje), Aktivitetslogg(), 2.mai).betal()
            assertØkonomi(tidslinje, 2161)
        }
    }

    @Test
    fun `bruker virkningsdato for å finne aktuell G-verdi ved begrensning`() {
        val skjæringstidspunkt = 1.mai(2020)
        val virkningsdatoForNyttGrunnbeløp = 21.september(2020)
        tidslinjeOf(2.NAV(3000), startDato = skjæringstidspunkt).let { tidslinje ->
            MaksimumUtbetaling(listOf(tidslinje), Aktivitetslogg(), skjæringstidspunkt).betal()
            assertØkonomi(tidslinje, Grunnbeløp.`6G`.beløp(skjæringstidspunkt).rundTilDaglig().reflection { _, _, _, daglig -> daglig}) // 2019-grunnbeløp
        }
        tidslinjeOf(2.NAV(3000), startDato = skjæringstidspunkt).let { tidslinje ->
            MaksimumUtbetaling(listOf(tidslinje), Aktivitetslogg(), virkningsdatoForNyttGrunnbeløp).betal()
            assertØkonomi(tidslinje, Grunnbeløp.`6G`.beløp(skjæringstidspunkt, virkningsdatoForNyttGrunnbeløp).rundTilDaglig().reflection { _, _, _, daglig -> daglig}) // 2020-grunnbeløp fordi virkningsdato er passert
        }
    }

    @Test
    fun `Beløp med arbeidsdag`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.ARB(1200))
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg(), 1.januar).betal()
        assertØkonomi(a, 724)
        assertØkonomi(b, 724)
        assertØkonomi(c, 0)
    }

    @Test
    fun `Beløp medNavDag som har blitt avvist`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.NAV(1200))
            .onEach { (it as NavDag).avvistDag(Begrunnelse.MinimumInntekt) }
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg(), 1.januar).betal()
        assertØkonomi(a, 724)
        assertØkonomi(b, 724)
        assertØkonomi(c, 0)
    }

    @Test
    fun `Beløp med avvistdager`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.AVV(1200, 100))
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg(), 1.januar).betal()
        assertØkonomi(a, 724)
        assertØkonomi(b, 724)
        assertØkonomi(c, 0)
    }

    @Test
    fun `Beløp med avvistdager som er låst opp`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.AVV(1200, 100))
            .onEach { (it as AvvistDag).navDag() }
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg(), 1.januar).betal()
        assertØkonomi(a, 721)
        assertØkonomi(b, 720)
        assertØkonomi(c, 720)
    }

    private fun assertØkonomi(tidslinje: Utbetalingstidslinje, arbeidsgiverbeløp: Int, personbeløp: Int = 0) {
        tidslinje.forEach {
            it.økonomi.reflection {
                    _,
                    _,
                    _,
                    _,
                    _,
                    _,
                    actualArbeidsgiverbeløp,
                    actualPersonbeløp,
                    _ ->
                assertEquals(arbeidsgiverbeløp, actualArbeidsgiverbeløp)
                assertEquals(personbeløp, actualPersonbeløp)
            }
        }
    }
}
