package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde
import no.nav.helse.økonomi.Økonomi

internal class Arbeidsgiverperiodeberegner(
    private val arbeidsgiverperiodeteller: Arbeidsgiverperiodeteller,
    oppsamler: Arbeidsgiverperiodeoppsamler,
    subsumsjonslogg: Subsumsjonslogg?
) : SykdomstidslinjeVisitor,
    Arbeidsgiverperiodeteller.Observatør {

    private val oppsamler = Arbeidsgiverperiodesubsumsjon(oppsamler, subsumsjonslogg)

    init {
        arbeidsgiverperiodeteller.observer(this)
    }

    private var tilstand: Tilstand = Initiell

    override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: List<Periode>) {
        oppsamler.tidslinje(tidslinje)
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

    override fun oppholdsdag() {
        oppsamler.oppholdsdag()
    }

    override fun arbeidsgiverperiodeAvbrutt() {
        oppsamler.arbeidsgiverperiodeAvbrutt()
    }

    private fun MutableList<LocalDate>.somSykedager(kilde: Hendelseskilde) {
        onEach {
            arbeidsgiverperiodeteller.inc()
            tilstand.feriedagSomSyk(this@Arbeidsgiverperiodeberegner, it, kilde)
        }.clear()
    }

    private fun MutableList<LocalDate>.somFerieOppholdsdager() {
        onEach {
            arbeidsgiverperiodeteller.dec()
            oppsamler.fridagOppholdsdag(it)
        }.clear()
    }

    override fun visitDag(dag: Dag.Sykedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        fridager.somSykedager(kilde)
        arbeidsgiverperiodeteller.inc()
        tilstand.sykdomsdag(this, dato, økonomi, kilde)
    }

    override fun visitDag(dag: Dag.SykedagNav, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        fridager.somSykedager(kilde)
        arbeidsgiverperiodeteller.inc()
        tilstand.sykdomsdagNav(this, dato, økonomi, kilde)
    }

    override fun visitDag(dag: Dag.SykHelgedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        fridager.somSykedager(kilde)
        arbeidsgiverperiodeteller.inc()
        tilstand.sykdomshelg(this, dato, økonomi, kilde)
    }

    override fun visitDag(dag: Dag.Arbeidsgiverdag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        fridager.somSykedager(kilde)
        arbeidsgiverperiodeteller.inc()
        tilstand.egenmeldingsdag(this, dato, økonomi, kilde)
    }

    override fun visitDag(dag: Dag.ArbeidsgiverHelgedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        fridager.somSykedager(kilde)
        arbeidsgiverperiodeteller.inc()
        tilstand.sykdomsdag(this, dato, økonomi, kilde)
    }

    override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: Hendelseskilde) {
        fridager.somSykedager(kilde)
        tilstand.feriedagMedSykmelding(this, dato, kilde)
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
        val begrunnelse = when(ytelse) {
            Dag.AndreYtelser.AnnenYtelse.AAP -> Begrunnelse.AndreYtelserAap
            Dag.AndreYtelser.AnnenYtelse.Dagpenger -> Begrunnelse.AndreYtelserDagpenger
            Dag.AndreYtelser.AnnenYtelse.Foreldrepenger -> Begrunnelse.AndreYtelserForeldrepenger
            Dag.AndreYtelser.AnnenYtelse.Omsorgspenger -> Begrunnelse.AndreYtelserOmsorgspenger
            Dag.AndreYtelser.AnnenYtelse.Opplæringspenger -> Begrunnelse.AndreYtelserOpplaringspenger
            Dag.AndreYtelser.AnnenYtelse.Pleiepenger -> Begrunnelse.AndreYtelserPleiepenger
            Dag.AndreYtelser.AnnenYtelse.Svangerskapspenger -> Begrunnelse.AndreYtelserSvangerskapspenger

        }
        tilstand.andreYtelser(this, dato, begrunnelse)
    }

    override fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) {
        tilstand(Initiell)
        fridager.somFerieOppholdsdager()
        arbeidsgiverperiodeteller.dec()
        oppsamler.arbeidsdag(dato)
    }

    override fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) {
        tilstand(Initiell)
        fridager.somFerieOppholdsdager()
        arbeidsgiverperiodeteller.dec()
        oppsamler.arbeidsdag(dato)
    }

    override fun visitDag(dag: Dag.UkjentDag, dato: LocalDate, kilde: Hendelseskilde) {
        if (dato.erHelg()) return tilstand.feriedag(this, dato)
        tilstand(Initiell)
        fridager.somFerieOppholdsdager()
        arbeidsgiverperiodeteller.dec()
        oppsamler.arbeidsdag(dato)
    }

    override fun visitDag(dag: Dag.ForeldetSykedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        fridager.somSykedager(kilde)
        arbeidsgiverperiodeteller.inc()
        tilstand.foreldetDag(this, dato, økonomi, kilde)
    }

    private val fridager = mutableListOf<LocalDate>()

    private interface Tilstand {
        fun entering(builder: Arbeidsgiverperiodeberegner) {}
        fun sykdomsdag(
            builder: Arbeidsgiverperiodeberegner,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) {
            builder.oppsamler.utbetalingsdag(dato)
        }
        fun sykdomsdagNav(
            builder: Arbeidsgiverperiodeberegner,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) {
            builder.oppsamler.utbetalingsdag(dato)
        }
        fun egenmeldingsdag(
            builder: Arbeidsgiverperiodeberegner,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = sykdomsdag(builder, dato, økonomi, kilde)
        fun sykdomshelg(
            builder: Arbeidsgiverperiodeberegner,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) {
            builder.oppsamler.utbetalingsdag(dato)
        }
        fun foreldetDag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
            builder.oppsamler.foreldetDag(dato)
        }
        fun feriedagSomSyk(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, kilde: Hendelseskilde)
        fun feriedag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate)
        fun feriedagMedSykmelding(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, kilde: Hendelseskilde)
        fun leaving(builder: Arbeidsgiverperiodeberegner) {}
        fun andreYtelser(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, begrunnelse: Begrunnelse)
    }
    private object Initiell : Tilstand {
        override fun feriedag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.arbeidsgiverperiodeteller.dec()
            builder.oppsamler.fridagOppholdsdag(dato)
        }

        override fun feriedagMedSykmelding(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, kilde: Hendelseskilde) {
            feriedag(builder, dato)
        }

        override fun andreYtelser(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, begrunnelse: Begrunnelse) {
            feriedag(builder, dato)
        }

        override fun feriedagSomSyk(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, kilde: Hendelseskilde) {
            throw IllegalStateException()
        }
    }
    private object Arbeidsgiverperiode : Tilstand {
        override fun sykdomsdagNav(
            builder: Arbeidsgiverperiodeberegner,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) {
            builder.oppsamler.arbeidsgiverperiodedagNav(dato)
        }

        override fun sykdomsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
            builder.oppsamler.arbeidsgiverperiodedag(dato)
        }
        override fun sykdomshelg(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
            builder.oppsamler.arbeidsgiverperiodedag(dato)
        }
        override fun feriedagSomSyk(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, kilde: Hendelseskilde) {
            builder.oppsamler.arbeidsgiverperiodedag(dato)
        }

        override fun feriedagMedSykmelding(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, kilde: Hendelseskilde) {
            builder.arbeidsgiverperiodeteller.inc()
            builder.tilstand.feriedagSomSyk(builder, dato, kilde)
        }

        override fun feriedag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.fridager.add(dato)
        }
        override fun andreYtelser(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, begrunnelse: Begrunnelse) {
            builder.fridager.add(dato)
        }

        override fun foreldetDag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
            builder.oppsamler.arbeidsgiverperiodedag(dato)
        }
    }
    private object ArbeidsgiverperiodeSisteDag : Tilstand {
        override fun entering(builder: Arbeidsgiverperiodeberegner) {
            builder.oppsamler.arbeidsgiverperiodeSistedag()
        }

        override fun sykdomsdagNav(
            builder: Arbeidsgiverperiodeberegner,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) {
            builder.oppsamler.arbeidsgiverperiodedagNav(dato)
            builder.tilstand(Utbetaling)
        }

        override fun sykdomsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
            builder.oppsamler.arbeidsgiverperiodedag(dato)
            builder.tilstand(Utbetaling)
        }
        override fun sykdomshelg(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
            builder.oppsamler.arbeidsgiverperiodedag(dato)
            builder.tilstand(Utbetaling)
        }
        override fun feriedagSomSyk(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, kilde: Hendelseskilde) {
            builder.oppsamler.arbeidsgiverperiodedag(dato)
            builder.tilstand(Utbetaling)
        }

        override fun foreldetDag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
            builder.oppsamler.arbeidsgiverperiodedag(dato)
            builder.tilstand(Utbetaling)
        }

        override fun feriedagMedSykmelding(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, kilde: Hendelseskilde) {
            throw IllegalStateException("kan ikke ha fridag som siste dag i arbeidsgiverperioden")
        }

        override fun feriedag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            throw IllegalStateException("kan ikke ha fridag som siste dag i arbeidsgiverperioden")
        }

        override fun andreYtelser(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, begrunnelse: Begrunnelse) {
            throw IllegalStateException("kan ikke ha andre ytelser som siste dag i arbeidsgiverperioden")
        }
    }
    private object Utbetaling : Tilstand {
        override fun entering(builder: Arbeidsgiverperiodeberegner) {
            // signaliserer at arbeidsgiverperioden er ferdig;
            // enten som følge av at vi nettopp har fullført å telle arbeidsgiverperioden,
            // eller fordi vi skal gå rett på utbetaling (arbeidsgiverperioden er unnagjort i Infotrygd, eller antall arbeidsgiverperiodedager skal være 0)
            builder.oppsamler.arbeidsgiverperiodeFerdig()
        }

        override fun egenmeldingsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
            builder.oppsamler.avvistDag(dato, Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode)
        }

        override fun feriedag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.oppsamler.fridag(dato)
        }

        override fun feriedagMedSykmelding(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, kilde: Hendelseskilde) {
            builder.oppsamler.fridag(dato)
        }

        override fun andreYtelser(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, begrunnelse: Begrunnelse) {
            builder.oppsamler.avvistDag(dato, begrunnelse)
        }

        override fun feriedagSomSyk(builder: Arbeidsgiverperiodeberegner, dato: LocalDate, kilde: Hendelseskilde) {
            builder.oppsamler.fridag(dato)
        }
    }
}
