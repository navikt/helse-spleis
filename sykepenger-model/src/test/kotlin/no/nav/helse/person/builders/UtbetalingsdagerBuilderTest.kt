package no.nav.helse.person.builders

import no.nav.helse.januar
import no.nav.helse.person.PersonObserver.Utbetalingsdag
import no.nav.helse.person.PersonObserver.Utbetalingsdag.Dagtype.*
import no.nav.helse.serde.api.BegrunnelseDTO
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.UkjentDag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtbetalingsdagerBuilderTest {

    @BeforeEach
    fun reset() {
        resetSeed()
    }

    @Test
    fun `bygger utbetalingsdager`() {
        val builder = UtbetalingsdagerBuilder(Sykdomstidslinje())
        val utbetalingstidslinje = tidslinjeOf(1.AP, 1.NAV, 1.HELG, 1.ARB, 1.FRI, 1.FOR, 1.AVV)
        utbetalingstidslinje.accept(builder)
        assertEquals(
            listOf(
                Utbetalingsdag(1.januar, ArbeidsgiverperiodeDag),
                Utbetalingsdag(2.januar, NavDag),
                Utbetalingsdag(3.januar, NavHelgDag),
                Utbetalingsdag(4.januar, Arbeidsdag),
                Utbetalingsdag(5.januar, Fridag),
                Utbetalingsdag(6.januar, ForeldetDag),
                Utbetalingsdag(7.januar, AvvistDag, listOf(BegrunnelseDTO.SykepengedagerOppbrukt))
            ), builder.result()
        )
    }

    @Test
    fun `tidslinje med fridager`() {
        val builder = UtbetalingsdagerBuilder(1.P + 4.F + 1.UK)
        val utbetalingstidslinje = tidslinjeOf(6.FRI)
        utbetalingstidslinje.accept(builder)
        assertEquals(
            listOf(
                Utbetalingsdag(1.januar, Permisjonsdag),
                Utbetalingsdag(2.januar, Feriedag),
                Utbetalingsdag(3.januar, Feriedag),
                Utbetalingsdag(4.januar, Feriedag),
                Utbetalingsdag(5.januar, Feriedag),
                Utbetalingsdag(6.januar, Fridag)
            ), builder.result()
        )
    }

    @Test
    fun `tidslinje med ukjentdag`() {
        val builder = UtbetalingsdagerBuilder(Sykdomstidslinje())
        val utbetalingstidslinje = tidslinjeOf(3.NAV).plus(tidslinjeOf(1.UTELATE, 1.NAV)) { venstre, høyre -> when (venstre) {
            is Utbetalingstidslinje.Utbetalingsdag.NavDag -> when (høyre) {
                is Utbetalingstidslinje.Utbetalingsdag.NavDag -> UkjentDag(venstre.dato, venstre.økonomi)
                else -> venstre
            }
            else -> høyre
        }}
        utbetalingstidslinje.accept(builder)
        assertEquals(listOf(
            Utbetalingsdag(1.januar, NavDag),
            Utbetalingsdag(2.januar, UkjentDag),
            Utbetalingsdag(3.januar, NavDag)
        ), builder.result())
    }
}
