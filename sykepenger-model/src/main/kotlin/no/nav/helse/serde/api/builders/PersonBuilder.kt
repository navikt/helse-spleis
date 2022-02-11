package no.nav.helse.serde.api.builders

import no.nav.helse.Fødselsnummer
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.serde.AbstractBuilder
import no.nav.helse.serde.api.ArbeidsforholdDTO
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
    private val fødselsnummer: Fødselsnummer,
    private val aktørId: String,
    private val dødsdato: LocalDate?,
    private val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    private val versjon: Int
) : BuilderState(builder) {
    private val arbeidsgivere = mutableListOf<ArbeidsgiverBuilder>()
    private val vilkårsgrunnlagInntektBuilder = VilkårsgrunnlagInntektBuilder(person)
    private val inntektshistorikkForAordningenBuilder = InntektshistorikkForAOrdningenBuilder(person)

    internal fun build(hendelser: List<HendelseDTO>): PersonDTO {

        val sammenligningsgrunnlagBuilder = OppsamletSammenligningsgrunnlagBuilder(person)
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagBuilder(person, sammenligningsgrunnlagBuilder, inntektshistorikkForAordningenBuilder).build()

        return PersonDTO(
            fødselsnummer = fødselsnummer.toString(),
            aktørId = aktørId,
            arbeidsgivere = arbeidsgivere.map { it.build(hendelser, fødselsnummer.toString(), vilkårsgrunnlagHistorikk) },
            vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk.toDTO(),
            inntektsgrunnlag = vilkårsgrunnlagInntektBuilder.build(),
            dødsdato = dødsdato,
            versjon = versjon,
            arbeidsforholdPerSkjæringstidspunkt = person.hentArbeidsforhold(::ArbeidsforholdDTO)
        )
    }

    override fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
        val arbeidsgiverBuilder =
            ArbeidsgiverBuilder(arbeidsgiver, vilkårsgrunnlagHistorikk, id, organisasjonsnummer, fødselsnummer.toString(), vilkårsgrunnlagInntektBuilder)
        if (vilkårsgrunnlagHistorikk.erRelevant(organisasjonsnummer, person.skjæringstidspunkter()) || arbeidsgiver.harFerdigstiltPeriode() || arbeidsgiver.harSpleisSykdom()) {
            arbeidsgivere.add(arbeidsgiverBuilder)
        }
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
        popState()
    }
}
