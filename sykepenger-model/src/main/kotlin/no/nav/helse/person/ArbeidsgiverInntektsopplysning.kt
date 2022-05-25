package no.nav.helse.person

import no.nav.helse.person.Inntektshistorikk.Inntektsopplysning.Companion.valider
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.summer

internal class ArbeidsgiverInntektsopplysning(
    private val orgnummer: String,
    private val inntektsopplysning: Inntektshistorikk.Inntektsopplysning
) {
    private fun sykepengegrunnlag(acc: Inntekt): Inntekt {
        return acc + inntektsopplysning.grunnlagForSykepengegrunnlag()
    }

    internal fun accept(visitor: ArbeidsgiverInntektsopplysningVisitor) {
        visitor.preVisitArbeidsgiverInntektsopplysning(this, orgnummer)
        inntektsopplysning.accept(visitor)
        visitor.postVisitArbeidsgiverInntektsopplysning(this, orgnummer)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ArbeidsgiverInntektsopplysning) return false
        return orgnummer == other.orgnummer && inntektsopplysning == other.inntektsopplysning
    }

    override fun hashCode(): Int {
        var result = orgnummer.hashCode()
        result = 31 * result + inntektsopplysning.hashCode()
        return result
    }

    internal companion object {
        internal fun List<ArbeidsgiverInntektsopplysning>.valider(aktivitetslogg: IAktivitetslogg) {
            map { it.inntektsopplysning }.valider(aktivitetslogg)
        }
        internal fun List<ArbeidsgiverInntektsopplysning>.sykepengegrunnlag() =
            fold(INGEN) { acc, item -> item.sykepengegrunnlag(acc)}

        internal fun List<ArbeidsgiverInntektsopplysning>.arbeidsgivergrunnlag() =
            associate { it.orgnummer to it.inntektsopplysning.grunnlagForSykepengegrunnlag() }

        internal fun List<ArbeidsgiverInntektsopplysning>.sammenligningsgrunnlag(): Inntekt {
            return map { it.inntektsopplysning.grunnlagForSammenligningsgrunnlag() }.summer()
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.inntektsopplysningPerArbeidsgiver() =
            associate { it.orgnummer to it.inntektsopplysning }
    }
}
