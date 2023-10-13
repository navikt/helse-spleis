package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde
import no.nav.helse.økonomi.Økonomi

internal class Infotrygddekoratør(
    private val teller: Arbeidsgiverperiodeteller,
    private val other: SykdomstidslinjeVisitor,
    private val betalteDager: List<Periode>
) : SykdomstidslinjeVisitor by(other) {
    override fun visitDag(dag: Dag.Sykedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        fullførArbeidsgiverperiode(dato)
        other.visitDag(dag, dato, økonomi, kilde)
    }
    override fun visitDag(dag: Dag.SykHelgedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        fullførArbeidsgiverperiode(dato)
        other.visitDag(dag, dato, økonomi, kilde)
    }

    private fun fullførArbeidsgiverperiode(dato: LocalDate) {
        if (betalteDager.any { dato in it }) teller.fullfør()
    }
}
