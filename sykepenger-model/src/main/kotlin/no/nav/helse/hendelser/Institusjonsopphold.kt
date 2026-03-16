package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

data class Institusjonsopphold(
    private val perioder: List<Institusjonsoppholdsperiode>
) {
    data class Institusjonsoppholdsperiode(private val fom: LocalDate, private val tom: LocalDate?) {
        internal fun tilPeriode() = fom til (tom ?: LocalDate.MAX)
    }

    internal fun overlapper(aktivitetslogg: IAktivitetslogg, sykdomsperiode: Periode): Boolean {
        if (perioder.isEmpty()) {
            aktivitetslogg.info("Bruker har ingen institusjonsoppholdsperioder")
            return false
        }
        return perioder.map(Institusjonsoppholdsperiode::tilPeriode).any { opphold -> opphold.overlapperMed(sykdomsperiode) }.also { overlapper ->
            if (!overlapper) aktivitetslogg.info("Bruker har institusjonsoppholdsperioder, men det slår ikke ut på overlappssjekken")
        }
    }
}
