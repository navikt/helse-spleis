package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate

internal class SyketilfelleSplitter(private val dagsats: BigDecimal) : SykdomstidslinjeVisitor {

    private var state: BetalingsState = ArbeidsgiverperiodeInitiell
    private val betalingslinjer = mutableListOf<Betalingslinje>()

    private var sykedager = 0
    private var ikkeSykedager = 0
    private var ubestemteDager = 0

    fun results(): List<Betalingslinje> {
        require(state != Ugyldig)
        return betalingslinjer.toList()
    }

    private fun state(state: BetalingsState) {
        this.state.leaving(this)
        this.state = state
        this.state.entering(this)
    }

    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) = arbeidsdag(arbeidsdag.dagen)
    override fun visitImplisittDag(implisittDag: ImplisittDag) = if (implisittDag.dagen.isWeekend()) state.ubestemt(this, implisittDag.dagen) else arbeidsdag(implisittDag.dagen)

    override fun visitFeriedag(feriedag: Feriedag) = state.ubestemt(this, feriedag.dagen)
    override fun visitSykedag(sykedag: Sykedag) = sykedag(sykedag.dagen)
    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) = sykedag(egenmeldingsdag.dagen)
    override fun visitSykHelgedag(sykHelgedag: SykHelgedag) = sykedag(sykHelgedag.dagen)

    private fun arbeidsdag(dagen: LocalDate) {
        if (ikkeSykedager < 16) state.færreEnn16arbeidsdager(this, dagen) else state.merEnn15arbeidsdager(this, dagen)
    }

    private fun sykedag(dagen: LocalDate) {
        if (sykedager < 16) state.færreEnn16Sykedager(this, dagen) else state.merEnn15Sykedager(this, dagen)
    }

    private fun opprettBetalingslinje(dagen: LocalDate) {
        Betalingslinje(dagen, dagsats).apply { betalingslinjer.add(this) }
    }

    override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) {
        state(Ugyldig)
    }

    override fun visitUbestemt(ubestemtdag: Ubestemtdag) {
        state(Ugyldig)
    }

    override fun visitStudiedag(studiedag: Studiedag) {
        state(Ugyldig)
    }

    override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag) {
        state(Ugyldig)
    }

    abstract class BetalingsState {
        internal val nyttSyketilfelleGrense = 16

        open fun færreEnn16Sykedager(splitter: SyketilfelleSplitter, dagen: LocalDate) {}
        open fun merEnn15Sykedager(splitter: SyketilfelleSplitter, dagen: LocalDate) {}
        open fun ubestemt(splitter: SyketilfelleSplitter, dagen: LocalDate) {}
        open fun færreEnn16arbeidsdager(splitter: SyketilfelleSplitter, dagen: LocalDate) {}
        open fun merEnn15arbeidsdager(splitter: SyketilfelleSplitter, dagen: LocalDate) {}

        open fun entering(splitter: SyketilfelleSplitter) {}
        open fun leaving(splitter: SyketilfelleSplitter) {}
    }

    private object ArbeidsgiverperiodeInitiell : BetalingsState() {
        override fun entering(splitter: SyketilfelleSplitter) {
            splitter.sykedager = 0
            splitter.ikkeSykedager = 0
            splitter.ubestemteDager = 0
        }

        override fun færreEnn16Sykedager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.sykedager = 1
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun merEnn15Sykedager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }
    }

    private object ArbeidsgiverperiodeSykedager : BetalingsState() {
        override fun færreEnn16Sykedager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.sykedager += 1
        }

        override fun merEnn15Sykedager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.state(SykBetaling).also { splitter.opprettBetalingslinje(dagen) }
        }

        override fun ubestemt(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.ubestemteDager = 1
            splitter.state(ArbeidsgiverperiodeHelgOgFerie)
        }

        override fun færreEnn16arbeidsdager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.ikkeSykedager = 1
            splitter.state(ArbeidsgiverperiodeGap)
        }

        override fun merEnn15arbeidsdager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }
    }

    private object ArbeidsgiverperiodeGap : BetalingsState() {
        override fun ubestemt(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.ikkeSykedager += 1
            if (splitter.ikkeSykedager == 16) splitter.state(ArbeidsgiverperiodeInitiell)
        }

        override fun færreEnn16arbeidsdager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.ikkeSykedager += 1
        }

        override fun merEnn15arbeidsdager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.state(ArbeidsgiverperiodeInitiell)
        }

        override fun færreEnn16Sykedager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.sykedager += 1
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun merEnn15Sykedager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.opprettBetalingslinje(dagen)
            splitter.state(SykBetaling)
        }
    }

    private object ArbeidsgiverperiodeHelgOgFerie : BetalingsState() {
        override fun ubestemt(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.ubestemteDager += 1
        }

        override fun færreEnn16arbeidsdager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.ikkeSykedager = (if (splitter.sykedager >= 16) 0 else splitter.ubestemteDager) + 1
            if (splitter.ikkeSykedager >= 16) splitter.state(ArbeidsgiverperiodeInitiell) else splitter.state =
                ArbeidsgiverperiodeGap
        }

        override fun merEnn15arbeidsdager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun færreEnn16Sykedager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.sykedager += splitter.ubestemteDager + 1
            if (splitter.sykedager >= 16) splitter.state =
                SykBetaling.also { splitter.opprettBetalingslinje(dagen) }
            else splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun merEnn15Sykedager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.state(SykBetaling).also { splitter.opprettBetalingslinje(dagen) }
        }
    }

    private object SykBetaling : BetalingsState() {
        override fun entering(splitter: SyketilfelleSplitter) {
            splitter.ikkeSykedager = 0
        }

        override fun merEnn15Sykedager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.betalingslinjer.last().tom = dagen
        }

        override fun færreEnn16Sykedager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun færreEnn16arbeidsdager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.ikkeSykedager = 1
            splitter.state(BetalingGap)
        }

        override fun merEnn15arbeidsdager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun ubestemt(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.state(BetalingHelgOgFerie)
        }
    }

    private object BetalingHelgOgFerie: BetalingsState() {
        override fun entering(splitter: SyketilfelleSplitter) {
            splitter.ubestemteDager = 1
        }

        override fun færreEnn16Sykedager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun merEnn15Sykedager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.opprettBetalingslinje(dagen)
            splitter.state(SykBetaling)
        }

        override fun ubestemt(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.ubestemteDager += 1
        }

        override fun færreEnn16arbeidsdager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.ikkeSykedager = splitter.ubestemteDager + 1
            if (splitter.ikkeSykedager >= 16) splitter.state(Ugyldig) else splitter.state =
                BetalingGap
        }

        override fun merEnn15arbeidsdager(splitter: SyketilfelleSplitter, dagen: LocalDate) {

        }
    }

    private object BetalingGap: BetalingsState() {
        override fun merEnn15Sykedager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.opprettBetalingslinje(dagen)
            splitter.state(SykBetaling)
        }

        override fun færreEnn16Sykedager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun færreEnn16arbeidsdager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.ikkeSykedager += 1
        }

        override fun merEnn15arbeidsdager(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.state(Ugyldig)
        }

        override fun ubestemt(splitter: SyketilfelleSplitter, dagen: LocalDate) {
            splitter.ikkeSykedager += 1
            if (splitter.ikkeSykedager == 16) splitter.state(Ugyldig)
        }
    }

    private object Ugyldig : BetalingsState()
}

private fun LocalDate.isWeekend(): Boolean {
    return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
}

