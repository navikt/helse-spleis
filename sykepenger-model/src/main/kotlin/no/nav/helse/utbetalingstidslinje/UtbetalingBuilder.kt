package no.nav.helse.utbetalingstidslinje

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.*
import java.time.DayOfWeek
import java.time.LocalDate

/**
 *  Forstår opprettelsen av en ArbeidsgiverUtbetalingstidslinje
 */

internal class UtbetalingBuilder internal constructor(
    private val sykdomstidslinje: Sykdomstidslinje,
    private val inntektHistorie: InntektHistorie,
    private val alder: Alder
) : SykdomstidslinjeVisitor {
    private var state: UtbetalingState = Initiell

    private var sykedager = 0
    private var ikkeSykedager = 0
    private var fridager = 0

    private var betalteSykedager = 0
    private var betalteSykepengerEtter67 = 0

    private var nåværendeInntekt = 0.00

    private val tidslinje = ArbeidsgiverUtbetalingstidslinje()

    fun result(): ArbeidsgiverUtbetalingstidslinje {
        sykdomstidslinje.accept(this)
        return tidslinje
    }

    private fun LocalDate.erHelg() = this.dayOfWeek == DayOfWeek.SATURDAY || this.dayOfWeek == DayOfWeek.SUNDAY

    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) = arbeidsdag(arbeidsdag.dagen)
    override fun visitImplisittDag(implisittDag: ImplisittDag) = implisittDag(implisittDag.dagen)
    override fun visitFeriedag(feriedag: Feriedag) = fridag(feriedag.dagen)
    override fun visitSykedag(sykedag: Sykedag) = sykedag(sykedag.dagen)
    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) = egenmeldingsdag(egenmeldingsdag.dagen)
    override fun visitSykHelgedag(sykHelgedag: SykHelgedag) = sykHelgedag(sykHelgedag.dagen)
