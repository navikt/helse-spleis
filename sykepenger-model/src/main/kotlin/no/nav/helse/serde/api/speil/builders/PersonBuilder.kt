package no.nav.helse.serde.api.speil.builders

import java.time.LocalDateTime
import no.nav.helse.Alder
import no.nav.helse.Personidentifikator
import no.nav.helse.dto.serialisering.PersonUtDto
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
    internal fun build(): PersonDTO {
        val alder = Alder(personUtDto.alder.fødselsdato, personUtDto.alder.dødsdato)

        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagBuilder(vilkårsgrunnlagHistorikk).build()
        val arbeidsgivere = personUtDto.arbeidsgivere
            .map { arbeidsgiverDto ->
                ArbeidsgiverBuilder(arbeidsgiverDto, pølsepakke.pakker.singleOrNull { it.yrkesaktivitetidentifikator == arbeidsgiverDto.organisasjonsnummer })
            }
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
