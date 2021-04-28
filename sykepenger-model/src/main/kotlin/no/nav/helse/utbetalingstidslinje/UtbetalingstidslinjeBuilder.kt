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
        tilstand.justerSykedagtelling(this, dagen)
        tilstand.foreldetSykedag(this, dagen, økonomi)
    }

    private fun egenmeldingsdag(dato: LocalDate) {
        tilstand.justerSykedagtelling(this, dato)
        tilstand.egenmeldingsdag(this, dato)
    }

    private fun implisittDag(dagen: LocalDate) = if (dagen.erHelg()) fridag(dagen) else arbeidsdag(dagen)

    private fun sykedag(dagen: LocalDate, økonomi: Økonomi) {
        tilstand.justerSykedagtelling(this, dagen)
        tilstand.sykedag(this, dagen, økonomi)
    }

    private fun sykHelgedag(dagen: LocalDate, økonomi: Økonomi) {
        tilstand.justerSykedagtelling(this, dagen)
        tilstand.sykHelgedag(this, dagen, økonomi)
    }

    private fun arbeidsdag(dagen: LocalDate) {
        tilstand.justerIkkeSykedagtelling(this)
        tilstand.arbeidsdag(this, dagen)
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
        if (!arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(sykedagerIArbeidsgiverperiode)) return false
        state(UtbetalingSykedager)
        return true
    }

    private fun inkrementerSykedagerIArbeidsgiverperiodeFraInfotrygd(dagen: LocalDate) {
        if (!forlengelseStrategy.erArbeidsgiverperiodenGjennomførtFør(dagen)) return
        sykedagerIArbeidsgiverperiode = arbeidsgiverRegler.fullførArbeidsgiverperiode()
        state(UtbetalingSykedager)
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

    private fun state(state: UtbetalingState) {
        this.tilstand.leaving(this)
        this.tilstand = state
        this.tilstand.entering(this)
    }

    internal interface UtbetalingState {
        fun justerSykedagtelling(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) { }
        fun sykedag(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        )
        fun foreldetSykedag(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            sykedag(splitter, dagen, økonomi)
        }

        fun egenmeldingsdag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            sykedag(splitter, dagen, Økonomi.ikkeBetalt())
        }

        fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate)
        fun justerIkkeSykedagtelling(splitter: UtbetalingstidslinjeBuilder) {}
        fun arbeidsdag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate)
        fun sykHelgedag(
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
        }

        override fun leaving(splitter: UtbetalingstidslinjeBuilder) {
            splitter.ikkeSykedager = 0
        }

        override fun justerSykedagtelling(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.inkrementerSykedagerIArbeidsgiverperiodeFraInfotrygd(dagen)
        }

        override fun sykedag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, økonomi: Økonomi) {
            splitter.state(ArbeidsgiverperiodeSykedager)
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun sykHelgedag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, økonomi: Økonomi) {
            splitter.state(ArbeidsgiverperiodeSykedager)
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.addFridag(dagen)
        }

        override fun arbeidsdag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
        }

    }

    private object ArbeidsgiverperiodeSykedager : UtbetalingState {
        override fun sykedag(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun sykHelgedag(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun arbeidsdag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(ArbeidsgiverperiodeOpphold)
            splitter.håndterArbeidsdag(dagen)
        }


        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(ArbeidsgiverperiodeFri)
            splitter.håndterUsikkerFridag(dagen)
        }
    }

    private object ArbeidsgiverperiodeFri : UtbetalingState {
        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterUsikkerFridag(dagen)
        }

        override fun justerSykedagtelling(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            // når vi treffer en sykedag skal evt. ferie/helg i forkant telles som en del av arbeidsgiverperioden
            splitter.inkrementerSykedagerIArbeidsgiverperiodeMedFridager()
        }

        override fun sykedag(
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

        override fun arbeidsdag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(ArbeidsgiverperiodeOpphold)
            splitter.håndterArbeidsdag(dagen)
        }

        override fun sykHelgedag(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(ArbeidsgiverperiodeSykedager)
            splitter.håndterArbeidsgiverdag(dagen)
        }

    }

    private object ArbeidsgiverperiodeOpphold : UtbetalingState {
        override fun leaving(splitter: UtbetalingstidslinjeBuilder) {
            splitter.ikkeSykedager = 0
        }

        override fun arbeidsdag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
        }

        override fun sykedag(
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

        override fun sykHelgedag(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(ArbeidsgiverperiodeSykedager)
            splitter.håndterArbeidsgiverdag(dagen)
        }

    }

    private object UtbetalingSykedager : UtbetalingState {
        override fun sykHelgedag(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.addNAVHelgedag(dagen, økonomi)
        }

        override fun egenmeldingsdag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.addAvvistDag(dagen)
        }

        override fun foreldetSykedag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, økonomi: Økonomi) {
            splitter.addForeldetDag(dagen, økonomi)
        }

        override fun sykedag(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.addNAVdag(dagen, økonomi)
        }

        override fun arbeidsdag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(UtbetalingOpphold)
            splitter.håndterArbeidsdag(dagen)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(UtbetalingFri)
            splitter.addFridag(dagen)
        }
    }

    private object UtbetalingFri : UtbetalingState {
        override fun sykedag(
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

        override fun arbeidsdag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.state(UtbetalingOpphold)
            splitter.håndterArbeidsdag(dagen)
        }

        override fun sykHelgedag(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(UtbetalingSykedager)
            splitter.addNAVHelgedag(dagen, økonomi)
        }
    }

    private object UtbetalingOpphold : UtbetalingState {
        override fun leaving(splitter: UtbetalingstidslinjeBuilder) {
            splitter.ikkeSykedager = 0
        }

        override fun sykedag(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(UtbetalingSykedager)
            splitter.addNAVdag(dagen, økonomi)
        }

        override fun arbeidsdag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterFridagOpphold(dagen)
        }

        override fun sykHelgedag(
            splitter: UtbetalingstidslinjeBuilder,
            dagen: LocalDate,
            økonomi: Økonomi
        ) {
            splitter.state(UtbetalingSykedager)
            splitter.addNAVHelgedag(dagen, økonomi)
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
