package no.nav.helse.hendelser

import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

class Foreldrepermisjon(
    private val foreldrepengeytelse: Periode?,
    private val svangerskapsytelse: Periode?
) {

    internal fun overlapper(aktivitetslogg: IAktivitetslogg, sykdomsperiode: Periode): Boolean {
        if (foreldrepengeytelse == null && svangerskapsytelse == null) {
            aktivitetslogg.info("Bruker har ingen foreldrepenge- eller svangerskapsytelser")
            return false
        }
        return listOfNotNull(foreldrepengeytelse, svangerskapsytelse)
                .any { ytelse -> ytelse.overlapperMed(sykdomsperiode) }
    }

}
