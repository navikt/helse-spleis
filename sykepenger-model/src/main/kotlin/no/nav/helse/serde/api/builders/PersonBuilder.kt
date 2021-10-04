package no.nav.helse.serde.api.builders

import no.nav.helse.Fødselsnummer
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.serde.AbstractBuilder
import no.nav.helse.serde.api.PersonDTO
import no.nav.helse.serde.api.v2.HendelseDTO
import no.nav.helse.serde.api.v2.buildere.OppsamletSammenligningsgrunnlagBuilder
import no.nav.helse.serde.api.v2.buildere.VilkårsgrunnlagBuilder
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class PersonBuilder(
    builder: AbstractBuilder,
    private val person: Person,
    private val fødselsnummer: String,
    private val aktørId: String,
    private val dødsdato: LocalDate?,
    private val versjon: Int
) : BuilderState(builder) {
    private val arbeidsgivere = mutableListOf<ArbeidsgiverBuilder>()
    private val inntektshistorikkBuilder = InntektshistorikkBuilder(person)

    internal fun build(hendelser: List<HendelseDTO>): PersonDTO {

        fun skalVises(orgnummer: String) = person.skjæringstidspunkter().any { person.harAktivtArbeidsforholdEllerInntekt(it, orgnummer) }

        val sammenligningsgrunnlagBuilder = OppsamletSammenligningsgrunnlagBuilder(person)
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagBuilder(person.vilkårsgrunnlagHistorikk, sammenligningsgrunnlagBuilder).build()

        return PersonDTO(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            arbeidsgivere = arbeidsgivere.map { it.build(hendelser, fødselsnummer, vilkårsgrunnlagHistorikk) }.filter {
                it.vedtaksperioder.isNotEmpty() || skalVises(it.organisasjonsnummer)
            },
            vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk.toDTO(),
            inntektsgrunnlag = inntektshistorikkBuilder.build(),
            dødsdato = dødsdato,
            versjon = versjon
        )
    }

    override fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
        val arbeidsgiverBuilder =
            ArbeidsgiverBuilder(arbeidsgiver, person.vilkårsgrunnlagHistorikk, id, organisasjonsnummer, fødselsnummer, inntektshistorikkBuilder)
        arbeidsgivere.add(arbeidsgiverBuilder)
        pushState(arbeidsgiverBuilder)
    }

    override fun postVisitPerson(
        person: Person,
        opprettet: LocalDateTime,
        aktørId: String,
        fødselsnummer: Fødselsnummer,
        dødsdato: LocalDate?
    ) {
        popState()
    }
}
