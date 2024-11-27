package no.nav.helse.hendelser

import java.time.LocalDate

internal sealed interface OverstyrInntektsgrunnlag : Hendelse {
    fun erRelevant(skjæringstidspunkt: LocalDate): Boolean
}
