package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje

internal class Arbeidsgiverperiodesubsumsjon(
    private val other: Arbeidsgiverperiodeoppsamler,
    private val subsumsjonslogg: Subsumsjonslogg?
) : Arbeidsgiverperiodeoppsamler by (other) {
    private var sykdomstidslinje: Sykdomstidslinje = Sykdomstidslinje()
    private var tilstand: Tilstand = Initiell
    private val sykdomstidslinjesubsumsjon by lazy { sykdomstidslinje.subsumsjonsformat() }

    internal fun tidslinje(tidslinje: Sykdomstidslinje) {
        this.sykdomstidslinje = tidslinje
    }

    override fun arbeidsdag(dato: LocalDate) {
        tilstand.oppholdsdag(this, dato)
        other.arbeidsdag(dato)
    }

    override fun foreldetDag(dato: LocalDate) {
        tilstand.oppholdsdag(this, dato)
        other.foreldetDag(dato)
    }

    override fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse) {
        tilstand.avvistDag(this, dato, begrunnelse)
        other.avvistDag(dato, begrunnelse)
    }

    override fun fridag(dato: LocalDate) {
        tilstand.oppholdsdag(this, dato)
        other.fridag(dato)
    }

    override fun fridagOppholdsdag(dato: LocalDate) {
        tilstand.oppholdsdag(this, dato)
        other.fridagOppholdsdag(dato)
    }

    override fun arbeidsgiverperiodedag(dato: LocalDate) {
        tilstand.arbeidsgiverperiodedag(this, dato)
        other.arbeidsgiverperiodedag(dato)
    }

    override fun arbeidsgiverperiodedagNav(dato: LocalDate) {
        other.arbeidsgiverperiodedagNav(dato)
    }

    override fun utbetalingsdag(dato: LocalDate) {
        // på første navdag etter fullført agp
        if (!dato.erHelg()) tilstand.utbetalingsdag(this, dato)
        other.utbetalingsdag(dato)
    }

    override fun arbeidsgiverperiodeAvbrutt() {
        tilstand = Avbrutt
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
        fun oppholdsdag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate) {}
        fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate) {}
        fun utbetalingsdag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate) {}
        fun avvistDag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, begrunnelse: Begrunnelse) {}
    }
    private object Initiell : Tilstand {
        override fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate) {
            parent.tilstand = PåbegyntArbeidsgiverperiode
        }
    }

    private object Avbrutt : Tilstand {
        override fun oppholdsdag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate) {
            parent.subsumsjonslogg?.also { it.`§ 8-19 fjerde ledd`(dato, parent.sykdomstidslinjesubsumsjon) }
            parent.tilstand = Initiell
        }

        override fun avvistDag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, begrunnelse: Begrunnelse) {
            oppholdsdag(parent, dato)
        }

        override fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate) {
            throw IllegalStateException("gir ikke mening")
        }

        override fun utbetalingsdag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate) {
            throw IllegalStateException("gir ikke mening")
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

        override fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate) {
            parent.subsumsjonslogg?.also { it.`§ 8-19 tredje ledd`(dato, parent.sykdomstidslinjesubsumsjon) }
            parent.tilstand = PåbegyntArbeidsgiverperiode
        }
    }

    private object SisteDagOgOppholdIPåbegyntArbeidsgiverperiode : Tilstand {
        override fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate) {
            parent.subsumsjonslogg?.also { it.`§ 8-19 første ledd`(dato, parent.sykdomstidslinjesubsumsjon) }
            parent.subsumsjonslogg?.also { it.`§ 8-19 tredje ledd`(dato, parent.sykdomstidslinjesubsumsjon) }
        }

        override fun utbetalingsdag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate) {
            parent.subsumsjonslogg?.also { it.`§ 8-17 ledd 1 bokstav a`(true, dagen = dato, parent.sykdomstidslinjesubsumsjon) }
            parent.tilstand = Initiell
        }
    }

    private object SisteDagIArbeidsgiverperioden : Tilstand {
        override fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate) {
            parent.subsumsjonslogg?.also { it.`§ 8-19 første ledd`(dato, parent.sykdomstidslinjesubsumsjon) }
        }

        override fun utbetalingsdag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate) {
            parent.subsumsjonslogg?.also { it.`§ 8-17 ledd 1 bokstav a`(true, dagen = dato, parent.sykdomstidslinjesubsumsjon) }
            parent.tilstand = Initiell
        }
    }
}
