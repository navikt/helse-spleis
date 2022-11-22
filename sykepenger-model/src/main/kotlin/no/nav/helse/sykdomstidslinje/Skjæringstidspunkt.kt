package no.nav.helse.sykdomstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.økonomi.Økonomi

internal fun Sykdomstidslinje.skjæringstidspunkt(periode: Periode) =
    Skjæringstidspunkt(periode, this).result()
internal fun Sykdomstidslinje.sisteSkjæringstidspunkt() =
    periode()?.let { skjæringstidspunkt(it) }

// Finner skjæringstidspunkt ved å ta utgangspunkt i siste dag i Periode, og går bakover på sykdomstidslinjen.
internal class Skjæringstidspunkt(private val søkeperiode: Periode, sykdomstidslinje: Sykdomstidslinje) : SykdomstidslinjeVisitor {
    private var tilstand: Tilstand = HarIkkeBegynt
    private var forrigeSkjæringstidspunkt: LocalDate? = null

    init {
        sykdomstidslinje.fremTilOgMed(søkeperiode.endInclusive).acceptReversed(this)
    }

    internal fun result() = forrigeSkjæringstidspunkt

    private interface Tilstand {
        fun oppholdsdag(skjæringstidspunkt: Skjæringstidspunkt, dato: LocalDate)
        fun feriedag(skjæringstidspunkt: Skjæringstidspunkt, dato: LocalDate)
        fun ikkeOppholdsdag(skjæringstidspunkt: Skjæringstidspunkt, dato: LocalDate)
    }

    private object HarIkkeBegynt : Tilstand {
        override fun oppholdsdag(skjæringstidspunkt: Skjæringstidspunkt, dato: LocalDate) {
            if (dato > skjæringstidspunkt.søkeperiode.start) return
            // avbryter søket dersom vi ikke har funnet noenting før perioden er over
            skjæringstidspunkt.tilstand = Ferdig
        }

        override fun feriedag(skjæringstidspunkt: Skjæringstidspunkt, dato: LocalDate) {
            // ferie på slutten har egentlig ingenting å si,
            // vi må uansett finne en sykedag
        }

        override fun ikkeOppholdsdag(skjæringstidspunkt: Skjæringstidspunkt, dato: LocalDate) {
            skjæringstidspunkt.forrigeSkjæringstidspunkt = dato
            if (dato.erHelg()) return
            skjæringstidspunkt.tilstand = HarBegynt
        }
    }

    private object HarBegynt : Tilstand {
        override fun oppholdsdag(skjæringstidspunkt: Skjæringstidspunkt, dato: LocalDate) {
            skjæringstidspunkt.tilstand = Ferdig
        }

        override fun feriedag(skjæringstidspunkt: Skjæringstidspunkt, dato: LocalDate) {
            // ferien kan være gyldig opphold, men bare dersom vi finner en sykedag før ferien
        }

        override fun ikkeOppholdsdag(skjæringstidspunkt: Skjæringstidspunkt, dato: LocalDate) {
            skjæringstidspunkt.forrigeSkjæringstidspunkt = dato
        }
    }

    private object Ferdig : Tilstand {
        override fun oppholdsdag(skjæringstidspunkt: Skjæringstidspunkt, dato: LocalDate) {}
        override fun feriedag(skjæringstidspunkt: Skjæringstidspunkt, dato: LocalDate) {}
        override fun ikkeOppholdsdag(skjæringstidspunkt: Skjæringstidspunkt, dato: LocalDate) {}
    }

    override fun visitDag(dag: Dag.UkjentDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        if (dato.erHelg()) return // helg er som oftest ok gap
        tilstand.oppholdsdag(this, dato)
    }

    override fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        tilstand.oppholdsdag(this, dato)
    }

    override fun visitDag(
        dag: Dag.Arbeidsgiverdag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        tilstand.ikkeOppholdsdag(this, dato)
    }

    override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        tilstand.feriedag(this, dato)
    }

    override fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        tilstand.oppholdsdag(this, dato)
    }

    override fun visitDag(
        dag: Dag.ArbeidsgiverHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        tilstand.ikkeOppholdsdag(this, dato)
    }

    override fun visitDag(
        dag: Dag.Sykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        tilstand.ikkeOppholdsdag(this, dato)
    }

    override fun visitDag(
        dag: Dag.ForeldetSykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        tilstand.ikkeOppholdsdag(this, dato)
    }

    override fun visitDag(
        dag: Dag.SykHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        tilstand.ikkeOppholdsdag(this, dato)
    }

    override fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        tilstand.feriedag(this, dato)
    }

    override fun visitDag(
        dag: Dag.ProblemDag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde,
        other: SykdomstidslinjeHendelse.Hendelseskilde?,
        melding: String
    ) {
        tilstand.oppholdsdag(this, dato)
    }
}