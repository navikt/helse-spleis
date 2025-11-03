package no.nav.helse.person.builders

import no.nav.helse.januar
import no.nav.helse.person.EventSubscription.Utbetalingsdag
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype.ArbeidIkkeGjenopptattDag
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype.Arbeidsdag
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype.ArbeidsgiverperiodeDag
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype.AvvistDag
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype.Feriedag
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype.ForeldetDag
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype.Fridag
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype.NavDag
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype.NavHelgDag
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype.Permisjonsdag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.AIG
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.AVV
import no.nav.helse.testhelpers.F
import no.nav.helse.testhelpers.FOR
import no.nav.helse.testhelpers.FRI
import no.nav.helse.testhelpers.HELG
import no.nav.helse.testhelpers.NAP
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.P
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.UK
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtbetalingsdagerBuilderTest {

    @BeforeEach
    fun reset() {
        resetSeed()
    }

    @Test
    fun `bygger utbetalingsdager`() {
        val utbetalingstidslinje = tidslinjeOf(1.AP, 1.NAV, 1.HELG, 1.ARB, 1.FRI, 1.FOR, 1.AVV)
        val builder = UtbetalingsdagerBuilder(Sykdomstidslinje())
        assertEquals(
            listOf(
                Utbetalingsdag(1.januar, ArbeidsgiverperiodeDag, 100),
                Utbetalingsdag(2.januar, NavDag, 0, 0, 100, 100, null),
                Utbetalingsdag(3.januar, NavHelgDag, 0, 0, 100, 100, null),
                Utbetalingsdag(4.januar, Arbeidsdag, 100),
                Utbetalingsdag(5.januar, Fridag, 100),
                Utbetalingsdag(6.januar, ForeldetDag, 100),
                Utbetalingsdag(7.januar, AvvistDag, 0, 0, 0, 100, listOf(Utbetalingsdag.EksternBegrunnelseDTO.SykepengedagerOppbrukt))
            ), builder.result(utbetalingstidslinje)
        )
    }

    @Test
    fun `tidslinje med fridager`() {
        val utbetalingstidslinje = tidslinjeOf(6.FRI)
        val builder = UtbetalingsdagerBuilder(1.P + 3.F + 1.AIG + 1.UK)
        assertEquals(
            listOf(
                Utbetalingsdag(1.januar, Permisjonsdag, 100),
                Utbetalingsdag(2.januar, Feriedag, 100),
                Utbetalingsdag(3.januar, Feriedag, 100),
                Utbetalingsdag(4.januar, Feriedag, 100),
                Utbetalingsdag(5.januar, ArbeidIkkeGjenopptattDag, 100),
                Utbetalingsdag(6.januar, Fridag, 100)
            ), builder.result(utbetalingstidslinje)
        )
    }

    @Test
    fun `tidslinje med ArbeidsgiverperiodeDagerNav`() {
        val utbetalingstidslinje = tidslinjeOf(6.NAP)
        val builder = UtbetalingsdagerBuilder(6.S)
        assertEquals(
            listOf(
                Utbetalingsdag(1.januar, ArbeidsgiverperiodeDag, 0, 0, 100, 100, null),
                Utbetalingsdag(2.januar, ArbeidsgiverperiodeDag, 0, 0, 100, 100, null),
                Utbetalingsdag(3.januar, ArbeidsgiverperiodeDag, 0, 0, 100, 100, null),
                Utbetalingsdag(4.januar, ArbeidsgiverperiodeDag, 0, 0, 100, 100, null),
                Utbetalingsdag(5.januar, ArbeidsgiverperiodeDag, 0, 0, 100, 100, null),
                Utbetalingsdag(6.januar, ArbeidsgiverperiodeDag, 0, 0, 100, 100, null)
            ), builder.result(utbetalingstidslinje)
        )
    }
}
