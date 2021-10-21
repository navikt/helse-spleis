package no.nav.helse.person

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.inntekt
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.inntektsopplysningPerArbeidsgiver
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate

internal class Sykepengegrunnlag(
    internal val sykepengegrunnlag: Inntekt,
    private val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
    internal val grunnlagForSykepengegrunnlag: Inntekt
) {
    private companion object {
        private fun sykepengegrunnlag(inntekt: Inntekt, skjæringstidspunkt: LocalDate, aktivitetslogg: IAktivitetslogg): Inntekt {
            val maks = Grunnbeløp.`6G`.beløp(skjæringstidspunkt)
            return if (inntekt > maks) {
                aktivitetslogg.etterlevelse.`§8-10 ledd 2`(
                    oppfylt = true,
                    funnetRelevant = true,
                    maks = maks,
                    skjæringstidspunkt = skjæringstidspunkt,
                    grunnlagForSykepengegrunnlag = inntekt
                )
                maks
            } else {
                aktivitetslogg.etterlevelse.`§8-10 ledd 2`(
                    oppfylt = true,
                    funnetRelevant = false,
                    maks = maks,
                    skjæringstidspunkt = skjæringstidspunkt,
                    grunnlagForSykepengegrunnlag = inntekt
                )
                inntekt
            }
        }
    }

    constructor(
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        skjæringstidspunkt: LocalDate,
        aktivitetslogg: IAktivitetslogg
    ) : this(
        sykepengegrunnlag(arbeidsgiverInntektsopplysninger.inntekt(), skjæringstidspunkt, aktivitetslogg),
        arbeidsgiverInntektsopplysninger,
        arbeidsgiverInntektsopplysninger.inntekt()
    )

    constructor(
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
    ) : this(
        arbeidsgiverInntektsopplysninger.inntekt(),
        arbeidsgiverInntektsopplysninger,
        arbeidsgiverInntektsopplysninger.inntekt()
    )

    internal fun accept(vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {
        vilkårsgrunnlagHistorikkVisitor.preVisitSykepengegrunnlag(this, sykepengegrunnlag, grunnlagForSykepengegrunnlag)
        arbeidsgiverInntektsopplysninger.forEach { it.accept(vilkårsgrunnlagHistorikkVisitor) }
        vilkårsgrunnlagHistorikkVisitor.postVisitSykepengegrunnlag(this, sykepengegrunnlag, grunnlagForSykepengegrunnlag)
    }

    internal fun avviksprosent(sammenligningsgrunnlag: Inntekt) = grunnlagForSykepengegrunnlag.avviksprosent(sammenligningsgrunnlag)
    internal fun inntektsopplysningPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning> =
        arbeidsgiverInntektsopplysninger.inntektsopplysningPerArbeidsgiver()
}
