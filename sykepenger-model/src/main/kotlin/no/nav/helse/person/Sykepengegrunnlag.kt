package no.nav.helse.person

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat.Companion.`§8-10 ledd 2 punktum 1`
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.sykepengegrunnlag
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.inntektsopplysningPerArbeidsgiver
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.*
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate

internal class Sykepengegrunnlag(
    internal val sykepengegrunnlag: Inntekt,
    private val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
    internal val grunnlagForSykepengegrunnlag: Inntekt,
    internal val begrensning: Begrensning
) {
    private companion object {
        private fun sykepengegrunnlag(inntekt: Inntekt, skjæringstidspunkt: LocalDate, aktivitetslogg: IAktivitetslogg): Inntekt {
            val maksimaltSykepengegrunnlag = Grunnbeløp.`6G`.beløp(skjæringstidspunkt)
            return if (inntekt > maksimaltSykepengegrunnlag) {
                aktivitetslogg.`§8-10 ledd 2 punktum 1`(
                    oppfylt = true,
                    funnetRelevant = true,
                    maksimaltSykepengegrunnlag = maksimaltSykepengegrunnlag,
                    skjæringstidspunkt = skjæringstidspunkt,
                    grunnlagForSykepengegrunnlag = inntekt
                )
                maksimaltSykepengegrunnlag
            } else {
                aktivitetslogg.`§8-10 ledd 2 punktum 1`(
                    oppfylt = true,
                    funnetRelevant = false,
                    maksimaltSykepengegrunnlag = maksimaltSykepengegrunnlag,
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
        arbeidsgiverInntektsopplysninger,
        skjæringstidspunkt,
        aktivitetslogg,
        arbeidsgiverInntektsopplysninger.sykepengegrunnlag(aktivitetslogg)
    )

    constructor(
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        aktivitetslogg: IAktivitetslogg
    ) : this(
        arbeidsgiverInntektsopplysninger,
        arbeidsgiverInntektsopplysninger.sykepengegrunnlag(aktivitetslogg)
    )

    private constructor(
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        skjæringstidspunkt: LocalDate,
        aktivitetslogg: IAktivitetslogg,
        grunnlagForSykepengegrunnlag: Inntekt,
    ): this(
        sykepengegrunnlag(grunnlagForSykepengegrunnlag, skjæringstidspunkt, aktivitetslogg),
        arbeidsgiverInntektsopplysninger,
        grunnlagForSykepengegrunnlag,
        if (grunnlagForSykepengegrunnlag > Grunnbeløp.`6G`.beløp(skjæringstidspunkt)) ER_6G_BEGRENSET else ER_IKKE_6G_BEGRENSET
    )

    private constructor(
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        grunnlagForSykepengegrunnlag: Inntekt
    ): this (
        grunnlagForSykepengegrunnlag,
        arbeidsgiverInntektsopplysninger,
        grunnlagForSykepengegrunnlag,
        VURDERT_I_INFOTRYGD
    )

    internal fun accept(vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {
        vilkårsgrunnlagHistorikkVisitor.preVisitSykepengegrunnlag(this, sykepengegrunnlag, grunnlagForSykepengegrunnlag, begrensning)
        arbeidsgiverInntektsopplysninger.forEach { it.accept(vilkårsgrunnlagHistorikkVisitor) }
        vilkårsgrunnlagHistorikkVisitor.postVisitSykepengegrunnlag(this, sykepengegrunnlag, grunnlagForSykepengegrunnlag, begrensning)
    }

    internal fun avviksprosent(sammenligningsgrunnlag: Inntekt) = grunnlagForSykepengegrunnlag.avviksprosent(sammenligningsgrunnlag)
    internal fun inntektsopplysningPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning> =
        arbeidsgiverInntektsopplysninger.inntektsopplysningPerArbeidsgiver()

    internal fun oppdaterHarMinimumInntekt(skjæringstidspunkt: LocalDate, person: Person, grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata) {
        val oppfyltKravTilMinimumInntekt = oppfyllerKravTilMinimumInntekt(person.minimumInntekt(skjæringstidspunkt))
        person.oppdaterHarMinimumInntekt(skjæringstidspunkt, grunnlagsdata, oppfyltKravTilMinimumInntekt)
    }

    internal fun oppfyllerKravTilMinimumInntekt(minimumInntekt: Inntekt) = grunnlagForSykepengegrunnlag >= minimumInntekt

    enum class Begrensning {
        ER_6G_BEGRENSET, ER_IKKE_6G_BEGRENSET, VURDERT_I_INFOTRYGD
    }
}
