package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*
import java.math.BigDecimal
import java.time.LocalDate

internal class Utbetalingsberegner(private val dagsats: BigDecimal) : SykdomstidslinjeVisitor {

    private var state: UtbetalingState = Initiell
    private val utbetalingslinjer = mutableListOf<Utbetalingslinje>()

    private var sykedager = 0
    private var ikkeSykedager = 0
    private var fridager = 0

    fun results(): List<Utbetalingslinje> {
        require(state != Ugyldig)
        return utbetalingslinjer.toList()
    }

    private fun state(state: UtbetalingState) {
        this.state.leaving(this)
        this.state = state
        this.state.entering(this)
    }

    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) = arbeidsdag(arbeidsdag.dagen)
    override fun visitImplisittDag(implisittDag: ImplisittDag) = if (implisittDag.erHelg()) fridag(implisittDag.dagen) else arbeidsdag(implisittDag.dagen)
    override fun visitFeriedag(feriedag: Feriedag) = fridag(feriedag.dagen)
    override fun visitSykedag(sykedag: Sykedag) = sykedag(sykedag.dagen)
    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) = sykedag(egenmeldingsdag.dagen)
    override fun visitSykHelgedag(sykHelgedag: SykHelgedag) = sykedag(sykHelgedag.dagen)

    private fun arbeidsdag(dagen: LocalDate) {
        //Siden telleren alltid er en dag bak dagen vi ser på, sjekker vi for < 16 i stedet for <= 16
        if (ikkeSykedager < 16) state.færreEllerLik16arbeidsdager(this, dagen) else state.merEnn16arbeidsdager(this, dagen)
    }

    private fun sykedag(dagen: LocalDate) {
        //Siden telleren alltid er en dag bak dagen vi ser på, sjekker vi for < 16 i stedet for <= 16
        if (sykedager < 16) state.færreEllerLik16Sykedager(this, dagen) else state.merEnn16Sykedager(this, dagen)
    }

    private fun fridag(dagen: LocalDate){
        state.fridag(this, dagen)
    }

    private fun opprettBetalingslinje(dagen: LocalDate) {
        utbetalingslinjer.add(Utbetalingslinje(dagen,dagsats))
    }

    override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) = state(Ugyldig)
    override fun visitUbestemt(ubestemtdag: Ubestemtdag) = state(Ugyldig)
    override fun visitStudiedag(studiedag: Studiedag) = state(Ugyldig)
    override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag) = state(Ugyldig)

    abstract class UtbetalingState {
        open fun færreEllerLik16Sykedager(splitter: Utbetalingsberegner, dagen: LocalDate) {}
        open fun merEnn16Sykedager(splitter: Utbetalingsberegner, dagen: LocalDate) {}
        open fun fridag(splitter: Utbetalingsberegner, dagen: LocalDate) {}
        open fun færreEllerLik16arbeidsdager(splitter: Utbetalingsberegner, dagen: LocalDate) {}
        open fun merEnn16arbeidsdager(splitter: Utbetalingsberegner, dagen: LocalDate) {}

        open fun entering(splitter: Utbetalingsberegner) {}
        open fun leaving(splitter: Utbetalingsberegner) {}
    }

    private object Initiell : UtbetalingState() {
        override fun entering(splitter: Utbetalingsberegner) {
            splitter.sykedager = 0
            splitter.ikkeSykedager = 0
            splitter.fridager = 0
        }

        override fun færreEllerLik16Sykedager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.sykedager = 1
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun merEnn16Sykedager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }
    }

    private object ArbeidsgiverperiodeSykedager : UtbetalingState() {
        override fun færreEllerLik16Sykedager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.sykedager += 1
        }

        override fun merEnn16Sykedager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.state(UtbetalingSykedager).also { splitter.opprettBetalingslinje(dagen) }
        }

        override fun fridag(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.fridager = 1
            splitter.state(Fri)
        }

        override fun færreEllerLik16arbeidsdager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.ikkeSykedager = 1
            splitter.state(ArbeidsgiverperiodeOpphold)
        }

        override fun merEnn16arbeidsdager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }
    }

    private object ArbeidsgiverperiodeOpphold : UtbetalingState() {
        override fun fridag(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.ikkeSykedager += 1
            if (splitter.ikkeSykedager == 16) splitter.state(Initiell)
        }

        override fun færreEllerLik16arbeidsdager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.ikkeSykedager += 1
        }

        override fun merEnn16arbeidsdager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.state(Initiell)
        }

        override fun færreEllerLik16Sykedager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.sykedager += 1
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun merEnn16Sykedager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.opprettBetalingslinje(dagen)
            splitter.state(UtbetalingSykedager)
        }
    }

    private object Fri : UtbetalingState() {
        override fun fridag(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.fridager += 1
        }

        override fun færreEllerLik16arbeidsdager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.ikkeSykedager = (if (splitter.sykedager >= 16) 0 else splitter.fridager) + 1
            if (splitter.ikkeSykedager >= 16) splitter.state(Initiell) else splitter.state =
                ArbeidsgiverperiodeOpphold
        }

        override fun merEnn16arbeidsdager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun færreEllerLik16Sykedager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.sykedager += splitter.fridager + 1
            if (splitter.sykedager >= 16) splitter.state =
                UtbetalingSykedager.also { splitter.opprettBetalingslinje(dagen) }
            else splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun merEnn16Sykedager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.state(UtbetalingSykedager).also { splitter.opprettBetalingslinje(dagen) }
        }
    }

    private object UtbetalingSykedager : UtbetalingState() {
        override fun entering(splitter: Utbetalingsberegner) {
            splitter.ikkeSykedager = 0
        }

        override fun merEnn16Sykedager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.utbetalingslinjer.last().tom = dagen
        }

        override fun færreEllerLik16Sykedager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun færreEllerLik16arbeidsdager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.ikkeSykedager = 1
            splitter.state(UtbetalingOpphold)
        }

        override fun merEnn16arbeidsdager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun fridag(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.state(UtbetalingFri)
        }
    }

    private object UtbetalingFri: UtbetalingState() {
        override fun entering(splitter: Utbetalingsberegner) {
            splitter.fridager = 1
        }

        override fun færreEllerLik16Sykedager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun merEnn16Sykedager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.opprettBetalingslinje(dagen)
            splitter.state(UtbetalingSykedager)
        }

        override fun fridag(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.fridager += 1
        }

        override fun færreEllerLik16arbeidsdager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.ikkeSykedager = splitter.fridager + 1
            if (splitter.ikkeSykedager >= 16) splitter.state(Ugyldig) else splitter.state =
                UtbetalingOpphold
        }

        override fun merEnn16arbeidsdager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.ikkeSykedager = splitter.fridager + 1
            if (splitter.ikkeSykedager >= 16) splitter.state(Ugyldig) else splitter.state =
                UtbetalingOpphold
        }
    }

    private object UtbetalingOpphold: UtbetalingState() {
        override fun merEnn16Sykedager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.opprettBetalingslinje(dagen)
            splitter.state(UtbetalingSykedager)
        }

        override fun færreEllerLik16Sykedager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun færreEllerLik16arbeidsdager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.ikkeSykedager += 1
        }

        override fun merEnn16arbeidsdager(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun fridag(splitter: Utbetalingsberegner, dagen: LocalDate) {
            splitter.ikkeSykedager += 1
            if (splitter.ikkeSykedager == 16) splitter.state(Ugyldig)
        }
    }

    private object Ugyldig : UtbetalingState()
}
