package no.nav.helse.inspectors

import java.time.LocalDate
import no.nav.helse.person.VilkårsgrunnlagHistorikkVisitor
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Sykepengegrunnlag
import no.nav.helse.utbetalingslinjer.UtbetalingInntektskilde
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import kotlin.properties.Delegates

internal val Sykepengegrunnlag.inspektør get() = SykepengegrunnlagInspektør(this)

internal class SykepengegrunnlagInspektør(sykepengegrunnlag: Sykepengegrunnlag) : VilkårsgrunnlagHistorikkVisitor {
    lateinit var minsteinntekt: Inntekt
    var oppfyllerMinsteinntektskrav: Boolean by Delegates.notNull<Boolean>()
    lateinit var sykepengegrunnlag: Inntekt
    lateinit var beregningsgrunnlag: Inntekt
    lateinit var omregnetÅrsinntekt: Inntekt
    lateinit var `6G`: Inntekt
    lateinit var deaktiverteArbeidsforhold: List<String>
    internal val arbeidsgiverInntektsopplysningerPerArbeidsgiver: MutableMap<String, ArbeidsgiverInntektsopplysning> = mutableMapOf()
    internal lateinit var inntektskilde: UtbetalingInntektskilde
        private set
    internal var arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning> = listOf()
        private set
    internal var deaktiverteArbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning> = listOf()
        private set
    internal var avviksprosent by Delegates.notNull<Int>()
        private set
    internal lateinit var tilstand: Sykepengegrunnlag.Tilstand

    init {
        sykepengegrunnlag.accept(this)
    }

    override fun preVisitSykepengegrunnlag(
        sykepengegrunnlag1: Sykepengegrunnlag,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Inntekt,
        avviksprosent: Prosent,
        totalOmregnetÅrsinntekt: Inntekt,
        beregningsgrunnlag: Inntekt,
        `6G`: Inntekt,
        begrensning: Sykepengegrunnlag.Begrensning,
        vurdertInfotrygd: Boolean,
        minsteinntekt: Inntekt,
        oppfyllerMinsteinntektskrav: Boolean,
        tilstand: Sykepengegrunnlag.Tilstand
    ) {
        this.minsteinntekt = minsteinntekt
        this.oppfyllerMinsteinntektskrav = oppfyllerMinsteinntektskrav
        this.`6G` = `6G`
        this.sykepengegrunnlag = sykepengegrunnlag
        this.beregningsgrunnlag = beregningsgrunnlag
        this.omregnetÅrsinntekt = totalOmregnetÅrsinntekt
        this.inntektskilde = sykepengegrunnlag1.inntektskilde()
        this.avviksprosent = avviksprosent.roundToInt()
        this.tilstand = tilstand
    }

    override fun preVisitDeaktiverteArbeidsgiverInntektsopplysninger(arbeidsgiverInntektopplysninger: List<ArbeidsgiverInntektsopplysning>) {
        this.deaktiverteArbeidsgiverInntektsopplysninger = arbeidsgiverInntektopplysninger
        this.deaktiverteArbeidsforhold = arbeidsgiverInntektopplysninger.map { it.inspektør.orgnummer }
    }

    override fun preVisitArbeidsgiverInntektsopplysninger(arbeidsgiverInntektopplysninger: List<ArbeidsgiverInntektsopplysning>) {
        this.arbeidsgiverInntektsopplysninger = arbeidsgiverInntektopplysninger
    }

    override fun preVisitArbeidsgiverInntektsopplysning(
        arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning,
        orgnummer: String
    ) {
        arbeidsgiverInntektsopplysningerPerArbeidsgiver[orgnummer] = arbeidsgiverInntektsopplysning
    }
}
