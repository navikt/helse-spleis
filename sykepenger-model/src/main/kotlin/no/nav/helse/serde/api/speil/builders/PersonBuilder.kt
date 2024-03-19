package no.nav.helse.serde.api.speil.builders

import no.nav.helse.Alder
import no.nav.helse.dto.serialisering.PersonUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlaghistorikkUtDto
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.serde.api.SpekematDTO
import no.nav.helse.serde.api.dto.PersonDTO

internal class PersonBuilder(
    private val personUtDto: PersonUtDto,
    private val pølsepakke: SpekematDTO,
    private val versjon: Int
) {
    internal fun build(): PersonDTO {
        val alder = Alder(personUtDto.alder.fødselsdato, personUtDto.alder.dødsdato)

        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagBuilder(personUtDto.vilkårsgrunnlagHistorikk).build()
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
}
