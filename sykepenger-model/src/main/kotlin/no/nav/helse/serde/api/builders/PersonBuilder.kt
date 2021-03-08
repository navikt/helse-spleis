package no.nav.helse.serde.api.builders

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.serde.AbstractBuilder
import no.nav.helse.serde.api.HendelseDTO
import no.nav.helse.serde.api.PersonDTO
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class PersonBuilder(
    builder: AbstractBuilder,
    private val person: Person,
    private val fødselsnummer: String,
    private val aktørId: String,
    private val dødsdato: LocalDate?
) : BuilderState(builder) {
    private val arbeidsgivere = mutableListOf<ArbeidsgiverBuilder>()
    private val inntektshistorikkBuilder = InntektshistorikkBuilder(person)

    internal fun build(hendelser: List<HendelseDTO>): PersonDTO {
        return PersonDTO(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            arbeidsgivere = arbeidsgivere.map { it.build(hendelser) }.filter { it.vedtaksperioder.isNotEmpty() },
            inntektsgrunnlag = inntektshistorikkBuilder.build(),
            dødsdato = dødsdato
        )
    }

    override fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
        val arbeidsgiverBuilder = ArbeidsgiverBuilder(arbeidsgiver, person.vilkårsgrunnlagHistorikk, id, organisasjonsnummer, fødselsnummer, inntektshistorikkBuilder)
        arbeidsgivere.add(arbeidsgiverBuilder)
        pushState(arbeidsgiverBuilder)
    }

    override fun postVisitPerson(
        person: Person,
        opprettet: LocalDateTime,
        aktørId: String,
        fødselsnummer: String,
        dødsdato: LocalDate?
    ) {
        popState()
    }
}
