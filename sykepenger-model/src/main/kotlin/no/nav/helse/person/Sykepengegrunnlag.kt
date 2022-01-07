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
    internal companion object {
        fun opprett(
            arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
            skjæringstidspunkt: LocalDate,
            aktivitetslogg: IAktivitetslogg
        ): Sykepengegrunnlag {
            val inntekt = arbeidsgiverInntektsopplysninger.sykepengegrunnlag(aktivitetslogg)
            val begrensning = if (inntekt > Grunnbeløp.`6G`.beløp(skjæringstidspunkt)) ER_6G_BEGRENSET else ER_IKKE_6G_BEGRENSET
            return Sykepengegrunnlag(
                sykepengegrunnlag(inntekt, skjæringstidspunkt, aktivitetslogg),
                arbeidsgiverInntektsopplysninger,
                inntekt,
                begrensning
            )
        }

        fun opprettForInfotrygd(
            arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
            skjæringstidspunkt: LocalDate,
            aktivitetslogg: IAktivitetslogg
        ): Sykepengegrunnlag {
            val inntekt = arbeidsgiverInntektsopplysninger.sykepengegrunnlag(aktivitetslogg)
            return Sykepengegrunnlag(
                sykepengegrunnlag(inntekt, skjæringstidspunkt, aktivitetslogg),
                arbeidsgiverInntektsopplysninger,
                inntekt,
                VURDERT_I_INFOTRYGD
            )
        }

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

    internal fun accept(vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {
        vilkårsgrunnlagHistorikkVisitor.preVisitSykepengegrunnlag(this, sykepengegrunnlag, grunnlagForSykepengegrunnlag, begrensning)
        vilkårsgrunnlagHistorikkVisitor.preVisitArbeidsgiverInntektsopplysninger()
        arbeidsgiverInntektsopplysninger.forEach { it.accept(vilkårsgrunnlagHistorikkVisitor) }
        vilkårsgrunnlagHistorikkVisitor.postVisitArbeidsgiverInntektsopplysninger()
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
