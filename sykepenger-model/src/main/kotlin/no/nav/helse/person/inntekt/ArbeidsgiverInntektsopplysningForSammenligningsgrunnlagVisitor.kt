package no.nav.helse.person.inntekt

import no.nav.helse.økonomi.Inntekt

internal interface ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagVisitor : SkatteopplysningVisitor {
    fun preVisitArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(
        arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag,
        orgnummer: String,
        rapportertInntekt: Inntekt
    ) {}
    fun postVisitArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(
        arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag,
        orgnummer: String,
        rapportertInntekt: Inntekt
    ) {}
}