package no.nav.helse

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

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
        if (this.tilstand == tilstand) return
        this.tilstand.leaving(this)
        this.tilstand = tilstand
        this.tilstand.entering(this)
    }

    override fun arbeidsgiverperiodedag() {
        tilstand(Arbeidsgiverperiode)
    }

    override fun sykedag() {
        tilstand(Utbetaling)
    }

    override fun arbeidsgiverperiodeAvbrutt() {
        mediator.arbeidsgiverperiodeAvbrutt()
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
        fun entering(builder: ArbeidsgiverperiodeBuilder) {}
        fun sykdomsdag(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate, økonomi: Økonomi) {
            builder.mediator.utbetalingsdag(dato, økonomi)
        }
        fun sykdomshelg(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate, økonomi: Økonomi) {
            builder.mediator.utbetalingsdag(dato, økonomi)
        }
        fun feriedagSomSyk(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate)
        fun feriedag(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate)
        fun leaving(builder: ArbeidsgiverperiodeBuilder) {}
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
        override fun entering(builder: ArbeidsgiverperiodeBuilder) {
            builder.mediator.arbeidsgiverperiodeFerdig()
        }

        override fun feriedag(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate) {
            builder.fridager.add(dato)
        }

        override fun feriedagSomSyk(builder: ArbeidsgiverperiodeBuilder, dato: LocalDate) {
            builder.mediator.fridag(dato)
        }
    }
}
