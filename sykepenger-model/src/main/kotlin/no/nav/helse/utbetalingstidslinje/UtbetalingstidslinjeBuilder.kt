package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilderException.ManglerInntektException
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilderException.UforventetDagException
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal fun interface IUtbetalingstidslinjeBuilder {
    fun result(sykdomstidslinje: Sykdomstidslinje, periode: Periode): Utbetalingstidslinje
}

internal fun interface Forlengelsestrategi {
    fun erArbeidsgiverperiodenGjennomførtFør(dagen: LocalDate): Boolean

    companion object {
        val Ingen = Forlengelsestrategi { false }
    }
}
/**
 *  Forstår opprettelsen av en Utbetalingstidslinje
 */

internal class UtbetalingstidslinjeBuilder internal constructor(
    private val skjæringstidspunkter: List<LocalDate>,
    private val inntektshistorikk: Inntektshistorikk,
    private val arbeidsgiverRegler: ArbeidsgiverRegler = NormalArbeidstaker
) : SykdomstidslinjeVisitor, IUtbetalingstidslinjeBuilder {
    private var forlengelseStrategy: Forlengelsestrategi = Forlengelsestrategi.Ingen
    private var tilstand: UtbetalingState = Initiell

    private var sykedagerIArbeidsgiverperiode = 0
    private var ikkeSykedager = 0
    private var fridager = mutableListOf<LocalDate>()

    private val tidslinje = Utbetalingstidslinje()

    internal fun forlengelsestrategi(strategi: Forlengelsestrategi) {
        this.forlengelseStrategy = strategi
    }

    private fun inntektForDatoOrNull(dato: LocalDate) =
        skjæringstidspunkter
            .sorted()
            .lastOrNull { it <= dato }
            ?.let { skjæringstidspunkt ->
                inntektshistorikk.grunnlagForSykepengegrunnlag(skjæringstidspunkt, dato)
                    ?.let { inntekt -> skjæringstidspunkt to inntekt }
            }

    private fun inntektForDato(dato: LocalDate) =
        inntektForDatoOrNull(dato) ?: throw ManglerInntektException(dato, skjæringstidspunkter)

    private fun Økonomi.inntektIfNotNull(dato: LocalDate) =
        inntektForDatoOrNull(dato)
            ?.let { (skjæringstidspunkt, inntekt) -> inntekt(inntekt, inntekt.dekningsgrunnlag(arbeidsgiverRegler), skjæringstidspunkt) }
            ?: inntekt(INGEN, skjæringstidspunkt = dato)

    override fun result(sykdomstidslinje: Sykdomstidslinje, periode: Periode): Utbetalingstidslinje {
        sykdomstidslinje.fremTilOgMed(periode.endInclusive).accept(this)
        hengendeFridager()
        return tidslinje
    }

    override fun visitDag(dag: Dag.UkjentDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        implisittDag(dato)

    override fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        fridag(dato)

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
    ) = throw UforventetDagException(dag, melding)

    private fun addForeldetDag(dagen: LocalDate, økonomi: Økonomi) {
        val (skjæringstidspunkt, inntekt) = inntektForDato(dagen)
        tidslinje.addForeldetDag(dagen, økonomi.inntekt(inntekt, inntekt.dekningsgrunnlag(arbeidsgiverRegler), skjæringstidspunkt))
    }

    private fun addArbeidsgiverdag(dato: LocalDate) {
        tidslinje.addArbeidsgiverperiodedag(dato, Økonomi.ikkeBetalt().inntektIfNotNull(dato))
    }

    private fun addNAVdag(dato: LocalDate, økonomi: Økonomi) {
        val (skjæringstidspunkt, inntekt) = inntektForDato(dato)
        tidslinje.addNAVdag(dato, økonomi.inntekt(inntekt, inntekt.dekningsgrunnlag(arbeidsgiverRegler), skjæringstidspunkt))
    }

    private fun addNAVHelgedag(dato: LocalDate, økonomi: Økonomi) {
        val skjæringstidspunkt = inntektForDatoOrNull(dato)?.let { (skjæringstidspunkt) -> skjæringstidspunkt } ?: dato
        tidslinje.addHelg(dato, økonomi.inntekt(INGEN, skjæringstidspunkt = skjæringstidspunkt))
    }

    private fun addArbeidsdag(dato: LocalDate) {
        tidslinje.addArbeidsdag(dato, Økonomi.ikkeBetalt().inntektIfNotNull(dato))
    }

    private fun addAvvistDag(dato: LocalDate) {
        tidslinje.addAvvistDag(dato, Økonomi.ikkeBetalt().inntektIfNotNull(dato), listOf(Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode))
    }

    private fun addFridag(dato: LocalDate) {
        tidslinje.addFridag(dato, Økonomi.ikkeBetalt().inntektIfNotNull(dato))
    }

    private fun foreldetSykedag(dagen: LocalDate, økonomi: Økonomi) {
        tilstand.justerSykedagtelling(this)
        if (arbeidsgiverperiodeGjennomført(dagen)) {
            state(UtbetalingSykedager)
            addForeldetDag(dagen, økonomi)
        } else tilstand.sykedagerIArbeidsgiverperioden(this, dagen, økonomi)
    }

    private fun egenmeldingsdag(dato: LocalDate) =
        if (arbeidsgiverperiodeGjennomført(dato))
            addAvvistDag(dato)
        else
            tilstand.egenmeldingsdagIArbeidsgiverperioden(this, dato)

    private fun implisittDag(dagen: LocalDate) = if (dagen.erHelg()) fridag(dagen) else arbeidsdag(dagen)

    private fun sykedag(dagen: LocalDate, økonomi: Økonomi) {
        tilstand.justerSykedagtelling(this)
        if (arbeidsgiverperiodeGjennomført(dagen))
            tilstand.sykedagerEtterArbeidsgiverperioden(this, dagen, økonomi)
        else
            tilstand.sykedagerIArbeidsgiverperioden(this, dagen, økonomi)
    }

    private fun sykHelgedag(dagen: LocalDate, økonomi: Økonomi) {
        tilstand.justerSykedagtelling(this)
        if (arbeidsgiverperiodeGjennomført(dagen))
            tilstand.sykHelgedagEtterArbeidsgiverperioden(this, dagen, økonomi)
        else
            tilstand.sykHelgedagIArbeidsgiverperioden(this, dagen, økonomi)
    }

    private fun arbeidsdag(dagen: LocalDate) {
        tilstand.justerIkkeSykedagtelling(this)
        if (arbeidsgiverRegler.burdeStarteNyArbeidsgiverperiode(ikkeSykedager))
            tilstand.arbeidsdagerEtterOppholdsdager(this, dagen)
        else
            tilstand.arbeidsdagerIOppholdsdager(this, dagen)
    }

    private fun fridag(dagen: LocalDate) {
        tilstand.fridag(this, dagen)
    }

    private fun håndterArbeidsgiverdag(dagen: LocalDate) {
        inkrementerSykedagerIArbeidsgiverperiode()
        addArbeidsgiverdag(dagen)
    }

    private fun håndterArbeidsdag(dagen: LocalDate) {
        inkrementerIkkeSykedager()
        addArbeidsdag(dagen)
    }
    private fun inkrementerSykedagerIArbeidsgiverperiode(): Boolean {
        sykedagerIArbeidsgiverperiode += 1
        if (!arbeidsgiverperiodeGjennomført()) return false
        state(UtbetalingSykedager)
        return true
    }

    private fun inkrementerSykedagerIArbeidsgiverperiodeMedFridager() {
        fridager.onEach {
            if (!inkrementerSykedagerIArbeidsgiverperiode())
                addFridag(it) // TODO: ArbeidsgiverperiodeDag
            else addFridag(it)
        }.clear()
    }

    private fun hengendeFridager() {
        fridager.onEach { addFridag(it) }.clear()
    }

    private fun inkrementerIkkeSykedagerFraFridager() {
        fridager.onEach { håndterFridagOpphold(it) }.clear()
    }

    private fun inkrementerIkkeSykedager() {
        ikkeSykedager += 1
        if (!arbeidsgiverRegler.burdeStarteNyArbeidsgiverperiode(ikkeSykedager)) return
        state(Initiell)
    }

    private fun håndterUsikkerFridag(dato: LocalDate) {
        fridager.add(dato)
    }

    private fun håndterFridagOpphold(dagen: LocalDate) {
        inkrementerIkkeSykedager()
        addFridag(dagen)
    }

    private fun håndterFriEgenmeldingsdag(dato: LocalDate) {
        inkrementerSykedagerIArbeidsgiverperiodeMedFridager()
        addAvvistDag(dato)
        if (arbeidsgiverperiodeGjennomført()) return
        state(ArbeidsgiverperiodeSykedager)
    }

    private fun arbeidsgiverperiodeGjennomført() =
        arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(sykedagerIArbeidsgiverperiode)
    private fun arbeidsgiverperiodeGjennomført(dagen: LocalDate): Boolean {
        if (sykedagerIArbeidsgiverperiode == 0 && forlengelseStrategy.erArbeidsgiverperiodenGjennomførtFør(dagen))
            sykedagerIArbeidsgiverperiode = arbeidsgiverRegler.gjennomførArbeidsgiverperiode()
        return arbeidsgiverperiodeGjennomført()
    }

    private fun state(state: UtbetalingState) {
        this.tilstand.leaving(this)
        this.tilstand = state
        this.tilstand.entering(this)
    }

    internal interface UtbetalingState {
        fun justerSykedagtelling(splitter: UtbetalingstidslinjeBuilder) { }
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
        fun justerIkkeSykedagtelling(splitter: UtbetalingstidslinjeBuilder) {}
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
        }

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(ArbeidsgiverperiodeSykedager)
            splitter.ikkeSykedager = 0
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(UtbetalingSykedager)
            splitter.addNAVdag(dagen, økonomi)
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(ArbeidsgiverperiodeSykedager)
            splitter.ikkeSykedager = 0
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(UtbetalingSykedager)
            splitter.addNAVHelgedag(dagen, økonomi)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.addFridag(dagen)
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }
    }

    private object ArbeidsgiverperiodeSykedager : UtbetalingState {

        override fun entering(splitter: UtbetalingstidslinjeBuilder) {
            splitter.ikkeSykedager = 0
        }

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
            splitter.state(UtbetalingSykedager)
            splitter.addNAVdag(dagen, økonomi)
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(UtbetalingSykedager)
            splitter.addNAVHelgedag(dagen, økonomi)
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(ArbeidsgiverperiodeOpphold)
            splitter.håndterArbeidsdag(dagen)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(ArbeidsgiverperiodeFri)
            splitter.håndterUsikkerFridag(dagen)
        }
    }

    private object ArbeidsgiverperiodeFri : UtbetalingState {
        override fun entering(splitter: UtbetalingstidslinjeBuilder) {
            splitter.ikkeSykedager = 0
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterUsikkerFridag(dagen)
        }

        override fun egenmeldingsdagIArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterFriEgenmeldingsdag(dagen)
        }

        override fun justerSykedagtelling(splitter: UtbetalingstidslinjeBuilder) {
            // når vi treffer en sykedag skal evt. ferie/helg i forkant telles som en del av arbeidsgiverperioden
            splitter.inkrementerSykedagerIArbeidsgiverperiodeMedFridager()
        }

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(ArbeidsgiverperiodeSykedager)
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun justerIkkeSykedagtelling(splitter: UtbetalingstidslinjeBuilder) {
            // når vi treffer en arbeidsdag skal evt. ferie/helg i forkant _ikke_ telle som del av arbeidsgiverperioden,
            // men heller telles som oppholdsdager
            splitter.inkrementerIkkeSykedagerFraFridager()
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(ArbeidsgiverperiodeOpphold)
            splitter.håndterArbeidsdag(dagen)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(Initiell)
            splitter.addArbeidsdag(dagen)
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(UtbetalingSykedager)
            splitter.addNAVdag(dagen, økonomi)
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(ArbeidsgiverperiodeSykedager)
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(UtbetalingSykedager)
            splitter.addNAVHelgedag(dagen, økonomi)
        }
    }

    private object ArbeidsgiverperiodeOpphold : UtbetalingState {
        override fun entering(splitter: UtbetalingstidslinjeBuilder) {
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(Initiell)
            splitter.addArbeidsdag(dagen)
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(UtbetalingSykedager)
            splitter.addNAVdag(dagen, økonomi)
        }

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(ArbeidsgiverperiodeSykedager)
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterFridagOpphold(dagen)
        }

        override fun sykHelgedagIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(ArbeidsgiverperiodeSykedager)
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun sykHelgedagEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(UtbetalingSykedager)
            splitter.addNAVHelgedag(dagen, økonomi)
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
            splitter.addNAVHelgedag(dagen, økonomi)
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.addNAVdag(dagen, økonomi)
        }

        override fun sykedagerIArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(Ugyldig)
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(UtbetalingOpphold)
            splitter.addArbeidsdag(dagen)
        }

        override fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(UtbetalingFri)
            splitter.addFridag(dagen)
        }
    }

    private object UtbetalingFri : UtbetalingState {
        override fun entering(splitter: UtbetalingstidslinjeBuilder) {
            splitter.ikkeSykedager = 0
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
            splitter.state(UtbetalingSykedager)
            splitter.addNAVdag(dagen, økonomi)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.addFridag(dagen)
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(UtbetalingOpphold)
            splitter.addArbeidsdag(dagen)
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
            splitter.state(UtbetalingSykedager)
            splitter.addNAVHelgedag(dagen, økonomi)
        }
    }

    private object UtbetalingOpphold : UtbetalingState {
        override fun entering(splitter: UtbetalingstidslinjeBuilder) {
            splitter.ikkeSykedager = 1
        }

        override fun sykedagerEtterArbeidsgiverperioden(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(UtbetalingSykedager)
            splitter.addNAVdag(dagen, økonomi)
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
            splitter.state(Initiell)
            splitter.addArbeidsdag(dagen)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterFridagOpphold(dagen)
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
            splitter.state(UtbetalingSykedager)
            splitter.addNAVHelgedag(dagen, økonomi)
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

internal sealed class UtbetalingstidslinjeBuilderException(private val kort: String, message: String) : RuntimeException(message) {
    internal fun logg(aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Feilmelding: $message")
        aktivitetslogg.error("Feil ved utbetalingstidslinjebygging: $kort")
    }

    internal class ManglerInntektException(dagen: LocalDate, skjæringstidspunkter: List<LocalDate>) : UtbetalingstidslinjeBuilderException(
        "Mangler inntekt for dag",
        "Fant ikke inntekt for $dagen med skjæringstidspunkter $skjæringstidspunkter"
    )
    internal class UforventetDagException(dag: Dag, melding: String) : UtbetalingstidslinjeBuilderException(
        "Forventet ikke ${dag::class.simpleName}",
        "Forventet ikke ${dag::class.simpleName} i utbetalingstidslinjen. Melding: $melding"
    )
}
