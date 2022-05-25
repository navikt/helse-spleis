package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.Grunnbeløp
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.inntektsopplysningPerArbeidsgiver
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.sykepengegrunnlag
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.ER_6G_BEGRENSET
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.VURDERT_I_INFOTRYGD
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Prosent

internal class Sykepengegrunnlag(
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
    internal val deaktiverteArbeidsforhold: List<String>,
    private val vurdertInfotrygd: Boolean,
    private val overstyrtGrunnlagForSykepengegrunnlag: Inntekt? = null,
    private val greguleringstidspunkt: LocalDateTime? = null
) {
    private val `6G`: Inntekt = Grunnbeløp.`6G`.beløp(skjæringstidspunkt, greguleringstidspunkt?.toLocalDate())
    private val grunnlag = arbeidsgiverInntektsopplysninger.sykepengegrunnlag()
    internal val grunnlagForSykepengegrunnlag: Inntekt = overstyrtGrunnlagForSykepengegrunnlag ?: grunnlag.values.summer() // TODO: gjøre private
    internal val sykepengegrunnlag = grunnlagForSykepengegrunnlag.coerceAtMost(`6G`)
    internal val begrensning = if (vurdertInfotrygd) VURDERT_I_INFOTRYGD else if (grunnlagForSykepengegrunnlag > `6G`) ER_6G_BEGRENSET else ER_IKKE_6G_BEGRENSET

    internal constructor(
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        deaktiverteArbeidsforhold: List<String>,
        skjæringstidspunkt: LocalDate,
        subsumsjonObserver: SubsumsjonObserver,
        vurdertInfotrygd: Boolean = false
    ) : this(skjæringstidspunkt, arbeidsgiverInntektsopplysninger, deaktiverteArbeidsforhold, vurdertInfotrygd) {
        subsumsjonObserver.apply {
            `§ 8-30 ledd 1`(grunnlag, grunnlagForSykepengegrunnlag)
            `§ 8-10 ledd 2 punktum 1`(
                erBegrenset = begrensning == ER_6G_BEGRENSET,
                maksimaltSykepengegrunnlag = `6G`,
                skjæringstidspunkt = skjæringstidspunkt,
                grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag
            )
        }
    }

    internal companion object {
        fun opprett(
            arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
            skjæringstidspunkt: LocalDate,
            subsumsjonObserver: SubsumsjonObserver,
            deaktiverteArbeidsforhold: List<String>
        ): Sykepengegrunnlag {
            return Sykepengegrunnlag(arbeidsgiverInntektsopplysninger, deaktiverteArbeidsforhold, skjæringstidspunkt, subsumsjonObserver)
        }

        fun opprettForInfotrygd(
            arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
            skjæringstidspunkt: LocalDate,
            subsumsjonObserver: SubsumsjonObserver
        ): Sykepengegrunnlag {
            return Sykepengegrunnlag(arbeidsgiverInntektsopplysninger, emptyList(), skjæringstidspunkt, subsumsjonObserver, true)
        }
    }

    internal fun justerGrunnbeløp() =
        Sykepengegrunnlag(
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiverInntektsopplysninger = arbeidsgiverInntektsopplysninger,
            deaktiverteArbeidsforhold = deaktiverteArbeidsforhold,
            vurdertInfotrygd = vurdertInfotrygd,
            overstyrtGrunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
            greguleringstidspunkt = LocalDateTime.now()
        )

    internal fun accept(visitor: SykepengegrunnlagVisitor) {
        visitor.preVisitSykepengegrunnlag(
            this,
            skjæringstidspunkt,
            sykepengegrunnlag,
            overstyrtGrunnlagForSykepengegrunnlag,
            grunnlagForSykepengegrunnlag,
            `6G`,
            begrensning,
            deaktiverteArbeidsforhold,
            greguleringstidspunkt,
            vurdertInfotrygd
        )
        visitor.preVisitArbeidsgiverInntektsopplysninger()
        arbeidsgiverInntektsopplysninger.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsgiverInntektsopplysninger()
        visitor.postVisitSykepengegrunnlag(
            this,
            skjæringstidspunkt,
            sykepengegrunnlag,
            overstyrtGrunnlagForSykepengegrunnlag,
            grunnlagForSykepengegrunnlag,
            `6G`,
            begrensning,
            deaktiverteArbeidsforhold,
            greguleringstidspunkt,
            vurdertInfotrygd
        )
    }

    internal fun avviksprosent(sammenligningsgrunnlag: Inntekt, subsumsjonObserver: SubsumsjonObserver) = grunnlagForSykepengegrunnlag.avviksprosent(sammenligningsgrunnlag).also { avvik ->
        subsumsjonObserver.`§ 8-30 ledd 2 punktum 1`(Prosent.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT, grunnlagForSykepengegrunnlag, sammenligningsgrunnlag, avvik)
    }
    internal fun inntektsopplysningPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning> =
        arbeidsgiverInntektsopplysninger.inntektsopplysningPerArbeidsgiver()

    internal fun oppfyllerKravTilMinimumInntekt(alder: Alder, subsumsjonObserver: SubsumsjonObserver): Boolean {
        val minimumInntekt = alder.minimumInntekt(skjæringstidspunkt)
        val oppfyllerKrav = grunnlagForSykepengegrunnlag >= minimumInntekt
        val alderPåSkjæringstidspunkt = alder.alderPåDato(skjæringstidspunkt)
        if (alder.forhøyetInntektskrav(skjæringstidspunkt))
            subsumsjonObserver.`§ 8-51 ledd 2`(oppfyllerKrav, skjæringstidspunkt, alderPåSkjæringstidspunkt, grunnlagForSykepengegrunnlag, minimumInntekt)
        else
            subsumsjonObserver.`§ 8-3 ledd 2 punktum 1`(oppfyllerKrav, skjæringstidspunkt, grunnlagForSykepengegrunnlag, minimumInntekt)
        return oppfyllerKrav
    }

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
