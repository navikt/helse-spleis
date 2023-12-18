package no.nav.helse.person.inntekt

import no.nav.helse.person.PersonObserver
import no.nav.helse.person.SammenligningsgrunnlagVisitor
import no.nav.helse.person.builders.VedtakFattetBuilder.FastsattISpleisBuilder
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag.Companion.sammenligningsgrunnlag
import no.nav.helse.økonomi.Inntekt

internal class Sammenligningsgrunnlag(
    internal val sammenligningsgrunnlag: Inntekt,
    private val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag>,
) {

    internal fun avviksprosent(beregningsgrunnlag: Inntekt) = beregningsgrunnlag.avviksprosent(sammenligningsgrunnlag)

    internal constructor(arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag>) : this(
        arbeidsgiverInntektsopplysninger.sammenligningsgrunnlag(),
        arbeidsgiverInntektsopplysninger
    )

    internal fun erRelevant(organisasjonsnummer: String) =
        arbeidsgiverInntektsopplysninger.any { it.gjelder(organisasjonsnummer) }

    internal fun accept(visitor: SammenligningsgrunnlagVisitor) {
        visitor.preVisitSammenligningsgrunnlag(this, sammenligningsgrunnlag)
        visitor.preVisitArbeidsgiverInntektsopplysningerForSammenligningsgrunnlag(arbeidsgiverInntektsopplysninger)
        arbeidsgiverInntektsopplysninger.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsgiverInntektsopplysningerForSammenligningsgrunnlag(arbeidsgiverInntektsopplysninger)
        visitor.postVisitSammenligningsgrunnlag(this, sammenligningsgrunnlag)
    }

    internal fun build(builder: FastsattISpleisBuilder) {
        builder.innrapportertÅrsinntekt(sammenligningsgrunnlag)
    }

    internal fun sammenligningsgrunnlagSomSkalBrukersIAvviksprosentBeregnetEvent(): List<PersonObserver.AvviksprosentBeregnetEvent.Sammenligningsgrunnlag> =
        arbeidsgiverInntektsopplysninger.map { it.sammenligningsgrunnlagSomSkalBrukesIAvviksprosentBeregnetEvent() }
}
