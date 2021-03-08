package no.nav.helse.serde.api

import no.nav.helse.person.Person
import no.nav.helse.serde.AbstractBuilder
import no.nav.helse.serde.api.builders.PersonBuilder
import java.time.LocalDate
import java.time.LocalDateTime

fun serializePersonForSpeil(person: Person, hendelser: List<HendelseDTO> = emptyList()): PersonDTO {
    val jsonBuilder = SpeilBuilder(hendelser)
    person.accept(jsonBuilder)
    return jsonBuilder.build()
}

internal class SpeilBuilder(private val hendelser: List<HendelseDTO>) : AbstractBuilder() {

    private lateinit var personBuilder: PersonBuilder

    fun build() = personBuilder.build(hendelser)

    override fun preVisitPerson(
        person: Person,
        opprettet: LocalDateTime,
        aktørId: String,
        fødselsnummer: String,
        dødsdato: LocalDate?
    ) {
        personBuilder = PersonBuilder(this, person, fødselsnummer, aktørId, dødsdato)
        pushState(personBuilder)
    }

}

