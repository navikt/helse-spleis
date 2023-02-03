package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Personopplysninger
import no.nav.helse.etterlevelse.SubsumsjonObserver
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
    personopplysninger: Personopplysninger,
    orgnummer: String,
    sykeperioder: List<Sykmeldingsperiode>,
    sykmeldingSkrevet: LocalDateTime
) : SykdomstidslinjeHendelse(meldingsreferanseId, fnr, aktørId, orgnummer, sykmeldingSkrevet, personopplysninger = personopplysninger) {

    private val sykdomstidslinje: Sykdomstidslinje
    private val periode: Periode

    init {
        if (sykeperioder.isEmpty()) logiskFeil("Ingen sykeperioder")
        sykdomstidslinje = Sykmeldingsperiode.tidslinje(this, sykeperioder)
        periode = requireNotNull(sykdomstidslinje.periode())
    }

    override fun valider(periode: Periode, subsumsjonObserver: SubsumsjonObserver) = this

    override fun sykdomstidslinje() = sykdomstidslinje

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    override fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) {
        hendelseIder.add(Dokumentsporing.sykmelding(meldingsreferanseId()))
    }

    internal fun oppdaterSykmeldingsperioder(perioder: List<Periode>): List<Periode> {
        if (trimmetForbi()) {
            info("Sykmeldingsperiode har allerede blitt tidligere håndtert, mistenker korrigert sykmelding")
            return perioder
        }
        val periode = periode()
        val (overlappendePerioder, gapPerioder) = perioder.partition { it.overlapperMed(periode) }
        val nyPeriode = periode + overlappendePerioder.periode()
        info("Legger til ny periode $nyPeriode i sykmeldingsperioder")
        return (gapPerioder + listOf(nyPeriode)).sortedBy { it.start }
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
                    if (tidslinje.any { it is Dag.ProblemDag }) sykmelding.logiskFeil("Sykeperioder overlapper")
                }

        internal fun periode(perioder: List<Sykmeldingsperiode>) =
            perioder.minOfOrNull { it.fom }?.let { fom ->
                fom til perioder.maxOf { it.tom }
            }
    }
}
