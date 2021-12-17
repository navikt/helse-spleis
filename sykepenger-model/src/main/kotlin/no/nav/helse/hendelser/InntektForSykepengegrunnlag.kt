package no.nav.helse.hendelser

import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.antallMåneder
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import java.time.LocalDate

class InntektForSykepengegrunnlag(
    private val inntekter: List<ArbeidsgiverInntekt>
) {

    internal fun valider(
        aktivitetslogg: IAktivitetslogg,
    ): IAktivitetslogg {

        if (inntekter.antallMåneder() > 3L) aktivitetslogg.error("Forventer maks 3 inntektsmåneder")
        return aktivitetslogg
    }

    internal fun lagreInntekter(person: Person, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) =
        ArbeidsgiverInntekt.lagreSykepengegrunnlag(inntekter, person, skjæringstidspunkt, hendelse)
}


