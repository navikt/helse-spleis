package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

/**
 *  Forstår opprettelsen av en Utbetalingstidslinje
 */

internal class UtbetalingstidslinjeBuilder internal constructor(
    private val sammenhengendePeriode: Periode,
    private val inntektshistorikk: Inntektshistorikk,
    private val forlengelseStrategy: (Sykdomstidslinje) -> Boolean = { false },
    private val arbeidsgiverRegler: ArbeidsgiverRegler = NormalArbeidstaker
) : SykdomstidslinjeVisitor {
    private var tilstand: UtbetalingState = Initiell

    private var sykedagerIArbeidsgiverperiode = 0
    private var ikkeSykedager = 0
    private var fridager = 0

    private var dekningsgrunnlag = INGEN
    private var aktuellDagsinntekt = INGEN

    private val tidslinje = Utbetalingstidslinje()

    internal fun result(sykdomstidslinje: Sykdomstidslinje): Utbetalingstidslinje {
        if (forlengelseStrategy(sykdomstidslinje)) sykedagerIArbeidsgiverperiode += 16
        Sykdomstidslinje(sykdomstidslinje, sammenhengendePeriode).accept(this)
        return tidslinje
    }

    override fun visitDag(dag: Dag.UkjentDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = implisittDag(dato)
    override fun visitDag(dag: Dag.Studiedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = implisittDag(dato)
    override fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = fridag(dato)
    override fun visitDag(dag: Dag.Utenlandsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = implisittDag(dato)
    override fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = arbeidsdag(dato)
    override fun visitDag(
        dag: Dag.Arbeidsgiverdag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = egenmeldingsdag(dato)
    override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = fridag(dato)
    override fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = friskHelgedag(dato)
    override fun visitDag(
        dag: Dag.ArbeidsgiverHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = sykHelgedag(dato, økonomi)
    override fun visitDag(
        dag: Dag.Sykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = sykedag(dato, økonomi)
    override fun visitDag(
        dag: Dag.ForeldetSykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = foreldetSykedag(dato, økonomi)
    override fun visitDag(
        dag: Dag.SykHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = sykHelgedag(dato, økonomi)
    override fun visitDag(dag: Dag.ProblemDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde, melding: String) = throw IllegalArgumentException("Forventet ikke problemdag i utbetalingstidslinjen. Melding: $melding")

    private fun foreldetSykedag(dagen: LocalDate, økonomi: Økonomi) {
        if (arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(sykedagerIArbeidsgiverperiode)) {
            tilstand = UtbetalingSykedager
            tidslinje.addForeldetDag(dagen, økonomi.inntekt(aktuellDagsinntekt, dekningsgrunnlag))
        }
        else tilstand.sykedagerIArbeidsgiverperioden(this, dagen, økonomi)
    }

    private fun egenmeldingsdag(dato: LocalDate) =
        if (arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(sykedagerIArbeidsgiverperiode))
            tidslinje.addAvvistDag(
                dato,
                Økonomi.ikkeBetalt().inntekt(aktuellDagsinntekt, dekningsgrunnlag),
                Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode
            )
        else tilstand.egenmeldingsdagIArbeidsgiverperioden(this, dato)

    private fun implisittDag(dagen: LocalDate) = if (dagen.erHelg()) fridag(dagen) else arbeidsdag(dagen)

    private fun sykedag(dagen: LocalDate, økonomi: Økonomi) =
        if (arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(sykedagerIArbeidsgiverperiode))
            tilstand.sykedagerEtterArbeidsgiverperioden(this, dagen, økonomi)
        else
            tilstand.sykedagerIArbeidsgiverperioden(this, dagen, økonomi)

    private fun sykHelgedag(dagen: LocalDate, økonomi: Økonomi) =
        if (arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(sykedagerIArbeidsgiverperiode))
            tilstand.sykHelgedagEtterArbeidsgiverperioden(this, dagen, økonomi)
        else
            tilstand.sykHelgedagIArbeidsgiverperioden(this, dagen, økonomi)

    private fun arbeidsdag(dagen: LocalDate) =
        if (arbeidsgiverRegler.burdeStarteNyArbeidsgiverperiode(ikkeSykedager))
            tilstand.arbeidsdagerEtterOppholdsdager(this, dagen)
        else
            tilstand.arbeidsdagerIOppholdsdager(this, dagen)

    private fun fridag(dagen: LocalDate) {
        tilstand.fridag(this, dagen)
    }

    private fun friskHelgedag(dagen: LocalDate) {
        oppdatereInntekt(dagen)
        tilstand.fridag(this, dagen)
    }

    private fun oppdatereInntekt(dato: LocalDate) {
        dekningsgrunnlag = inntektshistorikk.dekningsgrunnlag(dato, arbeidsgiverRegler)
        aktuellDagsinntekt = inntektshistorikk.inntekt(dato) ?: INGEN
    }

    private fun addArbeidsgiverdag(dato: LocalDate) {
        tidslinje.addArbeidsgiverperiodedag(dato, Økonomi.ikkeBetalt().inntekt(aktuellDagsinntekt, dekningsgrunnlag))
    }

    private fun håndterArbeidsgiverdag(dagen: LocalDate) {
        sykedagerIArbeidsgiverperiode += 1
        addArbeidsgiverdag(dagen)
    }

    private fun håndterNAVdag(dato: LocalDate, økonomi: Økonomi) {
        tidslinje.addNAVdag(dato, økonomi.inntekt(aktuellDagsinntekt, dekningsgrunnlag))
    }

    private fun håndterNAVHelgedag(dato: LocalDate, økonomi: Økonomi) {
        tidslinje.addHelg(dato, økonomi.inntekt(INGEN))
    }

    private fun håndterArbeidsdag(dato: LocalDate) {
        inkrementerIkkeSykedager()
        oppdatereInntekt(dato)
        tidslinje.addArbeidsdag(dato, Økonomi.ikkeBetalt().inntekt(aktuellDagsinntekt, dekningsgrunnlag))
    }

    private fun inkrementerIkkeSykedager() {
        ikkeSykedager += 1
        if (arbeidsgiverRegler.burdeStarteNyArbeidsgiverperiode(ikkeSykedager)) state(Initiell)
    }

    private fun håndterFridag(dato: LocalDate) {
        fridager += 1
        tidslinje.addFridag(dato, Økonomi.ikkeBetalt().inntekt(aktuellDagsinntekt, dekningsgrunnlag))
    }

    private fun håndterFriEgenmeldingsdag(dato: LocalDate) {
        sykedagerIArbeidsgiverperiode += fridager
        tidslinje.addAvvistDag(
            dato,
            Økonomi.ikkeBetalt().inntekt(aktuellDagsinntekt, dekningsgrunnlag),
            Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode
        )
        if (arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(sykedagerIArbeidsgiverperiode))
            state(UtbetalingSykedager)
        else
            state(ArbeidsgiverperiodeSykedager)
    }

    private fun state(state: UtbetalingState) {
        this.tilstand.leaving(this)
        this.tilstand = state
        this.tilstand.entering(this)
    }

    internal interface UtbetalingState {
        fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        )

        fun egenmeldingsdagIArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            sykedagerIArbeidsgiverperioden(splitter, dagen, Økonomi.ikkeBetalt())
        }

        fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        )

        fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate)
        fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate)
        fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate)
        fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        )
        fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        )

        fun entering(splitter: UtbetalingstidslinjeBuilder) {}
        fun leaving(splitter: UtbetalingstidslinjeBuilder) {}
    }

    private object Initiell : UtbetalingState {

        override fun entering(splitter: UtbetalingstidslinjeBuilder) {
            splitter.sykedagerIArbeidsgiverperiode = 0
            splitter.ikkeSykedager = 0
            splitter.fridager = 0
        }

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.oppdatereInntekt(dagen.minusDays(1))
            splitter.håndterArbeidsgiverdag(dagen)
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.oppdatereInntekt(dagen.minusDays(1))
            splitter.håndterNAVdag(dagen, økonomi)
            splitter.state(UtbetalingSykedager)
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.oppdatereInntekt(dagen.minusDays(1))
            splitter.håndterArbeidsgiverdag(dagen)
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.oppdatereInntekt(dagen.minusDays(1))
            splitter.håndterNAVHelgedag(dagen, økonomi)
            splitter.state(UtbetalingSykedager)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }
    }

    private object ArbeidsgiverperiodeSykedager : UtbetalingState {

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(UtbetalingSykedager).also { splitter.håndterNAVdag(dagen, økonomi) }
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVHelgedag(dagen, økonomi)
            splitter.state(UtbetalingSykedager)
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            splitter.state(ArbeidsgiverperiodeOpphold)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.fridager = 0
            splitter.håndterFridag(dagen)
            splitter.state(ArbeidsgiverperiodeFri)
        }
    }

    private object ArbeidsgiverperiodeFri : UtbetalingState {
        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
        }

        override fun egenmeldingsdagIArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterFriEgenmeldingsdag(dagen)
        }

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.sykedagerIArbeidsgiverperiode += splitter.fridager
            if (splitter.arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(splitter.sykedagerIArbeidsgiverperiode))
                splitter.state(UtbetalingSykedager).also { splitter.håndterNAVdag(dagen, økonomi) }
            else splitter.state(ArbeidsgiverperiodeSykedager)
                .also { splitter.håndterArbeidsgiverdag(dagen) }
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            splitter.ikkeSykedager =
                if (splitter.arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(splitter.sykedagerIArbeidsgiverperiode)) {
                    1
                } else {
                    splitter.fridager + 1
                }
            splitter.state(if (splitter.arbeidsgiverRegler.burdeStarteNyArbeidsgiverperiode(splitter.ikkeSykedager)) Initiell else ArbeidsgiverperiodeOpphold)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(UtbetalingSykedager).also { splitter.håndterNAVdag(dagen, økonomi) }
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.sykedagerIArbeidsgiverperiode += splitter.fridager
            if (splitter.arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(splitter.sykedagerIArbeidsgiverperiode)) {
                splitter.håndterNAVHelgedag(dagen, økonomi)
                splitter.state(UtbetalingSykedager)
            } else {
                splitter.håndterArbeidsgiverdag(dagen)
                splitter.state(ArbeidsgiverperiodeSykedager)
            }
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVHelgedag(dagen, økonomi)
        }
    }

    private object ArbeidsgiverperiodeOpphold : UtbetalingState {
        override fun entering(splitter: UtbetalingstidslinjeBuilder) {
            splitter.ikkeSykedager = 1
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            splitter.state(Initiell)
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVdag(dagen, økonomi)
            splitter.state(UtbetalingSykedager)
        }

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterArbeidsgiverdag(dagen)
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.inkrementerIkkeSykedager()
            splitter.håndterFridag(dagen)
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterArbeidsgiverdag(dagen)
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVHelgedag(dagen, økonomi)
            splitter.state(UtbetalingSykedager)
        }
    }

    private object UtbetalingSykedager : UtbetalingState {
        override fun entering(splitter: UtbetalingstidslinjeBuilder) {
            splitter.ikkeSykedager = 0
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(Ugyldig)
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVHelgedag(dagen, økonomi)
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVdag(dagen, økonomi)
        }

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(Ugyldig)
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            splitter.state(UtbetalingOpphold)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
            splitter.state(UtbetalingFri)
        }
    }

    private object UtbetalingFri : UtbetalingState {
        override fun entering(splitter: UtbetalingstidslinjeBuilder) {
            splitter.fridager = 1
        }

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(Ugyldig)
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVdag(dagen, økonomi)
            splitter.state(UtbetalingSykedager)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            splitter.ikkeSykedager = 1
            splitter.state(UtbetalingOpphold)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(Ugyldig)
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVHelgedag(dagen, økonomi)
            splitter.state(UtbetalingSykedager)
        }
    }

    private object UtbetalingOpphold : UtbetalingState {
        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVdag(dagen, økonomi)
            splitter.state(UtbetalingSykedager)
        }

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(Ugyldig)
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            splitter.state(Initiell)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
            splitter.inkrementerIkkeSykedager()
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(Ugyldig)
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterFridag(dagen)
            splitter.inkrementerIkkeSykedager()
        }
    }

    private object Ugyldig : UtbetalingState {
        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
        }
    }

}
