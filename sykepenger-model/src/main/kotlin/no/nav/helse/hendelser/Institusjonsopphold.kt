package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import java.time.LocalDate

class Institusjonsopphold(
    private val perioder: List<Institusjonsoppholdsperiode>,
    private val aktivitetslogg: Aktivitetslogg
) {
    class Institusjonsoppholdsperiode(private val fom: LocalDate, private val tom: LocalDate?) {
        internal fun tilPeriode() = fom til (tom ?: LocalDate.MAX)
    }

    internal fun overlapper(sykdomsperiode: Periode): Boolean {
        if (perioder.isEmpty()) {
            aktivitetslogg.info("Bruker har ingen institusjonsoppholdsperioder")
            return false
        }
        return perioder.map(Institusjonsoppholdsperiode::tilPeriode)
            .any { opphold -> opphold.overlapperMed(sykdomsperiode) }
    }
}
