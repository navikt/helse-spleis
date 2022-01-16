package no.nav.helse

import no.nav.helse.inspectors.UtbetalingstidslinjeInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.testhelpers.S
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtbetalingstidslinjeBuilderTest {
    @Test
    fun kort() {
        undersøke(15.S)
        Assertions.assertEquals(15, inspektør.size)
        Assertions.assertEquals(15, inspektør.arbeidsgiverperiodeDagTeller)
    }

    @Test
    fun enkel() {
        undersøke(31.S)
        Assertions.assertEquals(31, inspektør.size)
        Assertions.assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        Assertions.assertEquals(10, inspektør.navDagTeller)
        Assertions.assertEquals(5, inspektør.navHelgDagTeller)
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
