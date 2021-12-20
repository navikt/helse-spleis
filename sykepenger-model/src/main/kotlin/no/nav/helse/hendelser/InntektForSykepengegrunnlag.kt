package no.nav.helse.hendelser

import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.antallMåneder
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.harInntektFor
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import java.time.LocalDate

class InntektForSykepengegrunnlag(
    private val inntekter: List<ArbeidsgiverInntekt>,
    private val arbeidsforhold: List<Arbeidsforhold>
) {

    internal fun valider(
        aktivitetslogg: IAktivitetslogg,
    ): IAktivitetslogg {
        if (inntekter.antallMåneder() > 3L) aktivitetslogg.error("Forventer maks 3 inntektsmåneder")
        if (finnerFrilansinntektDeSiste3Månedene(inntekter, arbeidsforhold))
            aktivitetslogg.error("Fant frilanserinntekt på en arbeidsgiver de siste 3 månedene")

        return aktivitetslogg
    }

    internal fun finnerFrilansinntektDeSiste3Månedene(inntekter: List<ArbeidsgiverInntekt>, arbeidsforhold: List<Arbeidsforhold>) =
        arbeidsforhold.any { it.erFrilanser && inntekter.harInntektFor(it.orgnummer) }

    internal fun lagreInntekter(person: Person, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) =
        ArbeidsgiverInntekt.lagreSykepengegrunnlag(inntekter, person, skjæringstidspunkt, hendelse)

    class Arbeidsforhold(
        val orgnummer: String,
        val erFrilanser: Boolean
    )
}



