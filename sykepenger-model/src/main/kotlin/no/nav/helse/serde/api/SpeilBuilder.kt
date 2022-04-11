package no.nav.helse.serde.api

import no.nav.helse.Fødselsnummer
import no.nav.helse.person.Person
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.serde.AbstractBuilder
import no.nav.helse.serde.api.builders.PersonBuilder
import no.nav.helse.serde.api.v2.HendelseDTO
import java.time.LocalDate
import java.time.LocalDateTime

fun serializePersonForSpeil(person: Person, hendelser: List<HendelseDTO> = emptyList()): PersonDTO {
    val jsonBuilder = SpeilBuilder(hendelser)
    person.accept(jsonBuilder)
    return jsonBuilder.build()
}

internal class SpeilBuilder(private val hendelser: List<HendelseDTO>) : AbstractBuilder() {

    private companion object {
        /* Økes for å signalisere til spesialist at strukturen i snapshot'et
         * på et eller annet vis har endret seg, og at spesialist derfor må oppdatere cachede snapshots løpende
         */
        const val SNAPSHOT_VERSJON = 23
    }

    private lateinit var personBuilder: PersonBuilder

    fun build() = personBuilder.build(hendelser)

    override fun preVisitPerson(
        person: Person,
        opprettet: LocalDateTime,
        aktørId: String,
        fødselsnummer: Fødselsnummer,
        dødsdato: LocalDate?,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
    ) {
        personBuilder = PersonBuilder(this, person, fødselsnummer, aktørId, dødsdato, vilkårsgrunnlagHistorikk, SNAPSHOT_VERSJON)
        pushState(personBuilder)
    }
}

