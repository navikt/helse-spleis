package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.Companion.noOverlap
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.økonomi.Prosentdel

class Sykmelding(
    meldingsreferanseId: UUID,
    fnr: String,
    aktørId: String,
    orgnummer: String,
    sykeperioder: List<Sykmeldingsperiode>,
    private val sykmeldingSkrevet: LocalDateTime,
    private val mottatt: LocalDateTime,
    private val erFremtidig: Boolean = false
) : SykdomstidslinjeHendelse(meldingsreferanseId, fnr, aktørId, orgnummer, sykmeldingSkrevet) {

    private val sykdomstidslinje: Sykdomstidslinje
    private val periode: Periode

    internal companion object {
        const val ERRORTEKST_FOR_GAMMEL = "Søknadsperioden kan ikke være eldre enn 6 måneder fra mottattidspunkt"
    }

    init {
        if (sykeperioder.isEmpty()) severe("Ingen sykeperioder")
        sykdomstidslinje = Sykmeldingsperiode.tidslinje(this, sykeperioder)
        periode = requireNotNull(sykdomstidslinje.periode())
    }

    override fun valider(periode: Periode, subsumsjonObserver: SubsumsjonObserver): IAktivitetslogg {
        validerAtSykmeldingIkkeErForGammel()
        return this
    }

    internal fun validerAtSykmeldingIkkeErForGammel() {
        if (periode.endInclusive < mottatt.toLocalDate().minusMonths(6)) error(ERRORTEKST_FOR_GAMMEL)
    }

    internal fun nyVedtaksperiode() {
        info("Lager ny vedtaksperiode fra ${if (erFremtidig) "Fremtidig" else "Ny"} søknad")
    }

    override fun sykdomstidslinje() = sykdomstidslinje

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

    override fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) {
        hendelseIder.add(Dokumentsporing.sykmelding(meldingsreferanseId()))
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
