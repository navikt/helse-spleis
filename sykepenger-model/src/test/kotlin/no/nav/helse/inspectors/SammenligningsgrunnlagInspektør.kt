package no.nav.helse.inspectors

import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag
import no.nav.helse.person.inntekt.Sammenligningsgrunnlag
import no.nav.helse.person.SammenligningsgrunnlagVisitor
import no.nav.helse.økonomi.Inntekt

internal val Sammenligningsgrunnlag.inspektør get() = SammenligningsgrunnlagInspektør(this)

internal class SammenligningsgrunnlagInspektør(sammenligningsgrunnlag: Sammenligningsgrunnlag) : SammenligningsgrunnlagVisitor {
    internal var arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag> = listOf()
        private set

    internal lateinit var sammenligningsgrunnlag: Inntekt
        private set
    internal val arbeidsgiverInntektsopplysningerPerArbeidsgiver: MutableMap<String, ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag> = mutableMapOf()

    init {
        sammenligningsgrunnlag.accept(this)
    }

    override fun preVisitSammenligningsgrunnlag(
        sammenligningsgrunnlag1: Sammenligningsgrunnlag,
        sammenligningsgrunnlag: Inntekt
    ) {
        this.sammenligningsgrunnlag = sammenligningsgrunnlag
    }

    override fun preVisitArbeidsgiverInntektsopplysningerForSammenligningsgrunnlag(arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag>) {
        this.arbeidsgiverInntektsopplysninger = arbeidsgiverInntektsopplysninger
    }

    override fun preVisitArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(
        arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag,
        orgnummer: String
    ) {
        arbeidsgiverInntektsopplysningerPerArbeidsgiver[orgnummer] = arbeidsgiverInntektsopplysning
    }
}