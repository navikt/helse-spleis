package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.NyDag
import no.nav.helse.sykdomstidslinje.NyDag.Companion.noOverlap
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.merge
import java.time.LocalDate
import java.util.*

class Sykmelding(
    meldingsreferanseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    sykeperioder: List<Triple<LocalDate, LocalDate, Int>>
) : SykdomstidslinjeHendelse(meldingsreferanseId) {

    private val nySykdomstidslinje: NySykdomstidslinje

    init {
        if (sykeperioder.isEmpty()) severe("Ingen sykeperioder")
        nySykdomstidslinje = sykeperioder.map { (fom, tom, grad) ->
            NySykdomstidslinje.sykedager(fom, tom, grad, this.kilde)
        }
            .merge(noOverlap)
            .also { tidslinje ->
                if (tidslinje.any { it is NyDag.ProblemDag }) severe("Sykeperioder overlapper")
            }
    }

    override fun valider(periode: Periode) = aktivitetslogg  // No invalid possibilities if passed init block

    override fun melding(klassName: String) = "Sykmelding"

    override fun nySykdomstidslinje() = nySykdomstidslinje

    override fun nySykdomstidslinje(tom: LocalDate): NySykdomstidslinje = nySykdomstidslinje

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun aktørId() = aktørId

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }
}
