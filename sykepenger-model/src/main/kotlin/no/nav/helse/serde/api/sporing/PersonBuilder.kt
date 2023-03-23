package no.nav.helse.serde.api.sporing

import no.nav.helse.Personidentifikator
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.serde.AbstractBuilder
import no.nav.helse.serde.api.BuilderState
import java.time.LocalDateTime
import java.util.*

internal class PersonBuilder(builder: AbstractBuilder) : BuilderState(builder) {
    private val arbeidsgivere = mutableListOf<ArbeidsgiverBuilder>()
    private lateinit var personidentifikator: Personidentifikator
    private lateinit var aktørId: String

    internal fun build(): PersonDTO {
        return PersonDTO(
            fødselsnummer = personidentifikator.toString(),
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
        personidentifikator: Personidentifikator,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
    ) {
        this.personidentifikator = personidentifikator
        this.aktørId = aktørId
        popState()
    }
}
