package no.nav.helse.hendelser

import no.nav.helse.hendelser.Periode.Companion.slutterEtter
import no.nav.helse.person.Aktivitetslogg
import java.time.LocalDate

class Arbeidsavklaringspenger(private val perioder: List<Periode>) {
    internal fun valider(aktivitetslogg: Aktivitetslogg, skjæringstidspunkt: LocalDate): Aktivitetslogg {
        if (perioder.slutterEtter(skjæringstidspunkt.minusMonths(6))) {
            aktivitetslogg.warn("Bruker har mottatt AAP innenfor 6 måneder av skjæringstidspunkt. Kontroller at brukeren har rett til sykepenger")
        }
        return aktivitetslogg
    }
}
