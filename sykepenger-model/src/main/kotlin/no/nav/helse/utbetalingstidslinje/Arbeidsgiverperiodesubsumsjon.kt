package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserAap
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserDagpenger
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserForeldrepenger
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserOmsorgspenger
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserOpplaringspenger
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserPleiepenger
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserSvangerskapspenger
import no.nav.helse.økonomi.Økonomi

internal class Arbeidsgiverperiodesubsumsjon(
    private val other: ArbeidsgiverperiodeMediator,
    private val subsumsjonObserver: SubsumsjonObserver?
) : ArbeidsgiverperiodeMediator by (other) {
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

    override fun foreldetDag(dato: LocalDate, økonomi: Økonomi) {
        tilstand.oppholdsdag(this, dato)
        other.foreldetDag(dato, økonomi)
    }

    override fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse, økonomi: Økonomi) {
        tilstand.avvistDag(this, dato, begrunnelse)
        other.avvistDag(dato, begrunnelse, økonomi)
    }

    private fun subsummerAndreYtelser(dato: LocalDate, begrunnelse: Begrunnelse) {
        if (begrunnelse == AndreYtelserAap) {
            subsumsjonObserver?.`§ 8-48 ledd 2 punktum 2`(dato, sykdomstidslinjesubsumsjon)
            return
        }
        if (begrunnelse !in AndreYtelser) return
        subsumsjonObserver?.`Trygderettens kjennelse 2006-4023`(dato, sykdomstidslinjesubsumsjon)
    }

    override fun fridag(dato: LocalDate) {
        subsumsjonObserver?.also { it.`§ 8-17 ledd 2`(dato, sykdomstidslinjesubsumsjon) }
        tilstand.oppholdsdag(this, dato)
        other.fridag(dato)
    }

    override fun fridagOppholdsdag(dato: LocalDate) {
        subsumsjonObserver?.also { it.`§ 8-17 ledd 2`(dato, sykdomstidslinjesubsumsjon) }
        tilstand.oppholdsdag(this, dato)
        other.fridagOppholdsdag(dato)
    }

    override fun arbeidsgiverperiodedag(
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
        tilstand.arbeidsgiverperiodedag(this, dato, økonomi)
        subsumsjonObserver?.also { it.`§ 8-17 ledd 1 bokstav a`(false, dagen = dato, sykdomstidslinjesubsumsjon) }
        subsumsjonObserver?.also { it.`§ 8-19 andre ledd`(dato, sykdomstidslinjesubsumsjon) }
        other.arbeidsgiverperiodedag(dato, økonomi, kilde)
    }

    override fun arbeidsgiverperiodedagNav(
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
        subsumsjonObserver?.also { it.`§ 8-17 ledd 1`(dato) }
        other.arbeidsgiverperiodedagNav(dato, økonomi, kilde)
    }

    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        // på første navdag etter fullført agp
        if (dato.erHelg()) subsumsjonObserver?.also { it.`§ 8-11 ledd 1`(dato) }
        else tilstand.utbetalingsdag(this, dato, økonomi)
        other.utbetalingsdag(dato, økonomi, kilde)
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
        fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {}
        fun utbetalingsdag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {}
        fun avvistDag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, begrunnelse: Begrunnelse) {}
    }
    private object Initiell : Tilstand {
        override fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {
            parent.tilstand = PåbegyntArbeidsgiverperiode
        }

        override fun avvistDag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, begrunnelse: Begrunnelse) {
            parent.subsummerAndreYtelser(dato, begrunnelse)
        }
    }

    private object Avbrutt : Tilstand {
        override fun oppholdsdag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate) {
            parent.subsumsjonObserver?.also { it.`§ 8-19 fjerde ledd`(dato, parent.sykdomstidslinjesubsumsjon) }
            parent.tilstand = Initiell
        }

        override fun avvistDag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, begrunnelse: Begrunnelse) {
            parent.subsummerAndreYtelser(dato, begrunnelse)
            oppholdsdag(parent, dato)
        }

        override fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {
            throw IllegalStateException("gir ikke mening")
        }

        override fun utbetalingsdag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {
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

        override fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {
            parent.subsumsjonObserver?.also { it.`§ 8-19 tredje ledd`(dato, parent.sykdomstidslinjesubsumsjon) }
            parent.tilstand = PåbegyntArbeidsgiverperiode
        }
    }

    private object SisteDagOgOppholdIPåbegyntArbeidsgiverperiode : Tilstand {
        override fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {
            parent.subsumsjonObserver?.also { it.`§ 8-19 første ledd`(dato, parent.sykdomstidslinjesubsumsjon) }
            parent.subsumsjonObserver?.also { it.`§ 8-19 tredje ledd`(dato, parent.sykdomstidslinjesubsumsjon) }
        }

        override fun utbetalingsdag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {
            parent.subsumsjonObserver?.also { it.`§ 8-17 ledd 1 bokstav a`(true, dagen = dato, parent.sykdomstidslinjesubsumsjon) }
            parent.tilstand = Initiell
        }
    }

    private object SisteDagIArbeidsgiverperioden : Tilstand {
        override fun arbeidsgiverperiodedag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {
            parent.subsumsjonObserver?.also { it.`§ 8-19 første ledd`(dato, parent.sykdomstidslinjesubsumsjon) }
        }

        override fun utbetalingsdag(parent: Arbeidsgiverperiodesubsumsjon, dato: LocalDate, økonomi: Økonomi) {
            parent.subsumsjonObserver?.also { it.`§ 8-17 ledd 1 bokstav a`(true, dagen = dato, parent.sykdomstidslinjesubsumsjon) }
            parent.tilstand = Initiell
        }
    }

    private companion object {
        private val AndreYtelser = setOf(
            AndreYtelserDagpenger, AndreYtelserForeldrepenger, AndreYtelserOmsorgspenger, AndreYtelserOpplaringspenger, AndreYtelserSvangerskapspenger, AndreYtelserPleiepenger
        )
    }
}
