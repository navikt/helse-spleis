package no.nav.helse.hendelser

import no.nav.helse.hendelser.Ytelser.Companion.familieYtelserPeriode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

class Foreldrepermisjon(
    private val foreldrepengeytelse: List<Periode>,
    private val svangerskapsytelse: List<Periode>
) {

    internal fun overlapper(aktivitetslogg: IAktivitetslogg, sykdomsperiode: Periode): Boolean {
        if (foreldrepengeytelse.isEmpty() && svangerskapsytelse.isEmpty()) {
            aktivitetslogg.info("Bruker har ingen foreldrepenge- eller svangerskapsytelser")
            return false
        }
        return (foreldrepengeytelse + svangerskapsytelse)
                .any { ytelse -> ytelse.overlapperMed(sykdomsperiode.familieYtelserPeriode) }
    }

}
