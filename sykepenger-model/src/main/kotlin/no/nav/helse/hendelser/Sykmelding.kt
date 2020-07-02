package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.Companion.noOverlap
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.merge
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class Sykmelding(
    meldingsreferanseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    sykeperioder: List<Sykmeldingsperiode>,
    private val mottatt: LocalDateTime
) : SykdomstidslinjeHendelse(meldingsreferanseId) {

    private val sykdomstidslinje: Sykdomstidslinje

    init {
        if (sykeperioder.isEmpty()) severe("Ingen sykeperioder")
        sykdomstidslinje = sykeperioder.map { (fom, tom, grad) ->
            Sykdomstidslinje.sykedager(fom, tom, grad, this.kilde)
        }
            .merge(noOverlap)
            .also { tidslinje ->
                if (tidslinje.any { it is Dag.ProblemDag }) severe("Sykeperioder overlapper")
            }
    }

    override fun periode() = Periode(fom = sykdomstidslinje.førsteDag(), tom = sykdomstidslinje.sisteDag())

    override fun valider(periode: Periode): Aktivitetslogg {
        forGammel()
        return aktivitetslogg
    }

    internal fun forGammel() = (sykdomstidslinje.førsteDag() < mottatt.toLocalDate().minusMonths(6)).also {
        if (it) error("Sykmelding kan ikke være eldre enn 6 måneder fra mottattdato")
    }

    override fun melding(klassName: String) = "Sykmelding"

    override fun sykdomstidslinje() = sykdomstidslinje

    override fun sykdomstidslinje(tom: LocalDate): Sykdomstidslinje = sykdomstidslinje

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun aktørId() = aktørId

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }
}

data class Sykmeldingsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Int
)
