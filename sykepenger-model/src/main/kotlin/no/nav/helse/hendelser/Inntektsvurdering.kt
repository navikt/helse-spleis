package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.antallMåneder
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.kilder
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.utenOffentligeYtelser
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.Varselkode.RV_IV_1

class Inntektsvurdering(private val inntekter: List<ArbeidsgiverInntekt>) {
    init {
        require(inntekter.antallMåneder() <= 12) { "Forventer 12 eller færre inntektsmåneder" }
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, antallArbeidsgivereFraAareg: Int): Boolean {
        if (inntekter.utenOffentligeYtelser().kilder(3) > antallArbeidsgivereFraAareg) aktivitetslogg.varsel(RV_IV_1)
        return true
    }

    internal fun lagreRapporterteInntekter(person: Person, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) =
        ArbeidsgiverInntekt.lagreRapporterteInntekter(inntekter, person, skjæringstidspunkt, hendelse)
}
