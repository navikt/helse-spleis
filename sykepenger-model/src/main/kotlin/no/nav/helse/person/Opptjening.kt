package no.nav.helse.person

import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.opptjeningsperiode
import java.time.LocalDate

internal class Opptjening(arbeidsforhold: List<Arbeidsforholdhistorikk.Arbeidsforhold>, skjæringstidspunkt: LocalDate) {
    val opptjeningsperiode = arbeidsforhold.opptjeningsperiode(skjæringstidspunkt)

    fun erOppfylt(): Boolean = opptjeningsperiode.dagerMellom() >= 28
}
