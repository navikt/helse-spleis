package no.nav.helse

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.UtbetalingstidslinjeInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.testhelpers.*
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
    fun `arbeidsgiverperioden er ferdig tidligere`() {
        teller.fullfør()
        undersøke(15.S)
        assertEquals(15, inspektør.size)
        assertEquals(11, inspektør.navDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
    }

    @Test
    fun `arbeidsgiverperioden oppdages av noen andre`() {
        val betalteDager = listOf(10.januar til 16.januar)
        undersøke(15.S) { teller, other ->
            Infotrygd(teller, other, betalteDager)
        }
        assertEquals(15, inspektør.size)
        assertEquals(9, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(4, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
    }

    private class Infotrygd(
        private val teller: Arbeidsgiverperiodeteller,
        private val other: SykdomstidslinjeVisitor,
        private val betalteDager: List<Periode>
    ) : SykdomstidslinjeVisitor by(other) {
        override fun visitDag(dag: Dag.Sykedag, dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            fullførArbeidsgiverperiode(dato)
            other.visitDag(dag, dato, økonomi, kilde)
        }
        override fun visitDag(dag: Dag.SykHelgedag, dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            fullførArbeidsgiverperiode(dato)
            other.visitDag(dag, dato, økonomi, kilde)
        }

        private fun fullførArbeidsgiverperiode(dato: LocalDate) {
            if (betalteDager.any { dato in it }) teller.fullfør()
        }
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

    private lateinit var teller: Arbeidsgiverperiodeteller
    @BeforeEach
    fun setup() {
        resetSeed()
        teller = Arbeidsgiverperiodeteller.NormalArbeidstaker
    }

    private lateinit var inspektør: UtbetalingstidslinjeInspektør
    private fun undersøke(tidslinje: Sykdomstidslinje, delegator: ((Arbeidsgiverperiodeteller, SykdomstidslinjeVisitor) -> SykdomstidslinjeVisitor)? = null) {
        val builder = UtbetalingstidslinjeBuilder()
        val arbeidsgiverperiodeBuilder = ArbeidsgiverperiodeBuilder(teller, builder)
        tidslinje.accept(delegator?.invoke(teller, arbeidsgiverperiodeBuilder) ?: arbeidsgiverperiodeBuilder)
        inspektør = builder.result().inspektør
    }

    internal class UtbetalingstidslinjeBuilder() : ArbeidsgiverperiodeMediator {
        private val tidslinje = Utbetalingstidslinje()

        internal fun result(): Utbetalingstidslinje {
            return tidslinje
        }

        override fun fridag(dato: LocalDate) {
            tidslinje.addFridag(dato, Økonomi.ikkeBetalt())
        }

        override fun arbeidsdag(dato: LocalDate) {
            tidslinje.addArbeidsdag(dato, Økonomi.ikkeBetalt())
        }

        override fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi) {
            tidslinje.addArbeidsgiverperiodedag(dato, økonomi)
        }

        override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi) {
            if (dato.erHelg()) tidslinje.addHelg(dato, økonomi)
            else tidslinje.addNAVdag(dato, økonomi)
        }
    }
    internal interface ArbeidsgiverperiodeMediator {
        fun fridag(dato: LocalDate)
        fun arbeidsdag(dato: LocalDate)
        fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi)
        fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi)
    }
    internal class ArbeidsgiverperiodeBuilder(private val arbeidsgiverperiodeteller: Arbeidsgiverperiodeteller, private val mediator: ArbeidsgiverperiodeMediator) : SykdomstidslinjeVisitor,
        Arbeidsgiverperiodeteller.Observatør {

        init {
            arbeidsgiverperiodeteller.observer(this)
        }

        private var tilstand: Tilstand = Initiell

        override fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: MutableList<Periode>) {
            fridager.somFeriedager()
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

        private fun MutableList<LocalDate>.somSykedager() {
            onEach {
                arbeidsgiverperiodeteller.inc()
                tilstand.feriedagSomSyk(this@ArbeidsgiverperiodeBuilder, it)
            }.clear()
        }

        private fun MutableList<LocalDate>.somFeriedager() {
            onEach {
                arbeidsgiverperiodeteller.dec()
                mediator.fridag(it)
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
            mediator.arbeidsdag(dato)
        }

        override fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            tilstand(Initiell)
            fridager.somFeriedager()
            arbeidsgiverperiodeteller.dec()
            mediator.fridag(dato)
        }

        private val fridager = mutableListOf<LocalDate>()

        private interface Tilstand {
            fun sykdomsdag(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate, økonomi: Økonomi) {
                builder.mediator.utbetalingsdag(dato, økonomi)
            }
            fun sykdomshelg(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate, økonomi: Økonomi) {
                builder.mediator.utbetalingsdag(dato, økonomi)
            }
            fun feriedagSomSyk(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate)
            fun feriedag(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate)
        }
        private object Initiell : Tilstand {
            override fun feriedag(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate) {
                builder.arbeidsgiverperiodeteller.dec()
                builder.mediator.fridag(dato)
            }

            override fun feriedagSomSyk(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate) {
                throw IllegalStateException()
            }
        }
        private object Arbeidsgiverperiode : Tilstand {
            override fun sykdomsdag(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate, økonomi: Økonomi) {
                builder.mediator.arbeidsgiverperiodedag(dato, økonomi)
            }
            override fun sykdomshelg(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate, økonomi: Økonomi) {
                builder.mediator.arbeidsgiverperiodedag(dato, økonomi)
            }
            override fun feriedagSomSyk(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate) {
                builder.mediator.arbeidsgiverperiodedag(dato, Økonomi.ikkeBetalt())
            }
            override fun feriedag(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate) {
                builder.fridager.add(dato)
            }
        }
        private object Utbetaling : Tilstand {
            override fun feriedag(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate) {
                builder.fridager.add(dato)
            }

            override fun feriedagSomSyk(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate) {
                builder.mediator.fridag(dato)
            }
        }
    }
}
