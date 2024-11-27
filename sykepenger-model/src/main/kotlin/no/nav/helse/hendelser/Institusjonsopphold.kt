package no.nav.helse.hendelser

import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import java.time.LocalDate

class Institusjonsopphold(
    private val perioder: List<Institusjonsoppholdsperiode>,
) {
    class Institusjonsoppholdsperiode(
        private val fom: LocalDate,
        private val tom: LocalDate?,
    ) {
        internal fun tilPeriode() = fom til (tom ?: LocalDate.MAX)
    }

    internal fun overlapper(
        aktivitetslogg: IAktivitetslogg,
        sykdomsperiode: Periode,
    ): Boolean {
        if (perioder.isEmpty()) {
            aktivitetslogg.info("Bruker har ingen institusjonsoppholdsperioder")
            return false
        }
        return perioder
            .map(Institusjonsoppholdsperiode::tilPeriode)
            .any { opphold ->
                opphold.overlapperMed(sykdomsperiode)
            }.also { overlapper ->
                if (!overlapper) aktivitetslogg.info("Bruker har institusjonsoppholdsperioder, men det slår ikke ut på overlappssjekken")
            }
    }
}
