package no.nav.helse.person

import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer

internal class ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(
    private val orgnummer: String,
    private val inntektsopplysning: Inntektshistorikk.Inntektsopplysning
) {

    internal fun gjelder(organisasjonsnummer: String) = organisasjonsnummer == orgnummer

    internal fun accept(visitor: ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagVisitor) {
        visitor.preVisitArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(this, orgnummer)
        inntektsopplysning.accept(visitor)
        visitor.postVisitArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(this, orgnummer)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag) return false
        if (orgnummer != other.orgnummer) return false
        if (inntektsopplysning != other.inntektsopplysning) return false
        return true
    }

    override fun hashCode(): Int {
        var result = orgnummer.hashCode()
        result = 31 * result + inntektsopplysning.hashCode()
        return result
    }

    internal companion object {
        internal fun List<ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag>.sammenligningsgrunnlag(): Inntekt {
            return map { it.inntektsopplysning.rapportertInntekt() }.summer()
        }
    }
}