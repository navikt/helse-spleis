package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.hendelser.Periode.Companion.slutterEtter
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_4

class Dagpenger(private val perioder: List<Periode>) {
    internal fun valider(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate, periode: Periode): IAktivitetslogg {
        val starterFørEllerOverlapper = perioder.filterNot { it.start > periode.endInclusive }
        if (starterFørEllerOverlapper.slutterEtter(skjæringstidspunkt.minusWeeks(4))) {
            aktivitetslogg.varsel(RV_AY_4)
        }
        return aktivitetslogg
    }
}
