package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

class Sykmelding(
    meldingsreferanseId: MeldingsreferanseId,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
    sykeperioder: List<Sykmeldingsperiode>
) : Hendelse {
    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SYSTEM,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = true
        )
    }

    private val opprinneligPeriode = checkNotNull(Sykmeldingsperiode.periode(sykeperioder)) { "må ha minst én periode" }
    private var sykmeldingsperiode: Periode? = opprinneligPeriode

    internal fun periode() = opprinneligPeriode

    internal fun trimLeft(dato: LocalDate) {
        sykmeldingsperiode = sykmeldingsperiode?.beholdDagerEtter(dato)
    }

    internal fun oppdaterSykmeldingsperioder(aktivitetslogg: IAktivitetslogg, perioder: List<Periode>): List<Periode> {
        val periode = sykmeldingsperiode
        if (periode == null) {
            aktivitetslogg.info("Sykmeldingsperiode har allerede blitt tidligere håndtert, mistenker korrigert sykmelding")
            return perioder
        }
        val (overlappendePerioder, gapPerioder) = perioder.partition { it.overlapperMed(periode) }
        val nyPeriode = periode + overlappendePerioder.periode()
        aktivitetslogg.info("Legger til ny periode $nyPeriode i sykmeldingsperioder")
        return (gapPerioder + listOf(nyPeriode)).sortedBy { it.start }
    }
}

class Sykmeldingsperiode(fom: LocalDate, tom: LocalDate) {
    private val periode = fom til tom

    internal companion object {
        internal fun periode(perioder: List<Sykmeldingsperiode>) = perioder.map { it.periode }.periode()
    }
}
