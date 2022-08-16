package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Personopplysninger
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.somPersonidentifikator
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
    private val fødselsdato: LocalDate,
    orgnummer: String,
    sykeperioder: List<Sykmeldingsperiode>,
    sykmeldingSkrevet: LocalDateTime,
    private val mottatt: LocalDateTime
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

    override fun personopplysninger() = Personopplysninger(fødselsnummer.somPersonidentifikator(), aktørId, fødselsdato)

    override fun valider(periode: Periode, subsumsjonObserver: SubsumsjonObserver): IAktivitetslogg {
        validerAtSykmeldingIkkeErForGammel()
        return this
    }

    internal fun validerAtSykmeldingIkkeErForGammel() {
        if (periode.endInclusive < mottatt.toLocalDate().minusMonths(6)) funksjonellFeil(ERRORTEKST_FOR_GAMMEL)
    }

    override fun sykdomstidslinje() = sykdomstidslinje

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
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
