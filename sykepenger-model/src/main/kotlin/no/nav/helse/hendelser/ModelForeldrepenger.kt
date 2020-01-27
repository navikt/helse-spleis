package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogger

class ModelForeldrepenger(
    private val foreldrepengeytelse: Periode?,
    private val svangerskapsytelse: Periode?,
    aktivitetslogger: Aktivitetslogger
) {

    fun overlapperMedSyketilfelle(periode: Periode): Boolean {
        if (foreldrepengeytelse == null && svangerskapsytelse == null) return false
        return listOfNotNull(foreldrepengeytelse, svangerskapsytelse).any { ytelse ->
            ytelse.overlapperMed(periode)
        }
    }

}
