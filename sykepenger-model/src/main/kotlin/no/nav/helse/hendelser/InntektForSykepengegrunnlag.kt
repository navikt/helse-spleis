package no.nav.helse.hendelser

import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.antallMåneder
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.kilder
import no.nav.helse.person.*
import no.nav.helse.person.Periodetype.FORLENGELSE
import no.nav.helse.person.Periodetype.INFOTRYGDFORLENGELSE
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.util.*

class InntektForSykepengegrunnlag(
    private val inntekter: List<ArbeidsgiverInntekt>
) {

    internal fun valider(
        aktivitetslogg: IAktivitetslogg,
    ): Boolean {

        if (inntekter.antallMåneder() == 3L) aktivitetslogg.error("Forventer 3 inntektsmåneder")
        return aktivitetslogg.hasErrorsOrWorse()
    }

    internal fun lagreInntekter(person: Person, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) =
        ArbeidsgiverInntekt.lagreInntekter(inntekter, person, skjæringstidspunkt, hendelse)
}


