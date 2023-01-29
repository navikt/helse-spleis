package no.nav.helse.person.inntekt

import no.nav.helse.person.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagVisitor
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer

internal class ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(
    private val orgnummer: String,
    private val inntektsopplysning: Inntektsopplysning
) {
    private val rapportertInntekt = inntektsopplysning.rapportertInntekt()

    internal fun gjelder(organisasjonsnummer: String) = organisasjonsnummer == orgnummer

    internal fun accept(visitor: ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagVisitor) {
        visitor.preVisitArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(this, orgnummer, rapportertInntekt)
        inntektsopplysning.accept(visitor)
        visitor.postVisitArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(this, orgnummer, rapportertInntekt)
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
            return map { it.rapportertInntekt }.summer()
        }
    }
}