package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.tournament.Dagturnering
import java.time.LocalDate
import java.util.*

class Sykmelding(
    meldingsreferanseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    sykeperioder: List<Triple<LocalDate, LocalDate, Int>>
) : SykdomstidslinjeHendelse(meldingsreferanseId) {

    private val sykeperioder: List<Sykeperiode>

    init {
        if (sykeperioder.isEmpty()) severe("Ingen sykeperioder")
        this.sykeperioder = sykeperioder
            .sortedBy { (fom, _, _) -> fom }
            .map { (fom, tom, grad) -> Sykeperiode(fom, tom, grad) }
        if (!ingenOverlappende()) severe("Sykeperioder overlapper")
    }

    override fun valider() = aktivitetslogg  // No invalid possibilities if passed init block

    override fun melding(klassName: String) = "Sykmelding"

    private fun ingenOverlappende() = sykeperioder.zipWithNext(Sykeperiode::ingenOverlappende).all { it }

    override fun sykdomstidslinje() = sykeperioder.map(Sykeperiode::sykdomstidslinje).merge(IdentiskDagTurnering)

    override fun sykdomstidslinje(tom: LocalDate) = sykdomstidslinje()

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun aktørId() = aktørId

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    private inner class Sykeperiode(
        fom: LocalDate,
        tom: LocalDate,
        private val sykdomsgrad: Int
    ) {
        private val periode = Periode(fom, tom)

        internal fun sykdomstidslinje() =
            Sykdomstidslinje.sykedager(periode, sykdomsgrad.toDouble(), SykmeldingDagFactory)

        internal fun ingenOverlappende(other: Sykeperiode) =
            !this.periode.overlapperMed(other.periode)
    }

    internal object SykmeldingDagFactory : DagFactory {
        override fun sykHelgedag(dato: LocalDate, grad: Double): SykHelgedag.Sykmelding =
            SykHelgedag.Sykmelding(dato, grad)

        override fun sykedag(dato: LocalDate, grad: Double): Sykedag.Sykmelding = Sykedag.Sykmelding(dato, grad)
    }

    private object IdentiskDagTurnering : Dagturnering {
        override fun beste(venstre: Dag, høyre: Dag): Dag {
            return when {
                venstre::class == høyre::class -> venstre
                venstre is ImplisittDag -> høyre
                høyre is ImplisittDag -> venstre
                else -> Ubestemtdag(venstre.dagen)
            }
        }
    }
}
