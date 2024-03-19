package no.nav.helse.serde.api

import no.nav.helse.person.Person
import no.nav.helse.serde.api.sporing.PersonBuilder
import no.nav.helse.serde.api.sporing.PersonDTO

fun serializePersonForSporing(person: Person): PersonDTO {
    val jsonBuilder = PersonBuilder(person)
    return jsonBuilder.build()
}
