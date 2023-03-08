package no.nav.helse.hendelser

import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

class Pleiepenger(
    private val perioder: List<Periode>
) {
    internal fun overlapper(aktivitetslogg: IAktivitetslogg, sykdomsperiode: Periode): Boolean {
        if (perioder.isEmpty()) {
            aktivitetslogg.info("Bruker har ingen pleiepengeytelser")
            return false
        }
        return perioder.any { ytelse -> ytelse.overlapperMed(sykdomsperiode.oppdaterFom(sykdomsperiode.start.minusWeeks(4))) }
    }
}