//    override fun postVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {
//        if(utbetalingslinjer.isNotEmpty()) {
//            maksdato = alder.maksdato(betalteSykedager, betalteSykepengerEtter67, utbetalingslinjer.last().tom)
//        }
//    }

    private fun egenmeldingsdag(dagen: LocalDate) = if (dagen.erHelg()) sykHelgedag(dagen) else sykedag(dagen)
    private fun implisittDag(dagen: LocalDate) = if (dagen.erHelg()) fridag(dagen) else arbeidsdag(dagen)

    //Siden telleren alltid er en dag bak dagen vi ser på, sjekker vi for < 16 i stedet for <= 16
    private fun sykedag(dagen: LocalDate) =
        if (sykedager < 16) state.færreEllerLik16Sykedager(this, dagen) else state.merEnn16Sykedager(this, dagen)

    private fun sykHelgedag(dagen: LocalDate) = state.sykHelgedag(this, dagen)

    //Siden telleren alltid er en dag bak dagen vi ser på, sjekker vi for < 16 i stedet for <= 16
    private fun arbeidsdag(dagen: LocalDate) =
        if (ikkeSykedager < 16) state.færreEllerLik16arbeidsdager(this, dagen) else state.merEnn16arbeidsdager(
            this,
            dagen
        )

    private fun fridag(dagen: LocalDate) {
        state.fridag(this, dagen)
    }

    private fun setNåværendeInntekt(dagen: LocalDate) {
        nåværendeInntekt = inntektHistorie.inntekt(dagen)
    }

    private fun addArbeidsgiverdag(dagen: LocalDate) {
        tidslinje.addArbeidsgiverperiodedag(nåværendeInntekt, dagen)
    }

    private fun håndterArbeidsgiverdag(dagen: LocalDate) {
        sykedager += 1
        addArbeidsgiverdag(dagen)
    }

    private fun håndterNAVdag(dagen: LocalDate) {
        tidslinje.addNAVdag(nåværendeInntekt, dagen)
    }

    private fun håndterNAVHelgedag(dagen: LocalDate) {
        tidslinje.addNAVdag(0.0, dagen)
    }

    private fun håndterArbeidsdag(dagen: LocalDate) {
        ikkeSykedager += 1
        setNåværendeInntekt(dagen)
        tidslinje.addArbeidsdag(dagen)
    }

    private fun håndterFridag(dagen: LocalDate) {
        fridager += 1
        tidslinje.addFridag(dagen)
    }

    private fun state(state: UtbetalingState) {
        this.state.leaving(this)
        this.state = state
        this.state.entering(this)
    }

    abstract class UtbetalingState {
        open fun færreEllerLik16Sykedager(splitter: UtbetalingBuilder, dagen: LocalDate) {}
        open fun merEnn16Sykedager(splitter: UtbetalingBuilder, dagen: LocalDate) {}

        open fun fridag(splitter: UtbetalingBuilder, dagen: LocalDate) {}
        open fun færreEllerLik16arbeidsdager(splitter: UtbetalingBuilder, dagen: LocalDate) {}
        open fun merEnn16arbeidsdager(splitter: UtbetalingBuilder, dagen: LocalDate) {}
        open fun sykHelgedag(splitter: UtbetalingBuilder, dagen: LocalDate) {}

        open fun entering(splitter: UtbetalingBuilder) {}
        open fun leaving(splitter: UtbetalingBuilder) {}

    }

    private object Initiell : UtbetalingState() {

        override fun entering(splitter: UtbetalingBuilder) {
            splitter.sykedager = 0
            splitter.ikkeSykedager = 0
            splitter.fridager = 0
        }

        override fun færreEllerLik16Sykedager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.setNåværendeInntekt(dagen.minusDays(1))
            splitter.håndterArbeidsgiverdag(dagen)
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun merEnn16Sykedager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun sykHelgedag(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsgiverdag(dagen)
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun fridag(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
        }

    }

    private object ArbeidsgiverperiodeSykedager : UtbetalingState() {

        override fun færreEllerLik16Sykedager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun merEnn16Sykedager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.state(UtbetalingSykedager).also { splitter.håndterNAVdag(dagen) }
        }

        override fun sykHelgedag(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun færreEllerLik16arbeidsdager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            splitter.state(ArbeidsgiverperiodeOpphold)
        }

        override fun merEnn16arbeidsdager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun fridag(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.fridager = 0
            splitter.håndterFridag(dagen)
            splitter.state(ArbeidsgiverperiodeFri)
        }
    }

    private object ArbeidsgiverperiodeFri : UtbetalingState() {
        override fun fridag(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
        }

        override fun færreEllerLik16Sykedager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.sykedager += splitter.fridager
            if (splitter.sykedager >= 16) splitter.state(UtbetalingSykedager)
                .also { splitter.håndterNAVdag(dagen) }
            else splitter.state(ArbeidsgiverperiodeSykedager)
                .also { splitter.håndterArbeidsgiverdag(dagen) }
        }

        override fun færreEllerLik16arbeidsdager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            splitter.ikkeSykedager = (if (splitter.sykedager >= 16) 0 else splitter.fridager) + 1
            splitter.state(if (splitter.ikkeSykedager >= 16) Initiell else ArbeidsgiverperiodeOpphold)
        }

        override fun merEnn16arbeidsdager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }


        override fun merEnn16Sykedager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.state(UtbetalingSykedager).also { splitter.håndterNAVdag(dagen) }
        }
    }

    private object ArbeidsgiverperiodeOpphold : UtbetalingState() {
        override fun entering(splitter: UtbetalingBuilder) {
            splitter.ikkeSykedager = 0
        }

        override fun færreEllerLik16arbeidsdager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
        }

        override fun merEnn16Sykedager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterNAVdag(dagen)
            splitter.state(UtbetalingSykedager)
        }

        override fun færreEllerLik16Sykedager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsgiverdag(dagen)
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun fridag(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
        }
    }

    private object UtbetalingSykedager : UtbetalingState() {
        override fun entering(splitter: UtbetalingBuilder) {
            splitter.ikkeSykedager = 0
        }

        override fun sykHelgedag(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterNAVHelgedag(dagen)
        }

        override fun merEnn16Sykedager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterNAVdag(dagen)
        }

        override fun færreEllerLik16Sykedager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun færreEllerLik16arbeidsdager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            splitter.state(UtbetalingOpphold)
        }

        override fun merEnn16arbeidsdager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun fridag(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
            splitter.state(UtbetalingFri)
        }
    }

    private object UtbetalingFri : UtbetalingState() {
        override fun entering(splitter: UtbetalingBuilder) {
            splitter.fridager = 1
        }

        override fun færreEllerLik16Sykedager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun merEnn16Sykedager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterNAVdag(dagen)
            splitter.state(UtbetalingSykedager)
        }

        override fun fridag(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
        }

        override fun færreEllerLik16arbeidsdager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            splitter.ikkeSykedager = splitter.fridager + 1
            splitter.state(if (splitter.ikkeSykedager >= 16) Initiell else UtbetalingOpphold)
        }

        override fun merEnn16arbeidsdager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }
    }

    private object UtbetalingOpphold : UtbetalingBuilder.UtbetalingState() {
        override fun merEnn16Sykedager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterNAVdag(dagen)
            splitter.state(UtbetalingSykedager)
        }

        override fun færreEllerLik16Sykedager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun færreEllerLik16arbeidsdager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            if (splitter.ikkeSykedager == 16) splitter.state(Initiell)
        }

        override fun merEnn16arbeidsdager(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
            splitter.state(Initiell)
        }

        override fun fridag(splitter: UtbetalingBuilder, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
            splitter.ikkeSykedager += 1
        }
    }

    private object Ugyldig : UtbetalingState()

}
