package no.nav.helse.spleis.sporing

import no.nav.helse.person.Person

fun serializePersonForSporing(person: Person): PersonDTO {
    val jsonBuilder = PersonBuilder(person)
    return jsonBuilder.build()
}
