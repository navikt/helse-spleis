package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.etterlevelse.BehandlingSubsumsjonslogg
import no.nav.helse.person.Person

internal sealed interface OverstyrInntektsgrunnlag : Hendelse {
    fun erRelevant(skjæringstidspunkt: LocalDate): Boolean
}