package no.nav.helse.hendelser

import no.nav.helse.hendelser.Periode.Companion.slutterEtter
import no.nav.helse.person.IAktivitetslogg
import java.time.LocalDate

class Dagpenger(private val perioder: List<Periode>) {
    internal fun valider(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate): IAktivitetslogg {
        if (perioder.slutterEtter(skjæringstidspunkt.minusWeeks(4))) {
            aktivitetslogg.warn("Bruker har mottatt dagpenger innenfor 4 uker av skjæringstidspunkt. Kontroller om bruker er dagpengemottaker. Kombinerte ytelser støttes foreløpig ikke av systemet")
        }
        return aktivitetslogg
    }
}
