package no.nav.helse.spleis

import java.time.LocalDate
import no.nav.helse.Alder
import no.nav.helse.Personidentifikator
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.person.Person

internal class Personopplysninger internal constructor(
    private val personidentifikator: Personidentifikator,
    private val aktørId: String,
    private val alder: Alder
) {
    constructor(
        personidentifikator: Personidentifikator,
        aktørId: String,
        fødselsdato: LocalDate,
        dødsdato: LocalDate?
    ) : this(personidentifikator, aktørId, Alder(fødselsdato, dødsdato))

    fun person(subsumsjonslogg: Subsumsjonslogg) = Person(aktørId, personidentifikator, alder, subsumsjonslogg)
}