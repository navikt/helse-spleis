package no.nav.helse.spleis.dto

import no.nav.helse.person.Person
import no.nav.helse.serde.api.dto.PersonDTO
import no.nav.helse.serde.api.serializePersonForSpeil


internal fun håndterPerson(person: Person): PersonDTO {
    return serializePersonForSpeil(person)
}