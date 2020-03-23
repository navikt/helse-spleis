package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 *  Forstår opprettelsen av en Utbetalingstidslinje
 */

internal class UtbetalingstidslinjeBuilder internal constructor(
    private val sykdomstidslinje: ConcreteSykdomstidslinje,
    private val sisteDag: LocalDate,
    private val inntekthistorikk: Inntekthistorikk,
    private val sisteNavDagForArbeidsgiverFørPerioden: LocalDate?,
    private val arbeidsgiverRegler: ArbeidsgiverRegler = NormalArbeidstaker
    ) : SykdomstidslinjeVisitor {
    private var state: UtbetalingState = if (sisteNavDagForArbeidsgiverFørPerioden != null)
        InitiellMedNavdagFørPerioden else Initiell

    private var sykedagerIArbeidsgiverperiode = 0
    private var ikkeSykedager = 0
    private var fridager = 0

    private var nåværendeInntekt = 0.00

    private val tidslinje = Utbetalingstidslinje()

    internal fun result(): Utbetalingstidslinje {
        (sykdomstidslinje.kutt(sisteDag))?.accept(this)
        return tidslinje
    }

    override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag.Søknad) = fridag(permisjonsdag.dagen)
    override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag.Aareg) = fridag(permisjonsdag.dagen)
    override fun visitStudiedag(studiedag: Studiedag) = implisittDag(studiedag.dagen)
    override fun visitUbestemt(ubestemtdag: Ubestemtdag) = implisittDag(ubestemtdag.dagen)
    override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) = implisittDag(utenlandsdag.dagen)
    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag.Inntektsmelding) = arbeidsdag(arbeidsdag.dagen)
    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag.Søknad) = arbeidsdag(arbeidsdag.dagen)
    override fun visitImplisittDag(implisittDag: ImplisittDag) = implisittDag(implisittDag.dagen)
    override fun visitFeriedag(feriedag: Feriedag.Inntektsmelding) = fridag(feriedag.dagen)
    override fun visitFeriedag(feriedag: Feriedag.Søknad) = fridag(feriedag.dagen)
    override fun visitSykedag(sykedag: Sykedag.Sykmelding) = sykedag(sykedag.dagen, sykedag.grad)
    override fun visitSykedag(sykedag: Sykedag.Søknad) = sykedag(sykedag.dagen, sykedag.grad)
    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag.Inntektsmelding) = egenmeldingsdag(egenmeldingsdag.dagen)
    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag.Søknad) = egenmeldingsdag(egenmeldingsdag.dagen)
    override fun visitSykHelgedag(sykHelgedag: SykHelgedag.Søknad) = sykHelgedag(sykHelgedag.dagen, sykHelgedag.grad)
    override fun visitSykHelgedag(sykHelgedag: SykHelgedag.Sykmelding) = sykHelgedag(sykHelgedag.dagen, sykHelgedag.grad)
    override fun visitKunArbeidsgiverSykedag(sykedag: KunArbeidsgiverSykedag) = kunArbeidsgiverSykedag(sykedag.dagen)

    private fun kunArbeidsgiverSykedag(dagen: LocalDate) {
        if (arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(sykedagerIArbeidsgiverperiode) ||
                sisteNavDagForArbeidsgiverFørPerioden?.let {
                    (ChronoUnit.DAYS.between(it, dagen) <= 16)
                } == true) {
            state = UtbetalingSykedager
            tidslinje.addForeldetDag(dagen)
        }
        else state.sykedagerIArbeidsgiverperioden(this, dagen, Double.NaN)
    }

    private fun egenmeldingsdag(dagen: LocalDate) =
        if (arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(sykedagerIArbeidsgiverperiode) ||
                sisteNavDagForArbeidsgiverFørPerioden?.let {
                    (ChronoUnit.DAYS.between(it, dagen) <= 16)
                } == true)
            tidslinje.addAvvistDag(Double.NaN, dagen, Double.NaN, Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode)
        else state.egenmeldingsdagIArbeidsgiverperioden(this, dagen)

    private fun implisittDag(dagen: LocalDate) = if (dagen.erHelg()) fridag(dagen) else arbeidsdag(dagen)

    private fun sykedag(dagen: LocalDate, grad: Double) =
        if (arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(sykedagerIArbeidsgiverperiode))
            state.sykedagerEtterArbeidsgiverperioden(this, dagen, grad)
        else
            state.sykedagerIArbeidsgiverperioden(this, dagen, grad)

    private fun sykHelgedag(dagen: LocalDate, grad: Double) = state.sykHelgedag(this, dagen, grad)

    private fun arbeidsdag(dagen: LocalDate) =
        if (arbeidsgiverRegler.burdeStarteNyArbeidsgiverperiode(ikkeSykedager))
            state.arbeidsdagerEtterOppholdsdager(this, dagen)
        else
            state.arbeidsdagerIOppholdsdager(this, dagen)

    private fun fridag(dagen: LocalDate) {
        state.fridag(this, dagen)
    }

    private fun setNåværendeInntekt(dagen: LocalDate) {
        nåværendeInntekt = inntekthistorikk.inntekt(dagen)
            ?.multiply(arbeidsgiverRegler.prosentLønn().toBigDecimal())
            ?.multiply(12.toBigDecimal())
            ?.divide(260.toBigDecimal(), RoundingMode.HALF_UP)
            ?.toDouble()
            ?: Double.NaN
    }

    private fun addArbeidsgiverdag(dagen: LocalDate) {
        tidslinje.addArbeidsgiverperiodedag(nåværendeInntekt, dagen)
    }

    private fun håndterArbeidsgiverdag(dagen: LocalDate) {
        sykedagerIArbeidsgiverperiode += 1
        addArbeidsgiverdag(dagen)
    }

    private fun håndterNAVdag(dagen: LocalDate, grad: Double) {
        tidslinje.addNAVdag(nåværendeInntekt, dagen, grad)
    }

    private fun håndterNAVHelgedag(dagen: LocalDate, grad: Double) {
        tidslinje.addHelg(0.0, dagen, grad)
    }

    private fun håndterArbeidsdag(dagen: LocalDate) {
        inkrementerIkkeSykedager()
        setNåværendeInntekt(dagen)
        tidslinje.addArbeidsdag(nåværendeInntekt, dagen)
    }

    private fun inkrementerIkkeSykedager() {
        ikkeSykedager += 1
        if (arbeidsgiverRegler.burdeStarteNyArbeidsgiverperiode(ikkeSykedager)) state(Initiell)
    }

    private fun håndterFridag(dagen: LocalDate) {
        fridager += 1
        tidslinje.addFridag(nåværendeInntekt, dagen)
    }

    private fun håndterFriEgenmeldingsdag(dagen: LocalDate) {
        sykedagerIArbeidsgiverperiode += fridager
        tidslinje.addAvvistDag(Double.NaN, dagen, Double.NaN, Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode)
        if (arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(sykedagerIArbeidsgiverperiode))
            state(UtbetalingSykedager)
        else
            state(ArbeidsgiverperiodeSykedager)
    }

    private fun state(state: UtbetalingState) {
        this.state.leaving(this)
        this.state = state
        this.state.entering(this)
    }

    private abstract class UtbetalingState {
        open fun sykedagerIArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {}
        open fun egenmeldingsdagIArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            sykedagerIArbeidsgiverperioden(splitter, dagen, Double.NaN)
        }
        open fun sykedagerEtterArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {}

        open fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {}
        open fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {}
        open fun arbeidsdagerEtterOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {}
        open fun sykHelgedag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {}

        open fun entering(splitter: UtbetalingstidslinjeBuilder) {}
        open fun leaving(splitter: UtbetalingstidslinjeBuilder) {}

    }

    private object InitiellMedNavdagFørPerioden : UtbetalingState() {

        override fun sykedagerIArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.setNåværendeInntekt(dagen.minusDays(1))
            if (ChronoUnit.DAYS.between(splitter.sisteNavDagForArbeidsgiverFørPerioden!!, dagen) <= 16) {
                splitter.håndterNAVdag(dagen, grad)
                splitter.state(UtbetalingSykedager)
                return
            }
            splitter.håndterArbeidsgiverdag(dagen)
            splitter.state(ArbeidsgiverperiodeSykedager)
        }
    }

    private object Initiell : UtbetalingState() {

        override fun entering(splitter: UtbetalingstidslinjeBuilder) {
            splitter.sykedagerIArbeidsgiverperiode = 0
            splitter.ikkeSykedager = 0
            splitter.fridager = 0
        }

        override fun sykedagerIArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.setNåværendeInntekt(dagen.minusDays(1))
            splitter.håndterArbeidsgiverdag(dagen)
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun sykedagerEtterArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.setNåværendeInntekt(dagen.minusDays(1))
            splitter.håndterNAVdag(dagen, grad)
            splitter.state(UtbetalingSykedager)
        }

        override fun sykHelgedag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.setNåværendeInntekt(dagen.minusDays(1))
            splitter.håndterArbeidsgiverdag(dagen)
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
        }

        override fun arbeidsdagerIOppholdsdager(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterArbeidsdag(dagen)
        }
    }

    private object ArbeidsgiverperiodeSykedager : UtbetalingState() {

        override fun sykedagerIArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.håndterArbeidsgiverdag(dagen)
        }

        override fun sykedagerEtterArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.state(UtbetalingSykedager).also { splitter.håndterNAVdag(dagen, grad) }
        }

        override fun sykHelgedag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
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

    private object ArbeidsgiverperiodeFri : UtbetalingState() {
        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterFridag(dagen)
        }

        override fun egenmeldingsdagIArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.håndterFriEgenmeldingsdag(dagen)
        }

        override fun sykedagerIArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.sykedagerIArbeidsgiverperiode += splitter.fridager
            if (splitter.arbeidsgiverRegler.arbeidsgiverperiodenGjennomført(splitter.sykedagerIArbeidsgiverperiode)) splitter.state(
                    UtbetalingSykedager
                )
                .also { splitter.håndterNAVdag(dagen, grad) }
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


        override fun sykedagerEtterArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.state(UtbetalingSykedager).also { splitter.håndterNAVdag(dagen, grad) }
        }
    }

    private object ArbeidsgiverperiodeOpphold : UtbetalingState() {
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

        override fun sykedagerEtterArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.håndterNAVdag(dagen, grad)
            splitter.state(UtbetalingSykedager)
        }

        override fun sykedagerIArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.håndterArbeidsgiverdag(dagen)
            splitter.state(ArbeidsgiverperiodeSykedager)
        }

        override fun fridag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate) {
            splitter.inkrementerIkkeSykedager()
            splitter.håndterFridag(dagen)
        }

        override fun sykHelgedag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.inkrementerIkkeSykedager()
            splitter.håndterFridag(dagen)
        }
    }

    private object UtbetalingSykedager : UtbetalingState() {
        override fun entering(splitter: UtbetalingstidslinjeBuilder) {
            splitter.ikkeSykedager = 0
        }

        override fun sykHelgedag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.håndterNAVHelgedag(dagen, grad)
        }

        override fun sykedagerEtterArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.håndterNAVdag(dagen, grad)
        }

        override fun sykedagerIArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
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

    private object UtbetalingFri : UtbetalingState() {
        override fun entering(splitter: UtbetalingstidslinjeBuilder) {
            splitter.fridager = 1
        }

        override fun sykedagerIArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.state(Ugyldig)
        }

        override fun sykedagerEtterArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.håndterNAVdag(dagen, grad)
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

        override fun sykHelgedag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.håndterNAVHelgedag(dagen, grad)
        }
    }

    private object UtbetalingOpphold : UtbetalingstidslinjeBuilder.UtbetalingState() {
        override fun sykedagerEtterArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.håndterNAVdag(dagen, grad)
            splitter.state(UtbetalingSykedager)
        }

        override fun sykedagerIArbeidsgiverperioden(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
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

        override fun sykHelgedag(splitter: UtbetalingstidslinjeBuilder, dagen: LocalDate, grad: Double) {
            splitter.håndterFridag(dagen)
            splitter.inkrementerIkkeSykedager()
        }
    }

    private object Ugyldig : UtbetalingState()

}
