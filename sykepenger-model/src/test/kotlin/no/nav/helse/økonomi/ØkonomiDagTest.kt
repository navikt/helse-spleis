package no.nav.helse.økonomi

import no.nav.helse.*
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.AVV
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.AvvistDag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ØkonomiDagTest {

    @Test
    fun `Beløp er ikke 6G-begrenset`() {
        val a = tidslinjeOf(2.NAV(500))
        val b = tidslinjeOf(2.NAV(500))
        val c = tidslinjeOf(2.NAV(500))
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg(), 1.januar).betal()
        assertØkonomi(a, 500.0)
        assertØkonomi(b, 500.0)
        assertØkonomi(c, 500.0)
    }

    @Test
    fun `Dekningsgrunnlag med desimaler`() {
        val a = tidslinjeOf(2.NAV(1200.75, 50.0))
        MaksimumUtbetaling(listOf(a), Aktivitetslogg(), 1.januar).betal()
        assertØkonomi(a, 600.0)
    }

    @Test
    fun `Dekningsgrunnlag uten desimaler`() {
        val a = tidslinjeOf(2.NAV(1201, 50.0))
        MaksimumUtbetaling(listOf(a), Aktivitetslogg(), 1.januar).betal()
        assertØkonomi(a, 601.0)
    }

    @Test
    fun `Beløp er 6G-begrenset`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.NAV(1200))
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg(), 1.januar).betal()
        assertØkonomi(a, 721.0)
        assertØkonomi(b, 720.0)
        assertØkonomi(c, 720.0)
    }

    @Test
    fun `bruker riktig G-verdi ved 6G-begrensning`() {
        tidslinjeOf(2.NAV(3000)).let { tidslinje ->
            MaksimumUtbetaling(listOf(tidslinje), Aktivitetslogg(), 2.januar).betal()
            assertØkonomi(tidslinje, 2161.0)
        }
        tidslinjeOf(2.NAV(3000), startDato = 30.april).let { tidslinje ->
            MaksimumUtbetaling(listOf(tidslinje), Aktivitetslogg(), 2.mai).betal()
            assertØkonomi(tidslinje, 2161.0)
        }
    }

    @Test
    fun `bruker virkningsdato for å finne aktuell G-verdi ved begrensning`() {
        val skjæringstidspunkt = 1.mai(2020)
        val virkningsdatoForNyttGrunnbeløp = 21.september(2020)
        tidslinjeOf(2.NAV(3000), startDato = skjæringstidspunkt).let { tidslinje ->
            MaksimumUtbetaling(listOf(tidslinje), Aktivitetslogg(), skjæringstidspunkt).betal()
            assertØkonomi(tidslinje, Grunnbeløp.`6G`.beløp(skjæringstidspunkt).rundTilDaglig().reflection { _, _, daglig, _ -> daglig}) // 2019-grunnbeløp
        }
        tidslinjeOf(2.NAV(3000), startDato = skjæringstidspunkt).let { tidslinje ->
            MaksimumUtbetaling(listOf(tidslinje), Aktivitetslogg(), virkningsdatoForNyttGrunnbeløp).betal()
            assertØkonomi(tidslinje, Grunnbeløp.`6G`.beløp(skjæringstidspunkt, virkningsdatoForNyttGrunnbeløp).rundTilDaglig().reflection { _, _, daglig, _ -> daglig}) // 2020-grunnbeløp fordi virkningsdato er passert
        }
    }

    @Test
    fun `Beløp med arbeidsdag`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.ARB(1200))
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg(), 1.januar).betal()
        assertØkonomi(a, 721.0)
        assertØkonomi(b, 720.0)
        assertØkonomi(c, 0.0)
    }

    @Test
    fun `Beløp medNavDag som har blitt avvist`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.NAV(1200))
            .onEach { it.avvis(listOf(Begrunnelse.MinimumInntekt)) }
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg(), 1.januar).betal()
        assertØkonomi(a, 721.0)
        assertØkonomi(b, 720.0)
        assertØkonomi(c, 0.0)
    }

    @Test
    fun `Beløp med avvistdager`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.AVV(1200, 100))
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg(), 1.januar).betal()
        assertØkonomi(a, 721.0)
        assertØkonomi(b, 720.0)
        assertØkonomi(c, 0.0)
    }

    @Test
    fun `Beløp med avvistdager som er låst opp`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.AVV(1200, 100))
            .onEach { (it as AvvistDag).navDag() }
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg(), 1.januar).betal()
        assertØkonomi(a, 721.0)
        assertØkonomi(b, 720.0)
        assertØkonomi(c, 720.0)
    }

    private fun assertØkonomi(tidslinje: Utbetalingstidslinje, arbeidsgiverbeløp: Double, personbeløp: Double = 0.0) {
        tidslinje.forEach {
            it.økonomi.medData {
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
