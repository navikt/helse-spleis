package no.nav.helse.hendelser

import no.nav.helse.hendelser.Periode.Companion.etter
import no.nav.helse.person.Aktivitetslogg
import java.time.LocalDate

class Arbeidsavklaringspenger(private val perioder: List<Periode>) {
    internal fun valider(aktivitetslogg: Aktivitetslogg, førsteFraværsdag: LocalDate): Aktivitetslogg {
        if (perioder.etter(førsteFraværsdag.minusMonths(6))) {
            aktivitetslogg.warn("Bruker har mottatt AAP innenfor 6 måneder av første fraværsdag. Kontroller at brukeren har rett til sykepenger")
        }
        return aktivitetslogg
    }
}
