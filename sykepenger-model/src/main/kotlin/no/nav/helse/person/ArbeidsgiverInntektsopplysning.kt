package no.nav.helse.person

import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer

internal class ArbeidsgiverInntektsopplysning(private val orgnummer: String, private val inntektsopplysning: Inntektshistorikk.Inntektsopplysning) {

    internal fun accept(vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {
        vilkårsgrunnlagHistorikkVisitor.preVisitArbeidsgiverInntektsopplysning(this, orgnummer)
        inntektsopplysning.accept(vilkårsgrunnlagHistorikkVisitor)
        vilkårsgrunnlagHistorikkVisitor.postVisitArbeidsgiverInntektsopplysning(this, orgnummer)
    }

    companion object {
        internal fun List<ArbeidsgiverInntektsopplysning>.sykepengegrunnlag(subsumsjonObserver: SubsumsjonObserver): Inntekt {
            val grunnlagForSykepengegrunnlag = map { it.inntektsopplysning.grunnlagForSykepengegrunnlag() }.summer()
            val grunnlagForSykepengegrunnlagPerArbeidsgiver = this
                .associateBy { it.orgnummer }
                .mapValues { it.value.inntektsopplysning.grunnlagForSykepengegrunnlag() }
            subsumsjonObserver.`§8-30 ledd 1`(grunnlagForSykepengegrunnlagPerArbeidsgiver, grunnlagForSykepengegrunnlag)
            return grunnlagForSykepengegrunnlag
        }
        internal fun List<ArbeidsgiverInntektsopplysning>.sammenligningsgrunnlag(): Inntekt {
            return map { it.inntektsopplysning.grunnlagForSammenligningsgrunnlag() }.summer()
        }
        internal fun List<ArbeidsgiverInntektsopplysning>.inntektsopplysningPerArbeidsgiver() = associate { it.orgnummer to it.inntektsopplysning }
        internal fun List<ArbeidsgiverInntektsopplysning>.grunnlagForSykepengegrunnlagPerArbeidsgiver() = inntektsopplysningPerArbeidsgiver()
            .mapValues { (_, inntektsopplysning) -> inntektsopplysning.grunnlagForSykepengegrunnlag().reflection { årlig, _, _, _ -> årlig } }
    }
}
