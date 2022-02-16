package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.subsumsjonsformat
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

//TODO: Logge 8-19 fjerde ledd når vi teller neste arbeidsgiverperiode - BEREGNING
internal class Arbeidsgiverperiodesubsumsjon(
    private val other: ArbeidsgiverperiodeMediator,
    private val subsumsjonObserver: SubsumsjonObserver
) : ArbeidsgiverperiodeMediator by (other) {
    private var sykdomstidslinje: Sykdomstidslinje = Sykdomstidslinje()
    private var tilstand: Tilstand = Initiell

    internal fun tidslinje(tidslinje: Sykdomstidslinje) {
        this.sykdomstidslinje = tidslinje
    }

    override fun fridag(dato: LocalDate) {
        subsumsjonObserver.`§ 8-17 ledd 2`(dato, sykdomstidslinje.subsumsjonsformat())
        other.fridag(dato)
    }

    override fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi) {
        tilstand.arbeidsgiverperiodedag(this, dato, økonomi)
        subsumsjonObserver.`§ 8-17 ledd 1 bokstav a`(false, dagen = dato)
        subsumsjonObserver.`§ 8-19 andre ledd`(dato, sykdomstidslinje.subsumsjonsformat())
        other.arbeidsgiverperiodedag(dato, økonomi)
    }

    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi) {
        // på første navdag etter fullført agp
        if (dato.erHelg()) subsumsjonObserver.`§ 8-11 første ledd`(dato)
        else tilstand.utbetalingsdag(this, dato, økonomi)
        other.utbetalingsdag(dato, økonomi)
    }

    override fun arbeidsgiverperiodeAvbrutt() {
        tilstand = Initiell
        other.arbeidsgiverperiodeAvbrutt()
    }

    override fun arbeidsgiverperiodeSistedag() {
        tilstand.sisteDagIArbeidsgiverperioden(this)
        other.arbeidsgiverperiodeSistedag()
    }

    override fun oppholdsdag() {
        tilstand.oppholdsdag(this)
        other.oppholdsdag()
    }

    private interface Tilstand {
        fun oppholdsdag(parent: Arbeidsgiverperiodesubsumsjon) {}
        fun sisteDagIArbeidsgiverperioden(parent: Arbeidsgiverperiodesubsumsjon) {}
        fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {}
        fun utbetalingsdag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {}
    }
    private object Initiell : Tilstand {
        override fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {
            parent.tilstand = PåbegyntArbeidsgiverperiode
        }
    }
    private object PåbegyntArbeidsgiverperiode : Tilstand {
        override fun sisteDagIArbeidsgiverperioden(parent: Arbeidsgiverperiodesubsumsjon) {
            parent.tilstand = SisteDagIArbeidsgiverperioden
        }

        override fun oppholdsdag(parent: Arbeidsgiverperiodesubsumsjon) {
            parent.tilstand = OppholdIPåbegyntArbeidsgiverperiode
        }
    }

    private object OppholdIPåbegyntArbeidsgiverperiode : Tilstand {
        override fun sisteDagIArbeidsgiverperioden(parent: Arbeidsgiverperiodesubsumsjon) {
            parent.tilstand = SisteDagOgOppholdIPåbegyntArbeidsgiverperiode
        }

        override fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {
            parent.subsumsjonObserver.`§ 8-19 tredje ledd`(dato, parent.sykdomstidslinje.subsumsjonsformat())
            parent.tilstand = PåbegyntArbeidsgiverperiode
        }
    }

    private object SisteDagOgOppholdIPåbegyntArbeidsgiverperiode : Tilstand {
        override fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {
            parent.subsumsjonObserver.`§ 8-19 første ledd`(dato, parent.sykdomstidslinje.subsumsjonsformat())
            parent.subsumsjonObserver.`§ 8-19 tredje ledd`(dato, parent.sykdomstidslinje.subsumsjonsformat())
        }

        override fun utbetalingsdag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {
            parent.subsumsjonObserver.`§ 8-17 ledd 1 bokstav a`(true, dagen = dato)
            parent.tilstand = Initiell
        }
    }

    private object SisteDagIArbeidsgiverperioden : Tilstand {
        override fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {
            parent.subsumsjonObserver.`§ 8-19 første ledd`(dato, parent.sykdomstidslinje.subsumsjonsformat())
        }

        override fun utbetalingsdag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {
            parent.subsumsjonObserver.`§ 8-17 ledd 1 bokstav a`(true, dagen = dato)
            parent.tilstand = Initiell
        }
    }
}
