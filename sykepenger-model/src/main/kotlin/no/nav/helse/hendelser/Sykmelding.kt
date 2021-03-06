package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.Companion.noOverlap
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.økonomi.Prosentdel
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class Sykmelding(
    meldingsreferanseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    sykeperioder: List<Sykmeldingsperiode>,
    private val sykmeldingSkrevet: LocalDateTime,
    private val mottatt: LocalDateTime
) : SykdomstidslinjeHendelse(meldingsreferanseId, sykmeldingSkrevet) {

    private val sykdomstidslinje: Sykdomstidslinje
    private val periode: Periode

    init {
        if (sykeperioder.isEmpty()) severe("Ingen sykeperioder")
        sykdomstidslinje = Sykmeldingsperiode.tidslinje(this, sykeperioder)
        periode = requireNotNull(sykdomstidslinje.periode())
    }

    override fun valider(periode: Periode): IAktivitetslogg {
        forGammel()
        return this
    }

    override fun forGammel() = (periode.endInclusive < mottatt.toLocalDate().minusMonths(6)).also {
        if (it) error("Søknadsperioden kan ikke være eldre enn 6 måneder fra mottattidspunkt")
    }

    override fun melding(klassName: String) = "Sykmelding"

    override fun sykdomstidslinje() = sykdomstidslinje

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun aktørId() = aktørId

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    internal fun overlappIkkeStøttet(other: Periode) {
        val slutterFør = this.periode.start < other.start
        val slutterEtter = this.periode.endInclusive > other.endInclusive
        val starterSammeDag = this.periode.start == other.start
        val slutterSammeDag = this.periode.endInclusive == other.endInclusive
        val hvorfor = when {
            slutterFør -> when {
                slutterEtter -> "starter før og slutter etter vedtaksperioden"
                slutterSammeDag -> "starter før vedtaksperioden, slutter samme dag"
                else -> "starter før vedtaksperioden, slutter inni"
            }
            slutterEtter -> when {
                starterSammeDag -> "slutter etter vedtaksperioden, starter samme dag"
                else -> "slutter etter vedtaksperioden, starter inni"
            }
            this.periode != other -> when {
                starterSammeDag -> "perioden er inni vedtaksperioden (starter samme dag)"
                slutterSammeDag -> "perioden er inni vedtaksperioden (slutter samme dag)"
                else -> "perioden er inni vedtaksperioden (starter og slutter inni)"
            }
            else -> "nøyaktig samme periode som vedtaksperioden"
        }
        error("Mottatt overlappende sykmeldinger - $hvorfor")
    }
}

class Sykmeldingsperiode(
    private val fom: LocalDate,
    private val tom: LocalDate,
    private val grad: Prosentdel
) {
    internal fun tidslinje(kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        Sykdomstidslinje.sykedager(fom, tom, grad, kilde)

    internal companion object {
        internal fun tidslinje(sykmelding: Sykmelding, perioder: List<Sykmeldingsperiode>) =
            perioder.map { it.tidslinje(sykmelding.kilde) }
                .merge(noOverlap)
                .also { tidslinje ->
                    if (tidslinje.any { it is Dag.ProblemDag }) sykmelding.severe("Sykeperioder overlapper")
                }

        internal fun periode(perioder: List<Sykmeldingsperiode>) =
            perioder.minOfOrNull { it.fom }?.let { fom ->
                fom til perioder.maxOf { it.tom }
            }
    }
}
