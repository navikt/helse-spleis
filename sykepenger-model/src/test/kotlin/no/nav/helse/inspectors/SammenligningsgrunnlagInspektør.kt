package no.nav.helse.inspectors

import no.nav.helse.person.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.Sammenligningsgrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikkVisitor
import no.nav.helse.økonomi.Inntekt

internal val Sammenligningsgrunnlag.inspektør get() = Sammenligningsgrunnlag(this)

internal class Sammenligningsgrunnlag(sammenligningsgrunnlag: Sammenligningsgrunnlag) : VilkårsgrunnlagHistorikkVisitor {
    internal var arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning> = listOf()
        private set

    internal lateinit var sammenligningsgrunnlag: Inntekt
        private set
    internal val arbeidsgiverInntektsopplysningerPerArbeidsgiver: MutableMap<String, ArbeidsgiverInntektsopplysning> = mutableMapOf()

    init {
        sammenligningsgrunnlag.accept(this)
    }

    override fun preVisitSammenligningsgrunnlag(
        sammenligningsgrunnlag1: Sammenligningsgrunnlag,
        sammenligningsgrunnlag: Inntekt
    ) {
        this.sammenligningsgrunnlag = sammenligningsgrunnlag
    }

    override fun preVisitArbeidsgiverInntektsopplysninger(arbeidsgiverInntektopplysninger: List<ArbeidsgiverInntektsopplysning>) {
        this.arbeidsgiverInntektsopplysninger = arbeidsgiverInntektopplysninger
    }

    override fun preVisitArbeidsgiverInntektsopplysning(
        arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning,
        orgnummer: String
    ) {
        arbeidsgiverInntektsopplysningerPerArbeidsgiver[orgnummer] = arbeidsgiverInntektsopplysning
    }
}