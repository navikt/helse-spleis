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

    constructor(
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        skjæringstidspunkt: LocalDate,
    ) : this(
        minOf(arbeidsgiverInntektsopplysninger.inntekt(), Grunnbeløp.`6G`.beløp(skjæringstidspunkt)),
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
    internal fun inntektsopplysningPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning> = arbeidsgiverInntektsopplysninger.inntektsopplysningPerArbeidsgiver()
}
