package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde
import no.nav.helse.økonomi.Økonomi

internal class Arbeidsgiverperiodeberegner(
    private val arbeidsgiverperiodeteller: Arbeidsgiverperiodeteller,
) : SykdomstidslinjeVisitor,
    Arbeidsgiverperiodeteller.Observatør {

    private val arbeidsgiverperioder = mutableListOf<Arbeidsgiverperioderesultat>()
    private var aktivArbeidsgiverperioderesultat: Arbeidsgiverperioderesultat? = null

    init {
        arbeidsgiverperiodeteller.observer(this)
    }

    private var tilstand: Tilstand = Initiell

    private fun arbeidsgiverperiodeResultatet(dato: LocalDate): Arbeidsgiverperioderesultat {
        return aktivArbeidsgiverperioderesultat ?: Arbeidsgiverperioderesultat(
            omsluttendePeriode = dato.somPeriode(),
            arbeidsgiverperiode = emptyList(),
            utbetalingsperioder = emptyList(),
            oppholdsperioder = emptyList(),
            fullstendig = false,
            sisteDag = null
        ).also { aktivArbeidsgiverperioderesultat = it }
    }

    internal fun resultat(): List<Arbeidsgiverperioderesultat> {
        return aktivArbeidsgiverperioderesultat
            ?.let { arbeidsgiverperioder.toList() + it }
            ?: arbeidsgiverperioder.toList()
    }

    override fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: MutableList<Periode>) {
        fridager.somFerieOppholdsdager()
    }

    private fun tilstand(tilstand: Tilstand) {
        if (this.tilstand == tilstand) return
        this.tilstand.leaving(this)
        this.tilstand = tilstand
        this.tilstand.entering(this)
    }

    override fun arbeidsgiverperiodeFerdig() {
        tilstand(ArbeidsgiverperiodeSisteDag)
    }

    override fun arbeidsgiverperiodedag() {
        tilstand(Arbeidsgiverperiode)
    }

    override fun sykedag() {
        tilstand(Utbetaling)
    }

    override fun arbeidsgiverperiodeAvbrutt() {
        tilstand(ArbeidsgiverperiodeAvbrutt)
    }

    private fun MutableList<LocalDate>.somSykedager(kilde: Hendelseskilde) {
        onEach {
            arbeidsgiverperiodeteller.inc()
            tilstand.feriedagSomSyk(this@Arbeidsgiverperiodeberegner, it)
        }.clear()
    }

    private fun MutableList<LocalDate>.somFerieOppholdsdager() {
        onEach {
            arbeidsgiverperiodeteller.dec()
            tilstand.oppholdsdag(this@Arbeidsgiverperiodeberegner, it)
        }.clear()
    }

    override fun visitDag(dag: Dag.Sykedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        fridager.somSykedager(kilde)
        arbeidsgiverperiodeteller.inc()
        tilstand.sykdomsdag(this, dato)
    }

    override fun visitDag(dag: Dag.SykedagNav, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        fridager.somSykedager(kilde)
        arbeidsgiverperiodeteller.inc()
        tilstand.sykdomsdagNav(this, dato)
    }

    override fun visitDag(dag: Dag.SykHelgedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        fridager.somSykedager(kilde)
        arbeidsgiverperiodeteller.inc()
        tilstand.sykdomsdag(this, dato)
    }

    override fun visitDag(dag: Dag.Arbeidsgiverdag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        fridager.somSykedager(kilde)
        arbeidsgiverperiodeteller.inc()
        tilstand.egenmeldingsdag(this, dato)
    }

    override fun visitDag(dag: Dag.ArbeidsgiverHelgedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        fridager.somSykedager(kilde)
        arbeidsgiverperiodeteller.inc()
        tilstand.sykdomsdag(this, dato)
    }

    override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: Hendelseskilde) {
        fridager.somSykedager(kilde)
        tilstand.feriedagMedSykmelding(this, dato)
    }

    override fun visitDag(dag: Dag.ArbeidIkkeGjenopptattDag, dato: LocalDate, kilde: Hendelseskilde) {
        tilstand.feriedag(this, dato)
    }

    override fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: Hendelseskilde) {
        tilstand.feriedag(this, dato)
    }

    override fun visitDag(
        dag: Dag.ProblemDag,
        dato: LocalDate,
        kilde: Hendelseskilde,
        other: Hendelseskilde?,
        melding: String
    ) {
        throw UtbetalingstidslinjeBuilderException.ProblemdagException(melding)
    }

    override fun visitDag(
        dag: Dag.AndreYtelser,
        dato: LocalDate,
        kilde: Hendelseskilde,
        ytelse: Dag.AndreYtelser.AnnenYtelse
    ) {
        tilstand.andreYtelser(this, dato)
    }

    override fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) {
        fridager.somFerieOppholdsdager()
        arbeidsgiverperiodeteller.dec()
        tilstand.oppholdsdag(this, dato)
        tilstand(Initiell)
    }

    override fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) {
        fridager.somFerieOppholdsdager()
        arbeidsgiverperiodeteller.dec()
        tilstand.oppholdsdag(this, dato)
        tilstand(Initiell)
    }

    override fun visitDag(dag: Dag.UkjentDag, dato: LocalDate, kilde: Hendelseskilde) {
        if (dato.erHelg()) return tilstand.feriedag(this, dato)
        fridager.somFerieOppholdsdager()
        arbeidsgiverperiodeteller.dec()
        tilstand.oppholdsdag(this, dato)
        tilstand(Initiell)
    }

    override fun visitDag(dag: Dag.ForeldetSykedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        fridager.somSykedager(kilde)
        arbeidsgiverperiodeteller.inc()
        tilstand.foreldetDag(this, dato)
    }

    private val fridager = mutableListOf<LocalDate>()

    private interface Tilstand {
        fun entering(builder: Arbeidsgiverperiodeberegner) {}
        fun oppholdsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.aktivArbeidsgiverperioderesultat = builder.arbeidsgiverperiodeResultatet(dato).utvideMed(
                dato = dato,
                oppholdsperiode = dato
            )
        }
        fun sykdomsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.aktivArbeidsgiverperioderesultat = builder.arbeidsgiverperiodeResultatet(dato).utvideMed(
                dato = dato,
                utbetalingsperiode = dato
            )
        }
        fun sykdomsdagNav(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = sykdomsdag(builder, dato)
        fun egenmeldingsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = sykdomsdag(builder, dato)
        fun foreldetDag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = sykdomsdag(builder, dato)

        fun feriedagSomSyk(builder: Arbeidsgiverperiodeberegner, dato: LocalDate)
        fun feriedag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate)
        fun feriedagMedSykmelding(builder: Arbeidsgiverperiodeberegner, dato: LocalDate)
        fun andreYtelser(builder: Arbeidsgiverperiodeberegner, dato: LocalDate)
        fun leaving(builder: Arbeidsgiverperiodeberegner) {}
    }
    private object Initiell : Tilstand {
        override fun oppholdsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.aktivArbeidsgiverperioderesultat?.utvideMed(
                dato = dato,
                oppholdsperiode = dato
            )?.also {
                builder.aktivArbeidsgiverperioderesultat = it
            }
        }

        override fun feriedag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.arbeidsgiverperiodeteller.dec()
            builder.tilstand.oppholdsdag(builder, dato)
        }

        override fun feriedagMedSykmelding(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = feriedag(builder, dato)
        override fun andreYtelser(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = feriedag(builder, dato)

        override fun feriedagSomSyk(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            throw IllegalStateException()
        }
    }
    private object Arbeidsgiverperiode : Tilstand {
        override fun sykdomsdagNav(
            builder: Arbeidsgiverperiodeberegner,
            dato: LocalDate
        ) {
            builder.aktivArbeidsgiverperioderesultat = builder.arbeidsgiverperiodeResultatet(dato).utvideMed(
                dato = dato,
                arbeidsgiverperiode = dato,
                utbetalingsperiode = dato
            )
        }

        override fun sykdomsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.aktivArbeidsgiverperioderesultat = builder.arbeidsgiverperiodeResultatet(dato).utvideMed(
                dato = dato,
                arbeidsgiverperiode = dato
            )
        }
        override fun feriedagSomSyk(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = sykdomsdag(builder, dato)
        override fun foreldetDag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = sykdomsdag(builder, dato)

        override fun feriedagMedSykmelding(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.arbeidsgiverperiodeteller.inc()
            builder.tilstand.feriedagSomSyk(builder, dato)
        }
        override fun feriedag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.fridager.add(dato)
        }

        override fun andreYtelser(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = feriedag(builder, dato)
    }
    private object ArbeidsgiverperiodeSisteDag : Tilstand {
        override fun sykdomsdagNav(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.aktivArbeidsgiverperioderesultat = builder.arbeidsgiverperiodeResultatet(dato).utvideMed(
                dato = dato,
                arbeidsgiverperiode = dato,
                utbetalingsperiode = dato,
                fullstendig = true
            )
            builder.tilstand(Utbetaling)
        }

        override fun sykdomsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.aktivArbeidsgiverperioderesultat = builder.arbeidsgiverperiodeResultatet(dato).utvideMed(
                dato = dato,
                arbeidsgiverperiode = dato,
                fullstendig = true
            )
            builder.tilstand(Utbetaling)
        }
        override fun feriedagSomSyk(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = sykdomsdag(builder, dato)
        override fun foreldetDag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = sykdomsdag(builder, dato)

        override fun feriedagMedSykmelding(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            throw IllegalStateException("kan ikke ha fridag som siste dag i arbeidsgiverperioden")
        }

        override fun feriedag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            throw IllegalStateException("kan ikke ha fridag som siste dag i arbeidsgiverperioden")
        }

        override fun andreYtelser(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            throw IllegalStateException("kan ikke ha andre ytelser som siste dag i arbeidsgiverperioden")
        }
    }
    private object Utbetaling : Tilstand {
        private fun kjentDag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.aktivArbeidsgiverperioderesultat = builder.arbeidsgiverperiodeResultatet(dato).utvideMed(
                dato = dato
            )
        }
        override fun egenmeldingsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = kjentDag(builder, dato)
        override fun feriedag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = kjentDag(builder, dato)
        override fun feriedagMedSykmelding(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = kjentDag(builder, dato)
        override fun andreYtelser(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = kjentDag(builder, dato)
        override fun feriedagSomSyk(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = kjentDag(builder, dato)
    }

    private object ArbeidsgiverperiodeAvbrutt : Tilstand {
        override fun oppholdsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.aktivArbeidsgiverperioderesultat = builder.arbeidsgiverperiodeResultatet(dato).utvideMed(
                dato = dato,
                oppholdsperiode = dato,
                sisteDag = dato
            )
            builder.tilstand(Initiell)
        }

        override fun feriedag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = oppholdsdag(builder, dato)
        override fun feriedagMedSykmelding(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = oppholdsdag(builder, dato)
        override fun andreYtelser(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = oppholdsdag(builder, dato)

        override fun sykdomsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = throw IllegalStateException()
        override fun sykdomsdagNav(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = throw IllegalStateException()
        override fun egenmeldingsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = throw IllegalStateException()
        override fun foreldetDag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = throw IllegalStateException()
        override fun feriedagSomSyk(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = throw IllegalStateException()

        override fun leaving(builder: Arbeidsgiverperiodeberegner) {
            builder.aktivArbeidsgiverperioderesultat?.let {
                builder.arbeidsgiverperioder.add(it)
            }
            builder.aktivArbeidsgiverperioderesultat = null
        }
    }
}
