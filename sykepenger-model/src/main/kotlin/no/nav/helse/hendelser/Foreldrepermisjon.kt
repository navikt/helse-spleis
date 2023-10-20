package no.nav.helse.hendelser

import no.nav.helse.hendelser.Ytelser.Companion.familieYtelserPeriode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

class Foreldrepermisjon(
    private val foreldrepengeytelse: List<Periode>,
    private val svangerskapsytelse: List<Periode>
) {

    internal fun overlapper(aktivitetslogg: IAktivitetslogg, sykdomsperiode: Periode, erForlengelse: Boolean): Boolean {
        if (foreldrepengeytelse.isEmpty() && svangerskapsytelse.isEmpty()) {
            aktivitetslogg.info("Bruker har ingen foreldrepenge- eller svangerskapsytelser")
            return false
        }
        val overlappsperiode = if (erForlengelse) sykdomsperiode else sykdomsperiode.familieYtelserPeriode
        return (foreldrepengeytelse + svangerskapsytelse)
                .any { ytelse -> ytelse.overlapperMed(overlappsperiode) }
    }

}
