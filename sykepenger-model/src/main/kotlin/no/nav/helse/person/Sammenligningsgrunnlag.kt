package no.nav.helse.person

import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.inntektsopplysningPerArbeidsgiver
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.sammenligningsgrunnlag
import no.nav.helse.økonomi.Inntekt

internal class Sammenligningsgrunnlag(
    internal val sammenligningsgrunnlag: Inntekt,
    private val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
) {

    internal constructor(arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>) : this(
        arbeidsgiverInntektsopplysninger.sammenligningsgrunnlag(),
        arbeidsgiverInntektsopplysninger
    )


    internal fun accept(visitor: VilkårsgrunnlagHistorikkVisitor) {
        visitor.preVisitSammenligningsgrunnlag(this, sammenligningsgrunnlag)
        visitor.preVisitArbeidsgiverInntektsopplysninger()
        arbeidsgiverInntektsopplysninger.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsgiverInntektsopplysninger()
        visitor.postVisitSammenligningsgrunnlag(this, sammenligningsgrunnlag)
    }

    internal fun inntektsopplysningPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning> =
        arbeidsgiverInntektsopplysninger.inntektsopplysningPerArbeidsgiver()


}
