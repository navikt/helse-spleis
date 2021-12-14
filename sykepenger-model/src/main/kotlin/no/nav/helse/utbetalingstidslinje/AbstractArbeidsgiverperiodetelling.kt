package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal fun interface IArbeidsgiverperiodetelling {
    fun build(sykdomstidslinje: Sykdomstidslinje, periode: Periode)
}

internal abstract class AbstractArbeidsgiverperiodetelling(
    protected val regler: ArbeidsgiverRegler = ArbeidsgiverRegler.Companion.NormalArbeidstaker
) : SykdomstidslinjeVisitor, Arbeidsgiverperiodeteller.Observatør, IArbeidsgiverperiodetelling {
    private lateinit var teller: Arbeidsgiverperiodeteller

    init {
        teller(Forlengelsestrategi.Ingen)
    }

    internal fun forlengelsestrategi(strategi: Forlengelsestrategi) {
        teller(strategi)
    }

    override fun build(sykdomstidslinje: Sykdomstidslinje, periode: Periode) {
        sykdomstidslinje.fremTilOgMed(periode.endInclusive).accept(this)
        teller.avslutt()
    }

    private fun teller(strategi: Forlengelsestrategi) {
        this.teller = Arbeidsgiverperiodeteller(regler, strategi).also {
            it.observatør(this)
        }
    }

    protected abstract fun sykedagIArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode, dato: LocalDate)
    protected abstract fun sykedagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate, økonomi: Økonomi)
    protected abstract fun sykHelgedagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate, økonomi: Økonomi)
    protected abstract fun foreldetSykedagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate, økonomi: Økonomi)
    protected abstract fun egenmeldingsdagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate)
    protected open fun fridagIArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode, dato: LocalDate) =
        sykedagIArbeidsgiverperioden(arbeidsgiverperiode, dato)
    protected abstract fun fridagUtenforArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate)
    protected abstract fun arbeidsdag(dato: LocalDate)

    private fun sykHelgedag(dato: LocalDate, økonomi: Økonomi) {
        teller.inkrementer(dato,
            Arbeidsgiverperiodestrategi.Default({ sykedagIArbeidsgiverperioden(it, dato) }, { sykHelgedagEtterArbeidsgiverperioden(it, dato, økonomi) })
        )
    }

    private fun fridag(dato: LocalDate) {
        teller.inkrementEllerDekrement(dato,
            Arbeidsgiverperiodestrategi.Default({ fridagIArbeidsgiverperioden(it, dato) }, { fridagUtenforArbeidsgiverperioden(it, dato) })
        )
    }

    final override fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        teller.dekrementer(dato)
        arbeidsdag(dato)
    }

    final override fun visitDag(
        dag: Dag.Arbeidsgiverdag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        teller.inkrementer(dato,
            Arbeidsgiverperiodestrategi.Default({ sykedagIArbeidsgiverperioden(it, dato) }, { egenmeldingsdagEtterArbeidsgiverperioden(it, dato) })
        )
    }

    final override fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        teller.dekrementer(dato)
        arbeidsdag(dato)
    }

    final override fun visitDag(dag: Dag.UkjentDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        if (dato.erHelg()) return fridag(dato)
        teller.dekrementer(dato)
        arbeidsdag(dato)
    }

    final override fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        fridag(dato)
    }

    final override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        fridag(dato)
    }

    final override fun visitDag(dag: Dag.AvslåttDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        fridag(dato)
    }

    final override fun visitDag(
        dag: Dag.Sykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        teller.inkrementer(dato,
            Arbeidsgiverperiodestrategi.Default({ sykedagIArbeidsgiverperioden(it, dato) }, { sykedagEtterArbeidsgiverperioden(it, dato, økonomi) })
        )
    }

    final override fun visitDag(
        dag: Dag.ArbeidsgiverHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        sykHelgedag(dato, økonomi)
    }

    final override fun visitDag(
        dag: Dag.SykHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        sykHelgedag(dato, økonomi)
    }

    final override fun visitDag(
        dag: Dag.ForeldetSykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        teller.inkrementer(dato,
            Arbeidsgiverperiodestrategi.Default({ sykedagIArbeidsgiverperioden(it, dato) }, { foreldetSykedagEtterArbeidsgiverperioden(it, dato, økonomi) })
        )
    }

    final override fun visitDag(
        dag: Dag.ProblemDag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde,
        melding: String
    ) = throw UtbetalingstidslinjeBuilderException.UforventetDagException(dag, melding)
}
