package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Periode.Companion.periode

class Sykmelding(
    meldingsreferanseId: UUID,
    fnr: String,
    aktørId: String,
    orgnummer: String,
    sykeperioder: List<Sykmeldingsperiode>
) : ArbeidstakerHendelse(meldingsreferanseId, fnr, aktørId, orgnummer) {

    private val opprinneligPeriode = checkNotNull(Sykmeldingsperiode.periode(sykeperioder)) { "må ha minst én periode" }
    private var sykmeldingsperiode: Periode? = opprinneligPeriode

    internal fun periode() = opprinneligPeriode

    internal fun trimLeft(dato: LocalDate) {
        sykmeldingsperiode = sykmeldingsperiode?.beholdDagerEtter(dato)
    }

    internal fun oppdaterSykmeldingsperioder(perioder: List<Periode>): List<Periode> {
        val periode = sykmeldingsperiode
        if (periode == null) {
            info("Sykmeldingsperiode har allerede blitt tidligere håndtert, mistenker korrigert sykmelding")
            return perioder
        }
        val (overlappendePerioder, gapPerioder) = perioder.partition { it.overlapperMed(periode) }
        val nyPeriode = periode + overlappendePerioder.periode()
        info("Legger til ny periode $nyPeriode i sykmeldingsperioder")
        return (gapPerioder + listOf(nyPeriode)).sortedBy { it.start }
    }
}

class Sykmeldingsperiode(fom: LocalDate, tom: LocalDate) {
    private val periode = fom til tom
    internal companion object {
        internal fun periode(perioder: List<Sykmeldingsperiode>) = perioder.map { it.periode }.periode()
    }
}
