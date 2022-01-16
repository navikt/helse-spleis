package no.nav.helse

import no.nav.helse.inspectors.UtbetalingstidslinjeInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.testhelpers.A
import no.nav.helse.testhelpers.F
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtbetalingstidslinjeBuilderTest {
    @Test
    fun kort() {
        undersøke(15.S)
        assertEquals(15, inspektør.size)
        assertEquals(15, inspektør.arbeidsgiverperiodeDagTeller)
    }

    @Test
    fun enkel() {
        undersøke(31.S)
        assertEquals(31, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(11, inspektør.navDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
    }

    @Test
    fun `ferie med i arbeidsgiverperioden`() {
        undersøke(6.S + 6.F + 6.S)
        assertEquals(18, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(2, inspektør.navDagTeller)
    }

    @Test
    fun `bare arbeidsdager`() {
        undersøke(31.A)
        assertEquals(31, inspektør.size)
        assertEquals(23, inspektør.arbeidsdagTeller)
        assertEquals(8, inspektør.fridagTeller)
    }

    @Test
    fun `spredt arbeidsgiverperiode`() {
        undersøke(10.S + 15.A + 7.S)
        assertEquals(32, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(1, inspektør.navDagTeller)
        assertEquals(11, inspektør.arbeidsdagTeller)
        assertEquals(4, inspektør.fridagTeller)
    }

    @Test
    fun `nok opphold til å tilbakestille arbeidsgiverperiode`() {
        undersøke(10.S + 16.A + 7.S)
        assertEquals(33, inspektør.size)
        assertEquals(17, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(0, inspektør.navDagTeller)
        assertEquals(12, inspektør.arbeidsdagTeller)
        assertEquals(4, inspektør.fridagTeller)
    }

    @BeforeEach
    fun setup() {
        resetSeed()
    }

    private lateinit var inspektør: UtbetalingstidslinjeInspektør
    private fun undersøke(tidslinje: Sykdomstidslinje) {
        val builder = UtbetalingstidslinjeBuilder(Arbeidsgiverperiodeteller.NormalArbeidstaker)
        tidslinje.accept(builder)
        inspektør = builder.result().inspektør
    }

    private class UtbetalingstidslinjeBuilder(private val arbeidsgiverperiodeteller: Arbeidsgiverperiodeteller) : SykdomstidslinjeVisitor,
        Arbeidsgiverperiodeteller.Observatør {
        private val tidslinje = Utbetalingstidslinje()

        init {
            arbeidsgiverperiodeteller.observer(this)
        }

        private var tilstand: Tilstand = Utbetaling

        internal fun result() = tidslinje

        private fun tilstand(tilstand: Tilstand) {
            this.tilstand = tilstand
        }

        override fun arbeidsgiverperiodedag() {
            tilstand(Arbeidsgiverperiode)
        }

        override fun sykedag() {
            tilstand(Utbetaling)
        }

        override fun visitDag(dag: Dag.Sykedag, dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            arbeidsgiverperiodeteller.inc()
            tilstand.sykdomsdag(this, dato, økonomi)
        }

        override fun visitDag(dag: Dag.SykHelgedag, dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            arbeidsgiverperiodeteller.inc()
            tilstand.sykdomshelg(this, dato, økonomi)
        }

        override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            arbeidsgiverperiodeteller.inc()
            tidslinje.addArbeidsgiverperiodedag(dato, Økonomi.ikkeBetalt())
        }

        override fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            arbeidsgiverperiodeteller.dec()
            tidslinje.addArbeidsdag(dato, Økonomi.ikkeBetalt())
        }

        override fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            arbeidsgiverperiodeteller.dec()
            tidslinje.addFridag(dato, Økonomi.ikkeBetalt())
        }

        private interface Tilstand {
            fun sykdomsdag(builder: UtbetalingstidslinjeBuilder, dato: LocalDate, økonomi: Økonomi)
            fun sykdomshelg(builder: UtbetalingstidslinjeBuilder, dato: LocalDate, økonomi: Økonomi)
        }
        private object Arbeidsgiverperiode : Tilstand {
            override fun sykdomsdag(builder: UtbetalingstidslinjeBuilder, dato: LocalDate, økonomi: Økonomi) {
                builder.tidslinje.addArbeidsgiverperiodedag(dato, økonomi)
            }
            override fun sykdomshelg(builder: UtbetalingstidslinjeBuilder, dato: LocalDate, økonomi: Økonomi) {
                builder.tidslinje.addArbeidsgiverperiodedag(dato, økonomi)
            }
        }
        private object Utbetaling : Tilstand {
            override fun sykdomsdag(builder: UtbetalingstidslinjeBuilder, dato: LocalDate, økonomi: Økonomi) {
                builder.tidslinje.addNAVdag(dato, økonomi)
            }
            override fun sykdomshelg(builder: UtbetalingstidslinjeBuilder, dato: LocalDate, økonomi: Økonomi) {
                builder.tidslinje.addHelg(dato, økonomi)
            }
        }
    }
}
