package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.InntektshistorikkVol2
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

/**
 *  Forstår opprettelsen av en Utbetalingstidslinje
 */

internal class UtbetalingstidslinjeBuilderVol2 internal constructor(
    private val sammenhengendePeriode: Periode,
    private val skjæringstidspunkter: List<LocalDate>,
    private val inntektshistorikk: InntektshistorikkVol2,
    private val forlengelseStrategy: (LocalDate) -> Boolean = { false },
    private val arbeidsgiverRegler: ArbeidsgiverRegler = NormalArbeidstaker
) : SykdomstidslinjeVisitor {
    private var tilstand: UtbetalingState = Initiell

    private var sykedagerIArbeidsgiverperiode = 0
    private var ikkeSykedager = 0
    private var fridager = 0

    private val tidslinje = Utbetalingstidslinje()

    private fun inntektForDatoOrNull(dato: LocalDate) =
        skjæringstidspunkter
            .sorted()
            .lastOrNull { it <= dato }
            ?.let { skjæringstidspunkt ->
                inntektshistorikk.grunnlagForSykepengegrunnlag(skjæringstidspunkt, dato)
                    ?.let { inntekt -> skjæringstidspunkt to inntekt }
            }

    private fun inntektForDato(dato: LocalDate) =
        requireNotNull(inntektForDatoOrNull(dato)) { "Fant ikke inntekt for $dato med skjæringstidspunkter $skjæringstidspunkter" }

    private fun Økonomi.inntektIfNotNull(dato: LocalDate) =
        inntektForDatoOrNull(dato)
            ?.let { (skjæringstidspunkt, inntekt) -> inntekt(inntekt, inntekt.dekningsgrunnlag(arbeidsgiverRegler), skjæringstidspunkt) }
            ?: inntekt(INGEN)

    internal fun result(sykdomstidslinje: Sykdomstidslinje): Utbetalingstidslinje {
        Sykdomstidslinje(sykdomstidslinje, sammenhengendePeriode)
            .fremTilOgMed(sammenhengendePeriode.endInclusive)
            .accept(this)
        return tidslinje
    }

    override fun visitDag(dag: Dag.UkjentDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        implisittDag(dato)

    override fun visitDag(dag: Dag.Studiedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        implisittDag(dato)

    override fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        fridag(dato)

    override fun visitDag(dag: Dag.Utenlandsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        implisittDag(dato)

    override fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        arbeidsdag(dato)

    override fun visitDag(
        dag: Dag.Arbeidsgiverdag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = egenmeldingsdag(dato)

    override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        fridag(dato)

    override fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        arbeidsdag(dato)

    override fun visitDag(
        dag: Dag.ArbeidsgiverHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = sykHelgedag(dato, økonomi)

    override fun visitDag(
        dag: Dag.Sykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = sykedag(dato, økonomi)

    override fun visitDag(
        dag: Dag.ForeldetSykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = foreldetSykedag(dato, økonomi)

    override fun visitDag(
        dag: Dag.SykHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = sykHelgedag(dato, økonomi)

    override fun visitDag(
        dag: Dag.ProblemDag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde,
        melding: String
    ) = throw IllegalArgumentException("Forventet ikke problemdag i utbetalingstidslinjen. Melding: $melding")

    private fun foreldetSykedag(dagen: LocalDate, økonomi: Økonomi) {
        if (arbeidsgiverperiodeGjennomført(dagen)) {
            tilstand = UtbetalingSykedager
            val (skjæringstidspunkt, inntekt) = inntektForDato(dagen)
            tidslinje.addForeldetDag(
                dagen,
                økonomi.inntekt(inntekt, inntekt.dekningsgrunnlag(arbeidsgiverRegler), skjæringstidspunkt)
            )
        } else tilstand.sykedagerIArbeidsgiverperioden(this, dagen, økonomi)
    }

    private fun egenmeldingsdag(dato: LocalDate) =
        if (arbeidsgiverperiodeGjennomført(dato)) {
            tidslinje.addAvvistDag(
                dato,
                Økonomi.ikkeBetalt().inntektIfNotNull(dato),
                Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode
            )
        } else tilstand.egenmeldingsdagIArbeidsgiverperioden(this, dato)

    private fun implisittDag(dagen: LocalDate) = if (dagen.erHelg()) fridag(dagen) else arbeidsdag(dagen)

    private fun sykedag(dagen: LocalDate, økonomi: Økonomi) {
        if (arbeidsgiverperiodeGjennomført(dagen))
            tilstand.sykedagerEtterArbeidsgiverperioden(this, dagen, økonomi)
        else
            tilstand.sykedagerIArbeidsgiverperioden(this, dagen, økonomi)
    }

    private fun sykHelgedag(dagen: LocalDate, økonomi: Økonomi) =
        if (arbeidsgiverperiodeGjennomført(dagen))
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

    private fun addArbeidsgiverdag(dato: LocalDate) {
        tidslinje.addArbeidsgiverperiodedag(
            dato,
            Økonomi.ikkeBetalt().inntektIfNotNull(dato)
        )
    }

    private fun håndterArbeidsgiverdag(dagen: LocalDate) {
        sykedagerIArbeidsgiverperiode += 1
        addArbeidsgiverdag(dagen)
    }

    private fun håndterNAVdag(dato: LocalDate, økonomi: Økonomi) {
        tidslinje.addNAVdag(
            dato,
            inntektForDato(dato).let { (skjæringstidspunkt, inntekt) ->
                økonomi.inntekt(inntekt, inntekt.dekningsgrunnlag(arbeidsgiverRegler), skjæringstidspunkt)
            }
        )
    }

    private fun håndterNAVHelgedag(dato: LocalDate, økonomi: Økonomi) {
        tidslinje.addHelg(dato, økonomi.inntekt(INGEN, skjæringstidspunkt = inntektForDatoOrNull(dato)?.let { (skjæringstidspunkt) -> skjæringstidspunkt }))
    }

    private fun håndterArbeidsdag(dato: LocalDate) {
        inkrementerIkkeSykedager()
        tidslinje.addArbeidsdag(
            dato,
            Økonomi.ikkeBetalt().inntektIfNotNull(dato)
        )
    }

    private fun inkrementerIkkeSykedager() {
        ikkeSykedager += 1
        if (arbeidsgiverRegler.burdeStarteNyArbeidsgiverperiode(ikkeSykedager)) state(Initiell)
    }

    private fun håndterFridag(dato: LocalDate) {
        fridager += 1
        tidslinje.addFridag(
            dato,
            Økonomi.ikkeBetalt().inntektIfNotNull(dato)
        )
    }

    private fun håndterFriEgenmeldingsdag(dato: LocalDate) {
        sykedagerIArbeidsgiverperiode += fridager
        tidslinje.addAvvistDag(
            dato,
            Økonomi.ikkeBetalt().inntektIfNotNull(dato),
            Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode
        )
        if (arbeidsgiverperiodeGjennomført(dato))
            state(UtbetalingSykedager)
        else
            state(ArbeidsgiverperiodeSykedager)
    }

    private fun arbeidsgiverperiodeGjennomført(dagen: LocalDate): Boolean {
        if (sykedagerIArbeidsgiverperiode == 0 && forlengelseStrategy(dagen)) sykedagerIArbeidsgiverperiode += 16
        return arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(sykedagerIArbeidsgiverperiode)
    }

    private fun state(state: UtbetalingState) {
        this.tilstand.leaving(this)
        this.tilstand = state
        this.tilstand.entering(this)
    }

    internal interface UtbetalingState {
        fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        )

        fun egenmeldingsdagIArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            sykedagerIArbeidsgiverperioden(splitter, dagen, Økonomi.ikkeBetalt())
        }

        fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        )

        fun fridag(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate)
        fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate)
        fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate)
        fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        )

        fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        )

        fun entering(splitter: UtbetalingstidslinjeBuilderVol2) {}
        fun leaving(splitter: UtbetalingstidslinjeBuilderVol2) {}
    }

    private object Initiell : UtbetalingState {

        override fun entering(splitter: UtbetalingstidslinjeBuilderVol2) {
            splitter.sykedagerIArbeidsgiverperiode = 0
            splitter.ikkeSykedager = 0
            splitter.fridager = 0
        }

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.ikkeSykedager = 0
            splitter.fridager = 0
            splitter.håndterArbeidsgiverdag(dagen)
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVdag(dagen, økonomi)
            splitter.state(UtbetalingSykedager)
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.ikkeSykedager = 0
            splitter.fridager = 0
            splitter.håndterArbeidsgiverdag(dagen)
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVHelgedag(dagen, økonomi)
            splitter.state(UtbetalingSykedager)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }
    }

    private object ArbeidsgiverperiodeSykedager : UtbetalingState {

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(UtbetalingSykedager).also { splitter.håndterNAVdag(dagen, økonomi) }
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVHelgedag(dagen, økonomi)
            splitter.state(UtbetalingSykedager)
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            splitter.state(ArbeidsgiverperiodeOpphold)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.fridager = 0
            splitter.håndterFridag(dagen)
            splitter.state(ArbeidsgiverperiodeFri)
        }
    }

    private object ArbeidsgiverperiodeFri : UtbetalingState {
        override fun fridag(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
        }

        override fun egenmeldingsdagIArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.håndterFriEgenmeldingsdag(dagen)
        }

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.sykedagerIArbeidsgiverperiode += splitter.fridager
            if (splitter.arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(splitter.sykedagerIArbeidsgiverperiode))
                splitter.state(UtbetalingSykedager).also { splitter.håndterNAVdag(dagen, økonomi) }
            else splitter.state(ArbeidsgiverperiodeSykedager)
                .also { splitter.håndterArbeidsgiverdag(dagen) }
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            splitter.ikkeSykedager =
                if (splitter.arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(splitter.sykedagerIArbeidsgiverperiode)) {
                    1
                } else {
                    splitter.fridager + 1
                }
            splitter.state(if (splitter.arbeidsgiverRegler.burdeStarteNyArbeidsgiverperiode(splitter.ikkeSykedager)) Initiell else ArbeidsgiverperiodeOpphold)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(UtbetalingSykedager).also { splitter.håndterNAVdag(dagen, økonomi) }
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
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
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVHelgedag(dagen, økonomi)
        }
    }

    private object ArbeidsgiverperiodeOpphold : UtbetalingState {
        override fun entering(splitter: UtbetalingstidslinjeBuilderVol2) {
            splitter.ikkeSykedager = 1
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            splitter.state(Initiell)
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVdag(dagen, økonomi)
            splitter.state(UtbetalingSykedager)
        }

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterArbeidsgiverdag(dagen)
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.inkrementerIkkeSykedager()
            splitter.håndterFridag(dagen)
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterArbeidsgiverdag(dagen)
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVHelgedag(dagen, økonomi)
            splitter.state(UtbetalingSykedager)
        }
    }

    private object UtbetalingSykedager : UtbetalingState {
        override fun entering(splitter: UtbetalingstidslinjeBuilderVol2) {
            splitter.ikkeSykedager = 0
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(Ugyldig)
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVHelgedag(dagen, økonomi)
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVdag(dagen, økonomi)
        }

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(Ugyldig)
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            splitter.state(UtbetalingOpphold)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
            splitter.state(UtbetalingFri)
        }
    }

    private object UtbetalingFri : UtbetalingState {
        override fun entering(splitter: UtbetalingstidslinjeBuilderVol2) {
            splitter.fridager = 1
        }

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(Ugyldig)
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVdag(dagen, økonomi)
            splitter.state(UtbetalingSykedager)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            splitter.ikkeSykedager = 1
            splitter.state(UtbetalingOpphold)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(Ugyldig)
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVHelgedag(dagen, økonomi)
            splitter.state(UtbetalingSykedager)
        }
    }

    private object UtbetalingOpphold : UtbetalingState {
        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterNAVdag(dagen, økonomi)
            splitter.state(UtbetalingSykedager)
        }

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(Ugyldig)
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            splitter.state(Initiell)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
            splitter.inkrementerIkkeSykedager()
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(Ugyldig)
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterFridag(dagen)
            splitter.inkrementerIkkeSykedager()
        }
    }

    private object Ugyldig : UtbetalingState {
        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilderVol2, dagen: LocalDate) {
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilderVol2,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
        }
    }

}
