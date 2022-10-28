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
import no.nav.helse.serde.api.dto.ArbeidsgiverDTO
import no.nav.helse.serde.api.dto.HendelseDTO
import no.nav.helse.serde.api.dto.PersonDTO
import no.nav.helse.serde.api.dto.Vilkårsgrunnlag
import no.nav.helse.serde.api.speil.builders.ArbeidsgiverBuilder.Companion.vilkårsgrunnlagIderSomPekesPåAvGhostPerioder
import no.nav.helse.utbetalingstidslinje.Alder
import org.slf4j.LoggerFactory

internal class PersonBuilder(
    builder: AbstractBuilder,
    private val person: Person,
    private val personidentifikator: Personidentifikator,
    private val aktørId: String,
    private val dødsdato: LocalDate?,
    private val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    private val versjon: Int
) : BuilderState(builder) {
    private lateinit var alder: Alder
    private val arbeidsgivere = mutableListOf<ArbeidsgiverBuilder>()

    internal fun build(hendelser: List<HendelseDTO>): PersonDTO {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagBuilder(vilkårsgrunnlagHistorikk).build()
        val arbeidsgivere = arbeidsgivere.map { it.build(hendelser, alder, vilkårsgrunnlagHistorikk) }
        val vilkårsgrunnlag = vilkårsgrunnlagHistorikk.vilkårsgrunnlagSomPekesPåAvBeregnedePerioder()
        sjekkVilkårsgrunnlag(vilkårsgrunnlag, arbeidsgivere, vilkårsgrunnlagHistorikk)

        return PersonDTO(
            fødselsnummer = personidentifikator.toString(),
            aktørId = aktørId,
            arbeidsgivere = arbeidsgivere,
            vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk.toDTO(),
            dødsdato = dødsdato,
            versjon = versjon,
            vilkårsgrunnlag = vilkårsgrunnlag
        )
    }

    private fun sjekkVilkårsgrunnlag(
        vilkårsgrunnlag: Map<UUID, Vilkårsgrunnlag>,
        arbeidsgivere: List<ArbeidsgiverDTO>,
        vilkårsgrunnlagHistorikk: IVilkårsgrunnlagHistorikk
    ) {
        if (vilkårsgrunnlag.isNotEmpty() && arbeidsgivere.vilkårsgrunnlagIderSomPekesPåAvGhostPerioder()
                .any { ghostVilkårsgrunnlagId ->
                    !vilkårsgrunnlagHistorikk.vilkårsgrunnlagSomPekesPåAvBeregnedePerioder()
                        .containsKey(ghostVilkårsgrunnlagId)
                }
        ) {
            sikkerlog.warn("$aktørId har en arbeidsgiver med en ghostperiode som peker på et vilkårsgrunnlag som ingen av beregnede periodene peker på, hvordan kan dette skje?")
        }
    }

    override fun visitAlder(alder: Alder, fødselsdato: LocalDate) {
        this.alder = alder
    }

    override fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
        val arbeidsgiverBuilder = ArbeidsgiverBuilder(arbeidsgiver, id, organisasjonsnummer)
        if (vilkårsgrunnlagHistorikk.erRelevant(organisasjonsnummer, person.skjæringstidspunkter()) || arbeidsgiver.harFerdigstiltPeriode() || arbeidsgiver.harSpleisSykdom()) {
            arbeidsgivere.add(arbeidsgiverBuilder)
        }
        pushState(arbeidsgiverBuilder)
    }

    override fun postVisitPerson(
        person: Person,
        opprettet: LocalDateTime,
        aktørId: String,
        personidentifikator: Personidentifikator,
        dødsdato: LocalDate?,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
    ) {
        popState()
    }

    private companion object {
        private val sikkerlog = LoggerFactory.getLogger("tjenestekall")
    }
}
