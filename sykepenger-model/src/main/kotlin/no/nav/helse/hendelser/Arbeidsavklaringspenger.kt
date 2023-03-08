package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.hendelser.Periode.Companion.slutterEtter
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode

class Arbeidsavklaringspenger(private val perioder: List<Periode>) {
    internal fun valider(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate, periode: Periode): IAktivitetslogg {
        val starterFørEllerOverlapper = perioder.filterNot { it.start > periode.endInclusive }
        if (starterFørEllerOverlapper.slutterEtter(skjæringstidspunkt.minusMonths(6))) {
            aktivitetslogg.varsel(Varselkode.RV_AY_3)
        }
        return aktivitetslogg
    }
}
