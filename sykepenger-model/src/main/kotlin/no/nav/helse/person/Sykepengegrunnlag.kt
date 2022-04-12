package no.nav.helse.person

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.sykepengegrunnlag
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.inntektsopplysningPerArbeidsgiver
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.*
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate

internal class Sykepengegrunnlag(
    internal val sykepengegrunnlag: Inntekt,
    private val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
    internal val grunnlagForSykepengegrunnlag: Inntekt,
    internal val begrensning: Begrensning,
    internal val deaktiverteArbeidsforhold: List<String>
) {
    internal companion object {
        fun opprett(
            arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
            skjæringstidspunkt: LocalDate,
            subsumsjonObserver: SubsumsjonObserver,
            deaktiverteArbeidsforhold: List<String>
        ): Sykepengegrunnlag {
            val inntekt = arbeidsgiverInntektsopplysninger.sykepengegrunnlag(subsumsjonObserver)
            val begrensning = if (inntekt > Grunnbeløp.`6G`.beløp(skjæringstidspunkt)) ER_6G_BEGRENSET else ER_IKKE_6G_BEGRENSET
            return Sykepengegrunnlag(
                sykepengegrunnlag(inntekt, skjæringstidspunkt, subsumsjonObserver),
                arbeidsgiverInntektsopplysninger,
                inntekt,
                begrensning,
                deaktiverteArbeidsforhold
            )
        }

        fun opprettForInfotrygd(
            arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
            skjæringstidspunkt: LocalDate,
            subsumsjonObserver: SubsumsjonObserver
        ): Sykepengegrunnlag {
            val inntekt = arbeidsgiverInntektsopplysninger.sykepengegrunnlag(subsumsjonObserver)
            return Sykepengegrunnlag(
                sykepengegrunnlag(inntekt, skjæringstidspunkt, subsumsjonObserver),
                arbeidsgiverInntektsopplysninger,
                inntekt,
                VURDERT_I_INFOTRYGD,
                emptyList()
            )
        }

        private fun sykepengegrunnlag(inntekt: Inntekt, skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver): Inntekt {
            val maksimaltSykepengegrunnlag = Grunnbeløp.`6G`.beløp(skjæringstidspunkt)
            subsumsjonObserver.`§ 8-10 ledd 2 punktum 1`(
                erBegrenset = inntekt > maksimaltSykepengegrunnlag,
                maksimaltSykepengegrunnlag = maksimaltSykepengegrunnlag,
                skjæringstidspunkt = skjæringstidspunkt,
                grunnlagForSykepengegrunnlag = inntekt
            )
            return inntekt.coerceAtMost(maksimaltSykepengegrunnlag)
        }
    }

    internal fun accept(vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {
        vilkårsgrunnlagHistorikkVisitor.preVisitSykepengegrunnlag(this, sykepengegrunnlag, grunnlagForSykepengegrunnlag, begrensning, deaktiverteArbeidsforhold)
        vilkårsgrunnlagHistorikkVisitor.preVisitArbeidsgiverInntektsopplysninger()
        arbeidsgiverInntektsopplysninger.forEach { it.accept(vilkårsgrunnlagHistorikkVisitor) }
        vilkårsgrunnlagHistorikkVisitor.postVisitArbeidsgiverInntektsopplysninger()
        vilkårsgrunnlagHistorikkVisitor.postVisitSykepengegrunnlag(this, sykepengegrunnlag, grunnlagForSykepengegrunnlag, begrensning, deaktiverteArbeidsforhold)
    }

    internal fun avviksprosent(sammenligningsgrunnlag: Inntekt) = grunnlagForSykepengegrunnlag.avviksprosent(sammenligningsgrunnlag)
    internal fun inntektsopplysningPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning> =
        arbeidsgiverInntektsopplysninger.inntektsopplysningPerArbeidsgiver()

    internal fun oppdaterHarMinimumInntekt(skjæringstidspunkt: LocalDate, person: Person, grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata) {
        val oppfyltKravTilMinimumInntekt = oppfyllerKravTilMinimumInntekt(person.minimumInntekt(skjæringstidspunkt))
        person.oppdaterHarMinimumInntekt(skjæringstidspunkt, grunnlagsdata, oppfyltKravTilMinimumInntekt)
    }
    internal fun oppfyllerKravTilMinimumInntekt(minimumInntekt: Inntekt) = grunnlagForSykepengegrunnlag >= minimumInntekt

    override fun equals(other: Any?): Boolean {
        if (other !is Sykepengegrunnlag) return false
        return sykepengegrunnlag == other.sykepengegrunnlag
                 && arbeidsgiverInntektsopplysninger == other.arbeidsgiverInntektsopplysninger
                 && grunnlagForSykepengegrunnlag == other.grunnlagForSykepengegrunnlag
                 && begrensning == other.begrensning
                 && deaktiverteArbeidsforhold == other.deaktiverteArbeidsforhold
    }

    override fun hashCode(): Int {
        var result = sykepengegrunnlag.hashCode()
        result = 31 * result + arbeidsgiverInntektsopplysninger.hashCode()
        result = 31 * result + grunnlagForSykepengegrunnlag.hashCode()
        result = 31 * result + begrensning.hashCode()
        result = 31 * result + deaktiverteArbeidsforhold.hashCode()
        return result
    }

    enum class Begrensning {
        ER_6G_BEGRENSET, ER_IKKE_6G_BEGRENSET, VURDERT_I_INFOTRYGD
    }

}
