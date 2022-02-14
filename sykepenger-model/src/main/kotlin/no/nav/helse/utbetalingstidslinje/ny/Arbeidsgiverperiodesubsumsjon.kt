package no.nav.helse.utbetalingstidslinje.ny

import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.subsumsjonsformat
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

//TODO: Logge 8-19 første ledd når går ut av første arbeidsgiverperiode - BEREGNING
//TODO: Logge 8-19 andre ledd for hver dag i arbeidsgiverperioden - BEREGNING
//TODO: Logge 8-19 tredje ledd når vi teller første arbeidsgiverperiode - BEREGNING
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

    private interface Tilstand {
        fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {}
        fun utbetalingsdag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {}
    }
    private object Initiell : Tilstand {
        override fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {
            parent.tilstand = PåbegyntArbeidsgiverperiode
        }
    }
    private object PåbegyntArbeidsgiverperiode : Tilstand {
        override fun utbetalingsdag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {
            parent.subsumsjonObserver.`§ 8-17 ledd 1 bokstav a`(true, dagen = dato)
            parent.tilstand = Initiell
        }
    }
}
