package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg

class Opplæringspenger(
    private val perioder: List<Periode>,
    private val aktivitetslogg: Aktivitetslogg
) {
    internal fun overlapper(sykdomsperiode: Periode): Boolean {
        if (perioder.isEmpty()) {
            aktivitetslogg.info("Bruker har ingen opplæringspengeytelser")
            return false
        }
        return perioder.any { ytelse -> ytelse.overlapperMed(sykdomsperiode) }
    }
}
