package no.nav.helse.spleis

import no.nav.helse.Alder
import no.nav.helse.Personidentifikator
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.person.Person
import java.time.LocalDate

internal class Personopplysninger internal constructor(
    private val personidentifikator: Personidentifikator,
    private val alder: Alder
) {
    constructor(
        personidentifikator: Personidentifikator,
        fødselsdato: LocalDate,
        dødsdato: LocalDate?
    ) : this(personidentifikator, Alder(fødselsdato, dødsdato))

    fun person(subsumsjonslogg: Subsumsjonslogg) = Person(personidentifikator, alder, subsumsjonslogg)
}
