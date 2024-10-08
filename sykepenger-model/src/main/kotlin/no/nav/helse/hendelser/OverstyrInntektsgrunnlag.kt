package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.etterlevelse.BehandlingSubsumsjonslogg
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal interface OverstyrInntektsgrunnlag : IAktivitetslogg {
    fun erRelevant(skjæringstidspunkt: LocalDate): Boolean
    fun vilkårsprøvEtterNyInformasjonFraSaksbehandler(person: Person, jurist: BehandlingSubsumsjonslogg)
}