package no.nav.helse.serde.api.sporing

import no.nav.helse.Fødselsnummer
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.serde.AbstractBuilder
import no.nav.helse.serde.api.builders.BuilderState
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class PersonBuilder(builder: AbstractBuilder) : BuilderState(builder) {
    private val arbeidsgivere = mutableListOf<ArbeidsgiverBuilder>()
    private lateinit var fødselsnummer: Fødselsnummer
    private lateinit var aktørId: String

    internal fun build(): PersonDTO {
        return PersonDTO(
            fødselsnummer = fødselsnummer.toString(),
            aktørId = aktørId,
            arbeidsgivere = arbeidsgivere.map { it.build() },
        )
    }

    override fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
        val arbeidsgiverBuilder = ArbeidsgiverBuilder(organisasjonsnummer)
        arbeidsgivere.add(arbeidsgiverBuilder)
        pushState(arbeidsgiverBuilder)
    }

    override fun postVisitPerson(
        person: Person,
        opprettet: LocalDateTime,
        aktørId: String,
        fødselsnummer: Fødselsnummer,
        dødsdato: LocalDate?,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
    ) {
        this.fødselsnummer = fødselsnummer
        this.aktørId = aktørId
        popState()
    }
}
