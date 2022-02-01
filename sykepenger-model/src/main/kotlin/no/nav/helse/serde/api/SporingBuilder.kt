package no.nav.helse.serde.api

import no.nav.helse.Fødselsnummer
import no.nav.helse.person.Person
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.serde.AbstractBuilder
import no.nav.helse.serde.api.sporing.PersonBuilder
import no.nav.helse.serde.api.sporing.PersonDTO
import java.time.LocalDate
import java.time.LocalDateTime

fun serializePersonForSporing(person: Person): PersonDTO {
    val jsonBuilder = SporingBuilder()
    person.accept(jsonBuilder)
    return jsonBuilder.build()
}

internal class SporingBuilder() : AbstractBuilder() {
    private val personBuilder = PersonBuilder(this)

    fun build() = personBuilder.build()

    override fun preVisitPerson(
        person: Person,
        opprettet: LocalDateTime,
        aktørId: String,
        fødselsnummer: Fødselsnummer,
        dødsdato: LocalDate?,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
    ) {
        pushState(personBuilder)
    }
}

