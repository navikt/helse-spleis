package no.nav.helse.person

import no.nav.helse.person.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag.Companion.sammenligningsgrunnlag
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.økonomi.Inntekt

internal class Sammenligningsgrunnlag(
    internal val sammenligningsgrunnlag: Inntekt,
    private val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag>,
) {

    internal fun avviksprosent(sykepengegrunnlag: Sykepengegrunnlag, subsumsjonObserver: SubsumsjonObserver) =
        sykepengegrunnlag.avviksprosent(sammenligningsgrunnlag, subsumsjonObserver)

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

}
