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
    fun `ferie fullfører arbeidsgiverperioden`() {
        undersøke(1.S + 15.F + 6.S)
        assertEquals(22, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(4, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
    }

    @Test
    fun `ferie etter utbetaling`() {
        undersøke(16.S + 15.F)
        assertEquals(31, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(15, inspektør.fridagTeller)
    }

    @Test
    fun `ferie mellom utbetaling`() {
        undersøke(16.S + 10.F + 5.S)
        assertEquals(31, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(3, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
        assertEquals(10, inspektør.fridagTeller)
    }

    @Test
    fun `ferie før arbeidsdag tilbakestiller arbeidsgiverperioden`() {
        undersøke(1.S + 15.F + 1.A + 16.S)
        assertEquals(33, inspektør.size)
        assertEquals(17, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(15, inspektør.fridagTeller)
        assertEquals(1, inspektør.arbeidsdagTeller)
    }

    @Test
    fun `ferie etter arbeidsdag tilbakestiller arbeidsgiverperioden`() {
        undersøke(1.S + 1.A + 15.F + 16.S)
        assertEquals(33, inspektør.size)
        assertEquals(17, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(15, inspektør.fridagTeller)
        assertEquals(1, inspektør.arbeidsdagTeller)
    }

    @Test
    fun `ferie etter frisk helg tilbakestiller arbeidsgiverperioden`() {
        undersøke(6.S + 1.A + 15.F + 16.S)
        assertEquals(38, inspektør.size)
        assertEquals(22, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(16, inspektør.fridagTeller)
    }

    @Test
    fun `ferie før frisk helg tilbakestiller arbeidsgiverperioden`() {
        undersøke(5.S + 15.F + 1.A + 16.S)
        assertEquals(37, inspektør.size)
        assertEquals(21, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(16, inspektør.fridagTeller)
    }

    @Test
    fun `ferie som opphold før arbeidsgiverperioden`() {
        undersøke(15.F + 16.S)
        assertEquals(31, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(15, inspektør.fridagTeller)
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

        private var tilstand: Tilstand = Initiell

        internal fun result(): Utbetalingstidslinje {
            fridager.somFeriedager()
            return tidslinje
        }

        private fun tilstand(tilstand: Tilstand) {
            this.tilstand = tilstand
        }

        override fun arbeidsgiverperiodedag() {
            tilstand(Arbeidsgiverperiode)
        }

        override fun sykedag() {
            tilstand(Utbetaling)
        }

        fun MutableList<LocalDate>.somSykedager() {
            onEach {
                arbeidsgiverperiodeteller.inc()
                tilstand.feriedagSomSyk(this@UtbetalingstidslinjeBuilder, it)
            }.clear()
        }

        fun MutableList<LocalDate>.somFeriedager() {
            onEach {
                arbeidsgiverperiodeteller.dec()
                tidslinje.addFridag(it, Økonomi.ikkeBetalt())
            }.clear()
        }

        override fun visitDag(dag: Dag.Sykedag, dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            fridager.somSykedager()
            arbeidsgiverperiodeteller.inc()
            tilstand.sykdomsdag(this, dato, økonomi)
        }

        override fun visitDag(dag: Dag.SykHelgedag, dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            fridager.somSykedager()
            arbeidsgiverperiodeteller.inc()
            tilstand.sykdomshelg(this, dato, økonomi)
        }

        override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            tilstand.feriedag(this, dato)
        }

        override fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            tilstand(Initiell)
            fridager.somFeriedager()
            arbeidsgiverperiodeteller.dec()
            tidslinje.addArbeidsdag(dato, Økonomi.ikkeBetalt())
        }

        override fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            tilstand(Initiell)
            fridager.somFeriedager()
            arbeidsgiverperiodeteller.dec()
            tidslinje.addFridag(dato, Økonomi.ikkeBetalt())
        }

        private val fridager = mutableListOf<LocalDate>()

        private interface Tilstand {
            fun sykdomsdag(builder: UtbetalingstidslinjeBuilder, dato: LocalDate, økonomi: Økonomi) {
                builder.tidslinje.addNAVdag(dato, økonomi)
            }
            fun sykdomshelg(builder: UtbetalingstidslinjeBuilder, dato: LocalDate, økonomi: Økonomi) {
                builder.tidslinje.addHelg(dato, økonomi)
            }
            fun feriedagSomSyk(builder: UtbetalingstidslinjeBuilder, dato: LocalDate)
            fun feriedag(builder: UtbetalingstidslinjeBuilder, dato: LocalDate)
        }
        private object Initiell : Tilstand {
            override fun feriedag(builder: UtbetalingstidslinjeBuilder, dato: LocalDate) {
                builder.arbeidsgiverperiodeteller.dec()
                builder.tidslinje.addFridag(dato, Økonomi.ikkeBetalt())
            }

            override fun feriedagSomSyk(builder: UtbetalingstidslinjeBuilder, dato: LocalDate) {
                throw IllegalStateException()
            }
        }
        private object Arbeidsgiverperiode : Tilstand {
            override fun sykdomsdag(builder: UtbetalingstidslinjeBuilder, dato: LocalDate, økonomi: Økonomi) {
                builder.tidslinje.addArbeidsgiverperiodedag(dato, økonomi)
            }
            override fun sykdomshelg(builder: UtbetalingstidslinjeBuilder, dato: LocalDate, økonomi: Økonomi) {
                builder.tidslinje.addArbeidsgiverperiodedag(dato, økonomi)
            }
            override fun feriedagSomSyk(builder: UtbetalingstidslinjeBuilder, dato: LocalDate) {
                builder.tidslinje.addArbeidsgiverperiodedag(dato, Økonomi.ikkeBetalt())
            }
            override fun feriedag(builder: UtbetalingstidslinjeBuilder, dato: LocalDate) {
                builder.fridager.add(dato)
            }
        }
        private object Utbetaling : Tilstand {
            override fun feriedag(builder: UtbetalingstidslinjeBuilder, dato: LocalDate) {
                builder.fridager.add(dato)
            }

            override fun feriedagSomSyk(builder: UtbetalingstidslinjeBuilder, dato: LocalDate) {
                builder.tidslinje.addFridag(dato, Økonomi.ikkeBetalt())
            }
        }
    }
}
