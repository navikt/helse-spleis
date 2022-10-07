package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.Grunnbeløp
import no.nav.helse.Grunnbeløp.Companion.halvG
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.erOverstyrt
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.harInntekt
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.medInntekt
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.omregnetÅrsinntekt
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.omregnetÅrsinntektPerArbeidsgiver
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.valider
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.ER_6G_BEGRENSET
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.VURDERT_I_INFOTRYGD
import no.nav.helse.person.Varselkode.RV_IV_2
import no.nav.helse.person.Varselkode.RV_SV_1
import no.nav.helse.person.Varselkode.RV_SV_2
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Økonomi

internal class Sykepengegrunnlag(
    private val alder: Alder,
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
    private val deaktiverteArbeidsforhold: List<String>,
    private val vurdertInfotrygd: Boolean,
    private val skjønnsmessigFastsattBeregningsgrunnlag: Inntekt? = null,
    `6G`: Inntekt? = null
) : Comparable<Inntekt> {
    private val `6G`: Inntekt = `6G` ?: Grunnbeløp.`6G`.beløp(skjæringstidspunkt, LocalDate.now())
    private val beregningsgrunnlag: Inntekt = skjønnsmessigFastsattBeregningsgrunnlag ?: arbeidsgiverInntektsopplysninger.omregnetÅrsinntekt()
    private val sykepengegrunnlag = beregningsgrunnlag.coerceAtMost(this.`6G`)
    private val begrensning = if (vurdertInfotrygd) VURDERT_I_INFOTRYGD else if (beregningsgrunnlag > this.`6G`) ER_6G_BEGRENSET else ER_IKKE_6G_BEGRENSET

    private val forhøyetInntektskrav = alder.forhøyetInntektskrav(skjæringstidspunkt)
    private val minsteinntekt = (if (forhøyetInntektskrav) Grunnbeløp.`2G` else halvG).minsteinntekt(skjæringstidspunkt)
    private val oppfyllerMinsteinntektskrav = beregningsgrunnlag >= minsteinntekt

    internal constructor(
        alder: Alder,
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        deaktiverteArbeidsforhold: List<String>,
        skjæringstidspunkt: LocalDate,
        subsumsjonObserver: SubsumsjonObserver,
        vurdertInfotrygd: Boolean = false
    ) : this(alder, skjæringstidspunkt, arbeidsgiverInntektsopplysninger, deaktiverteArbeidsforhold, vurdertInfotrygd) {
        subsumsjonObserver.apply {
            `§ 8-30 ledd 1`(arbeidsgiverInntektsopplysninger.omregnetÅrsinntektPerArbeidsgiver(), beregningsgrunnlag)
            `§ 8-10 ledd 2 punktum 1`(
                erBegrenset = begrensning == ER_6G_BEGRENSET,
                maksimaltSykepengegrunnlag = `6G`,
                skjæringstidspunkt = skjæringstidspunkt,
                beregningsgrunnlag = beregningsgrunnlag
            )
            if (alder.forhøyetInntektskrav(skjæringstidspunkt))
                `§ 8-51 ledd 2`(oppfyllerMinsteinntektskrav, skjæringstidspunkt, alder.alderPåDato(skjæringstidspunkt), beregningsgrunnlag, minsteinntekt)
            else
                `§ 8-3 ledd 2 punktum 1`(oppfyllerMinsteinntektskrav, skjæringstidspunkt, beregningsgrunnlag, minsteinntekt)
        }
    }

    internal companion object {
        private val varsel: IAktivitetslogg.(Varselkode) -> Unit = IAktivitetslogg::varsel
        private val funksjonellFeil: IAktivitetslogg.(Varselkode) -> Unit = IAktivitetslogg::funksjonellFeil

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
        else aktivitetslogg.varsel(RV_SV_1)
        return oppfyllerMinsteinntektskrav && !aktivitetslogg.harFunksjonelleFeilEllerVerre()
    }

    internal fun harNødvendigInntektForVilkårsprøving(organisasjonsnummer: String) =
        arbeidsgiverInntektsopplysninger.harInntekt(organisasjonsnummer)

    internal fun validerInntekt(
        aktivitetslogg: IAktivitetslogg,
        organisasjonsnummer: List<String>
    ) {
        val manglerInntekt = organisasjonsnummer.filterNot { harNødvendigInntektForVilkårsprøving(it) }.takeUnless { it.isEmpty() } ?: return
        manglerInntekt.forEach {
            aktivitetslogg.info("Mangler inntekt for $it på skjæringstidspunkt $skjæringstidspunkt")
        }
        valideringstrategi()(aktivitetslogg, RV_SV_2)
    }

    internal fun validerAvvik(aktivitetslogg: IAktivitetslogg, sammenligningsgrunnlag: Sammenligningsgrunnlag) {
        val avvik = avviksprosent(sammenligningsgrunnlag.sammenligningsgrunnlag, SubsumsjonObserver.NullObserver)
        if (harAkseptabeltAvvik(avvik)) aktivitetslogg.info("Har %.0f %% eller mindre avvik i inntekt (%.2f %%)", Prosent.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.prosent(), avvik.prosent())
        else valideringstrategi()(aktivitetslogg, RV_IV_2)
    }

    private fun harAkseptabeltAvvik(avvik: Prosent) = avvik <= Prosent.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT

    private fun valideringstrategi(): IAktivitetslogg.(Varselkode) -> Unit {
        return when {
            erOverstyrt() -> varsel
            else -> funksjonellFeil
        }
    }

    private fun erOverstyrt() = deaktiverteArbeidsforhold.isNotEmpty() || arbeidsgiverInntektsopplysninger.erOverstyrt()

    internal fun justerGrunnbeløp() =
        Sykepengegrunnlag(
            alder = alder,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiverInntektsopplysninger = arbeidsgiverInntektsopplysninger,
            deaktiverteArbeidsforhold = deaktiverteArbeidsforhold,
            vurdertInfotrygd = vurdertInfotrygd,
            skjønnsmessigFastsattBeregningsgrunnlag = skjønnsmessigFastsattBeregningsgrunnlag
        )

    internal fun accept(visitor: SykepengegrunnlagVisitor) {
        visitor.preVisitSykepengegrunnlag(
            this,
            skjæringstidspunkt,
            sykepengegrunnlag,
            skjønnsmessigFastsattBeregningsgrunnlag,
            beregningsgrunnlag,
            `6G`,
            begrensning,
            deaktiverteArbeidsforhold,
            vurdertInfotrygd,
            minsteinntekt,
            oppfyllerMinsteinntektskrav
        )
        visitor.preVisitArbeidsgiverInntektsopplysninger(arbeidsgiverInntektsopplysninger)
        arbeidsgiverInntektsopplysninger.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsgiverInntektsopplysninger(arbeidsgiverInntektsopplysninger)
        visitor.postVisitSykepengegrunnlag(
            this,
            skjæringstidspunkt,
            sykepengegrunnlag,
            skjønnsmessigFastsattBeregningsgrunnlag,
            beregningsgrunnlag,
            `6G`,
            begrensning,
            deaktiverteArbeidsforhold,
            vurdertInfotrygd,
            minsteinntekt,
            oppfyllerMinsteinntektskrav
        )
    }

    internal fun avviksprosent(sammenligningsgrunnlag: Inntekt, subsumsjonObserver: SubsumsjonObserver) = beregningsgrunnlag.avviksprosent(sammenligningsgrunnlag).also { avvik ->
        subsumsjonObserver.`§ 8-30 ledd 2 punktum 1`(Prosent.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT, beregningsgrunnlag, sammenligningsgrunnlag, avvik)
    }

    internal fun inntektskilde() = when {
        arbeidsgiverInntektsopplysninger.size > 1 -> Inntektskilde.FLERE_ARBEIDSGIVERE
        else -> Inntektskilde.EN_ARBEIDSGIVER
    }

    internal fun harInntektFraAOrdningen() =
        arbeidsgiverInntektsopplysninger.any { it.harInntektFraAOrdningen() }

    internal fun erRelevant(organisasjonsnummer: String) = arbeidsgiverInntektsopplysninger.any {
        it.gjelder(organisasjonsnummer)
    }
    internal fun medInntekt(organisasjonsnummer: String, dato: LocalDate, økonomi: Økonomi, arbeidsgiverperiode: Arbeidsgiverperiode?, regler: ArbeidsgiverRegler, subsumsjonObserver: SubsumsjonObserver): Økonomi? {
        return arbeidsgiverInntektsopplysninger.medInntekt(organisasjonsnummer, skjæringstidspunkt, dato, økonomi, arbeidsgiverperiode, regler, subsumsjonObserver)
    }
    internal fun build(builder: VedtakFattetBuilder) {
        builder
            .sykepengegrunnlag(this.sykepengegrunnlag)
            .beregningsgrunnlag(this.beregningsgrunnlag)
            .begrensning(this.begrensning)
            .omregnetÅrsinntektPerArbeidsgiver(arbeidsgiverInntektsopplysninger.omregnetÅrsinntektPerArbeidsgiver())
    }
    override fun equals(other: Any?): Boolean {
        if (other !is Sykepengegrunnlag) return false
        return sykepengegrunnlag == other.sykepengegrunnlag
                 && arbeidsgiverInntektsopplysninger == other.arbeidsgiverInntektsopplysninger
                 && beregningsgrunnlag == other.beregningsgrunnlag
                 && begrensning == other.begrensning
                 && deaktiverteArbeidsforhold == other.deaktiverteArbeidsforhold
    }

    override fun hashCode(): Int {
        var result = sykepengegrunnlag.hashCode()
        result = 31 * result + arbeidsgiverInntektsopplysninger.hashCode()
        result = 31 * result + beregningsgrunnlag.hashCode()
        result = 31 * result + begrensning.hashCode()
        result = 31 * result + deaktiverteArbeidsforhold.hashCode()
        return result
    }
    override fun compareTo(other: Inntekt) = this.sykepengegrunnlag.compareTo(other)
    internal fun er6GBegrenset() = begrensning == ER_6G_BEGRENSET

    enum class Begrensning {
        ER_6G_BEGRENSET, ER_IKKE_6G_BEGRENSET, VURDERT_I_INFOTRYGD
    }

}
