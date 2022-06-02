package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.Grunnbeløp
import no.nav.helse.Grunnbeløp.Companion.halvG
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.inntektsopplysningPerArbeidsgiver
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.omregnetÅrsinntekt
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.omregnetÅrsinntektPerArbeidsgiver
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.valider
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.ER_6G_BEGRENSET
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.VURDERT_I_INFOTRYGD
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent

internal class Sykepengegrunnlag(
    private val alder: Alder,
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
    internal val deaktiverteArbeidsforhold: List<String>,
    private val vurdertInfotrygd: Boolean,
    private val skjønnsmessigFastsattÅrsinntekt: Inntekt? = null,
    `6G`: Inntekt? = null
) {
    private val `6G`: Inntekt = `6G` ?: Grunnbeløp.`6G`.beløp(skjæringstidspunkt, LocalDate.now())
    internal val inntektsgrunnlag: Inntekt = skjønnsmessigFastsattÅrsinntekt ?: arbeidsgiverInntektsopplysninger.omregnetÅrsinntekt() // TODO: gjøre private
    internal val sykepengegrunnlag = inntektsgrunnlag.coerceAtMost(this.`6G`)
    private val maksimalDagsats = sykepengegrunnlag.rundTilDaglig()
    private val begrensning = if (vurdertInfotrygd) VURDERT_I_INFOTRYGD else if (inntektsgrunnlag > this.`6G`) ER_6G_BEGRENSET else ER_IKKE_6G_BEGRENSET

    private val forhøyetInntektskrav = alder.forhøyetInntektskrav(skjæringstidspunkt)
    private val minsteinntekt = (if (forhøyetInntektskrav) Grunnbeløp.`2G` else halvG).minsteinntekt(skjæringstidspunkt)
    private val oppfyllerMinsteinntektskrav = inntektsgrunnlag >= minsteinntekt

    internal constructor(
        alder: Alder,
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        deaktiverteArbeidsforhold: List<String>,
        skjæringstidspunkt: LocalDate,
        subsumsjonObserver: SubsumsjonObserver,
        vurdertInfotrygd: Boolean = false
    ) : this(alder, skjæringstidspunkt, arbeidsgiverInntektsopplysninger, deaktiverteArbeidsforhold, vurdertInfotrygd) {
        subsumsjonObserver.apply {
            `§ 8-30 ledd 1`(arbeidsgiverInntektsopplysninger.omregnetÅrsinntektPerArbeidsgiver(), inntektsgrunnlag)
            `§ 8-10 ledd 2 punktum 1`(
                erBegrenset = begrensning == ER_6G_BEGRENSET,
                maksimaltSykepengegrunnlag = `6G`,
                skjæringstidspunkt = skjæringstidspunkt,
                inntektsgrunnlag = inntektsgrunnlag
            )
            if (alder.forhøyetInntektskrav(skjæringstidspunkt))
                `§ 8-51 ledd 2`(oppfyllerMinsteinntektskrav, skjæringstidspunkt, alder.alderPåDato(skjæringstidspunkt), inntektsgrunnlag, minsteinntekt)
            else
                `§ 8-3 ledd 2 punktum 1`(oppfyllerMinsteinntektskrav, skjæringstidspunkt, inntektsgrunnlag, minsteinntekt)
        }
    }

    internal companion object {
        fun opprett(
            alder: Alder,
            arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
            skjæringstidspunkt: LocalDate,
            subsumsjonObserver: SubsumsjonObserver,
            deaktiverteArbeidsforhold: List<String>
        ): Sykepengegrunnlag {
            return Sykepengegrunnlag(alder, arbeidsgiverInntektsopplysninger, deaktiverteArbeidsforhold, skjæringstidspunkt, subsumsjonObserver)
        }

        fun opprettForInfotrygd(
            alder: Alder,
            arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
            skjæringstidspunkt: LocalDate,
            subsumsjonObserver: SubsumsjonObserver
        ): Sykepengegrunnlag {
            return Sykepengegrunnlag(alder, arbeidsgiverInntektsopplysninger, emptyList(), skjæringstidspunkt, subsumsjonObserver, true)
        }
    }

    // TODO: la Sykepengegrunnlag _avvise_ dager selv, ikke returnere begrunnelse
    // TODO: Sykepengegrunnlag må avvise dager under 2G etter at bruker har fylt 67 år, selv om skjæringstidspunktet er satt før 67 års-dagen: https://trello.com/c/0ld9Q4qD
    internal fun begrunnelse(begrunnelser: MutableList<Begrunnelse>) {
        if (oppfyllerMinsteinntektskrav) return
        begrunnelser.add(if (forhøyetInntektskrav) Begrunnelse.MinimumInntektOver67 else Begrunnelse.MinimumInntekt)
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg): Boolean {
        arbeidsgiverInntektsopplysninger.valider(aktivitetslogg)
        if (oppfyllerMinsteinntektskrav) aktivitetslogg.info("Krav til minste sykepengegrunnlag er oppfylt")
        else aktivitetslogg.warn("Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag")
        return oppfyllerMinsteinntektskrav && !aktivitetslogg.hasErrorsOrWorse()
    }

    internal fun justerGrunnbeløp() =
        Sykepengegrunnlag(
            alder = alder,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiverInntektsopplysninger = arbeidsgiverInntektsopplysninger,
            deaktiverteArbeidsforhold = deaktiverteArbeidsforhold,
            vurdertInfotrygd = vurdertInfotrygd,
            skjønnsmessigFastsattÅrsinntekt = skjønnsmessigFastsattÅrsinntekt
        )
    internal fun accept(visitor: SykepengegrunnlagVisitor) {
        visitor.preVisitSykepengegrunnlag(
            this,
            skjæringstidspunkt,
            sykepengegrunnlag,
            skjønnsmessigFastsattÅrsinntekt,
            inntektsgrunnlag,
            maksimalDagsats,
            `6G`,
            begrensning,
            deaktiverteArbeidsforhold,
            vurdertInfotrygd,
            minsteinntekt,
            oppfyllerMinsteinntektskrav
        )
        visitor.preVisitArbeidsgiverInntektsopplysninger()
        arbeidsgiverInntektsopplysninger.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsgiverInntektsopplysninger()
        visitor.postVisitSykepengegrunnlag(
            this,
            skjæringstidspunkt,
            sykepengegrunnlag,
            skjønnsmessigFastsattÅrsinntekt,
            inntektsgrunnlag,
            maksimalDagsats,
            `6G`,
            begrensning,
            deaktiverteArbeidsforhold,
            vurdertInfotrygd,
            minsteinntekt,
            oppfyllerMinsteinntektskrav
        )
    }

    internal fun avviksprosent(sammenligningsgrunnlag: Inntekt, subsumsjonObserver: SubsumsjonObserver) = inntektsgrunnlag.avviksprosent(sammenligningsgrunnlag).also { avvik ->
        subsumsjonObserver.`§ 8-30 ledd 2 punktum 1`(Prosent.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT, inntektsgrunnlag, sammenligningsgrunnlag, avvik)
    }

    internal fun inntektsopplysningPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning> =
        arbeidsgiverInntektsopplysninger.inntektsopplysningPerArbeidsgiver()

    internal fun build(builder: VedtakFattetBuilder) {
        builder
            .sykepengegrunnlag(this.sykepengegrunnlag)
            .inntektsgrunnlag(this.inntektsgrunnlag)
            .begrensning(this.begrensning)
            .omregnetÅrsinntektPerArbeidsgiver(arbeidsgiverInntektsopplysninger.omregnetÅrsinntektPerArbeidsgiver())
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Sykepengegrunnlag) return false
        return sykepengegrunnlag == other.sykepengegrunnlag
                 && arbeidsgiverInntektsopplysninger == other.arbeidsgiverInntektsopplysninger
                 && inntektsgrunnlag == other.inntektsgrunnlag
                 && begrensning == other.begrensning
                 && deaktiverteArbeidsforhold == other.deaktiverteArbeidsforhold
    }

    override fun hashCode(): Int {
        var result = sykepengegrunnlag.hashCode()
        result = 31 * result + arbeidsgiverInntektsopplysninger.hashCode()
        result = 31 * result + inntektsgrunnlag.hashCode()
        result = 31 * result + begrensning.hashCode()
        result = 31 * result + deaktiverteArbeidsforhold.hashCode()
        return result
    }

    enum class Begrensning {
        ER_6G_BEGRENSET, ER_IKKE_6G_BEGRENSET, VURDERT_I_INFOTRYGD
    }

}
