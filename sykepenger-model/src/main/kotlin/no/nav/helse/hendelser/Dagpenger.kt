package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.hendelser.Periode.Companion.slutterEtter
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Varselkode.RV_AY_4

class Dagpenger(private val perioder: List<Periode>) {
    internal fun valider(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate): IAktivitetslogg {
        if (perioder.slutterEtter(skjæringstidspunkt.minusWeeks(4))) {
            aktivitetslogg.varsel(RV_AY_4)
        }
        return aktivitetslogg
    }
}
