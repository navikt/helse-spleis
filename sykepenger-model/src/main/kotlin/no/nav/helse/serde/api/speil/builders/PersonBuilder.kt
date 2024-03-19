package no.nav.helse.serde.api.speil.builders

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.Personidentifikator
import no.nav.helse.dto.serialisering.PersonUtDto
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.serde.AbstractBuilder
import no.nav.helse.serde.api.BuilderState
import no.nav.helse.serde.api.SpekematDTO
import no.nav.helse.serde.api.dto.PersonDTO

internal class  PersonBuilder(
    builder: AbstractBuilder,
    private val personUtDto: PersonUtDto,
    private val pølsepakke: SpekematDTO,
    private val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    private val versjon: Int
) : BuilderState(builder) {
    private val alder: Alder = Alder(personUtDto.alder.fødselsdato, personUtDto.alder.dødsdato)
    private val arbeidsgivere = mutableListOf<ArbeidsgiverBuilder>()

    internal fun build(): PersonDTO {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagBuilder(vilkårsgrunnlagHistorikk).build()
        val arbeidsgivere = arbeidsgivere
            .map { it.build(alder, vilkårsgrunnlagHistorikk) }
            .let { arbeidsgivere ->
                arbeidsgivere.map { it.medGhostperioder(vilkårsgrunnlagHistorikk, arbeidsgivere) }
            }
            .filterNot { it.erTom(vilkårsgrunnlagHistorikk) }

        return PersonDTO(
            fødselsnummer = personUtDto.fødselsnummer,
            aktørId = personUtDto.aktørId,
            arbeidsgivere = arbeidsgivere,
            dødsdato = personUtDto.alder.dødsdato,
            versjon = versjon,
            vilkårsgrunnlag = vilkårsgrunnlagHistorikk.toDTO()
        )
    }

    override fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
        val arbeidsgiverBuilder = ArbeidsgiverBuilder(arbeidsgiver, id, organisasjonsnummer, pølsepakke.pakker.singleOrNull { it.yrkesaktivitetidentifikator == organisasjonsnummer })
        arbeidsgivere.add(arbeidsgiverBuilder)
        pushState(arbeidsgiverBuilder)
    }

    override fun postVisitPerson(
        person: Person,
        opprettet: LocalDateTime,
        aktørId: String,
        personidentifikator: Personidentifikator,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
    ) {
        popState()
    }
}
