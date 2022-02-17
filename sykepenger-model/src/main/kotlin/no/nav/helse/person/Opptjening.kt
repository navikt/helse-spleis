package no.nav.helse.person

import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.førsteFom
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal class Opptjening(arbeidsforhold: List<Arbeidsforholdhistorikk.Arbeidsforhold>, skjæringstidspunkt: LocalDate) {
    val opptjeningsdager = when (val førsteFom = arbeidsforhold.førsteFom(skjæringstidspunkt)) {
        null -> 0
        else -> ChronoUnit.DAYS.between(førsteFom, skjæringstidspunkt)
    }

    fun erOppfylt(): Boolean = opptjeningsdager >= 28
}
