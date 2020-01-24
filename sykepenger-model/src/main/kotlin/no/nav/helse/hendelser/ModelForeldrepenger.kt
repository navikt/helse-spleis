package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogger
import java.time.LocalDate

class ModelForeldrepenger(
    private val foreldrepengeytelse: Pair<LocalDate, LocalDate>?,
    private val svangerskapsytelse: Pair<LocalDate, LocalDate>?,
    aktivitetslogger: Aktivitetslogger
) {

    init {
        if (foreldrepengeytelse != null && foreldrepengeytelse.first > foreldrepengeytelse.second) aktivitetslogger.severe(
            "fom er større enn tom for foreldrepengeytelse"
        )
        if (svangerskapsytelse != null && svangerskapsytelse.first > svangerskapsytelse.second) aktivitetslogger.severe(
            "fom er større enn tom for svangerskapsytelse"
        )
    }

    fun overlapperMedSyketilfelle(syketilfelleFom: LocalDate, syketilfelleTom: LocalDate): Boolean {
        val syketilfelleRange = syketilfelleFom.rangeTo(syketilfelleTom)
        if (foreldrepengeytelse == null && svangerskapsytelse == null) {
            return false
        }

        return listOfNotNull(foreldrepengeytelse, svangerskapsytelse).any { ytelse ->
            ytelse.overlapperMed(syketilfelleRange)
        }
    }

    private fun Pair<LocalDate, LocalDate>.overlapperMed(range: ClosedRange<LocalDate>) =
        range.contains(first) || range.contains(second) || (second > range.start && first < range.endInclusive)
}
