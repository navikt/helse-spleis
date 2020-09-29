package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg

class Pleiepenger(
    private val perioder: List<Periode>,
    private val aktivitetslogg: Aktivitetslogg
) {
    internal fun overlapper(sykdomsperiode: Periode): Boolean {
        if (perioder.isEmpty()) {
            aktivitetslogg.info("Bruker har ingen pleiepengeytelser")
            return false
        }
        return perioder.any { ytelse -> ytelse.overlapperMed(sykdomsperiode) }
    }
}
