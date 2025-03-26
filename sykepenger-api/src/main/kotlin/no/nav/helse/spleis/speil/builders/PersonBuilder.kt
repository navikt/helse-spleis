package no.nav.helse.spleis.speil.builders

import java.util.UUID
import no.nav.helse.dto.serialisering.PersonUtDto
import no.nav.helse.spleis.speil.SpekematDTO
import no.nav.helse.spleis.speil.dto.AlderDTO
import no.nav.helse.spleis.speil.dto.ArbeidsgiverDTO
import no.nav.helse.spleis.speil.dto.PersonDTO

internal class PersonBuilder(
    private val personUtDto: PersonUtDto,
    private val pølsepakke: SpekematDTO,
    private val versjon: Int
) {
    internal fun build(): PersonDTO {
        val alder = AlderDTO(personUtDto.alder.fødselsdato, personUtDto.alder.dødsdato)
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagBuilder(personUtDto.vilkårsgrunnlagHistorikk, alder).build()
        val arbeidsgivere = personUtDto.arbeidsgivere
            .map { arbeidsgiverDto ->
                ArbeidsgiverBuilder(arbeidsgiverDto, pølsepakke.pakker.singleOrNull { it.yrkesaktivitetidentifikator == arbeidsgiverDto.organisasjonsnummer })
            }
            .map { it.build(alder, vilkårsgrunnlagHistorikk) }
            .let { arbeidsgivere ->
                val arbeidsgivereFraVilkårsgrunnlag = vilkårsgrunnlagHistorikk
                    .arbeidsgivere()
                    .filter { arbeidsgiverUtenSøknad ->
                        arbeidsgivere.none { it.organisasjonsnummer == arbeidsgiverUtenSøknad }
                    }
                    .map { arbeidsgiverUtenSøknad ->
                        ArbeidsgiverDTO(
                            organisasjonsnummer = arbeidsgiverUtenSøknad,
                            id = UUID.randomUUID(),
                            generasjoner = emptyList()
                        )
                    }
                (arbeidsgivere + arbeidsgivereFraVilkårsgrunnlag).map {
                    it.medGhostperioder(vilkårsgrunnlagHistorikk, arbeidsgivere)
                }
            }
            .filterNot { it.erTom(vilkårsgrunnlagHistorikk) }

        return PersonDTO(
            fødselsnummer = personUtDto.fødselsnummer,
            arbeidsgivere = arbeidsgivere,
            dødsdato = personUtDto.alder.dødsdato,
            versjon = versjon,
            vilkårsgrunnlag = vilkårsgrunnlagHistorikk.toDTO()
        )
    }
}
