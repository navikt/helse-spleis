package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.serde.AbstractBuilder
import no.nav.helse.serde.api.BuilderState
import no.nav.helse.serde.api.dto.HendelseDTO
import no.nav.helse.serde.api.dto.PersonDTO
import no.nav.helse.Alder

internal class  PersonBuilder(
    builder: AbstractBuilder,
    private val personidentifikator: Personidentifikator,
    private val aktørId: String,
    private val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    private val versjon: Int
) : BuilderState(builder) {
    private lateinit var alder: Alder
    private var dødsdato: LocalDate? = null
    private val arbeidsgivere = mutableListOf<ArbeidsgiverBuilder>()

    internal fun build(hendelser: List<HendelseDTO>): PersonDTO {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagBuilder(vilkårsgrunnlagHistorikk).build()
        val arbeidsgivere = arbeidsgivere
            .map { it.build(hendelser, alder, vilkårsgrunnlagHistorikk) }
            .let { arbeidsgivere ->
                arbeidsgivere.map { it.medGhostperioder(vilkårsgrunnlagHistorikk, arbeidsgivere) }
            }
            .filterNot { it.erTom(vilkårsgrunnlagHistorikk) }

        return PersonDTO(
            fødselsnummer = personidentifikator.toString(),
            aktørId = aktørId,
            arbeidsgivere = arbeidsgivere,
            dødsdato = dødsdato,
            versjon = versjon,
            vilkårsgrunnlag = vilkårsgrunnlagHistorikk.toDTO()
        )
    }

    override fun visitAlder(alder: Alder, fødselsdato: LocalDate, dødsdato: LocalDate?) {
        this.alder = alder
        this.dødsdato = dødsdato
    }

    override fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
        val arbeidsgiverBuilder = ArbeidsgiverBuilder(arbeidsgiver, id, organisasjonsnummer)
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
