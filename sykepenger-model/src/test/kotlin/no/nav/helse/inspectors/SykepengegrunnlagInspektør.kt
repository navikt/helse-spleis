package no.nav.helse.inspectors

import java.time.LocalDate
import no.nav.helse.person.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikkVisitor
import no.nav.helse.økonomi.Inntekt
import kotlin.properties.Delegates

internal val Sykepengegrunnlag.inspektør get() = SykepengegrunnlagInspektør(this)

internal class SykepengegrunnlagInspektør(sykepengegrunnlag: Sykepengegrunnlag) : VilkårsgrunnlagHistorikkVisitor {
    lateinit var minsteinntekt: Inntekt
    var oppfyllerMinsteinntektskrav: Boolean by Delegates.notNull<Boolean>()
    lateinit var sykepengegrunnlag: Inntekt
    lateinit var beregningsgrunnlag: Inntekt
    var skjønnsmessigFastsattÅrsinntekt: Inntekt? = null
    lateinit var `6G`: Inntekt
    lateinit var deaktiverteArbeidsforhold: List<String>
    internal val arbeidsgiverInntektsopplysningerPerArbeidsgiver: MutableMap<String, ArbeidsgiverInntektsopplysning> = mutableMapOf()
    internal lateinit var inntektskilde: Inntektskilde
        private set
    internal var arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning> = listOf()
        private set
    init {
        sykepengegrunnlag.accept(this)
    }

    override fun preVisitSykepengegrunnlag(
        sykepengegrunnlag1: Sykepengegrunnlag,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Inntekt,
        skjønnsmessigFastsattÅrsinntekt: Inntekt?,
        beregningsgrunnlag: Inntekt,
        `6G`: Inntekt,
        begrensning: Sykepengegrunnlag.Begrensning,
        deaktiverteArbeidsforhold: List<String>,
        vurdertInfotrygd: Boolean,
        minsteinntekt: Inntekt,
        oppfyllerMinsteinntektskrav: Boolean
    ) {
        this.minsteinntekt = minsteinntekt
        this.oppfyllerMinsteinntektskrav = oppfyllerMinsteinntektskrav
        this.`6G` = `6G`
        this.sykepengegrunnlag = sykepengegrunnlag
        this.skjønnsmessigFastsattÅrsinntekt = skjønnsmessigFastsattÅrsinntekt
        this.beregningsgrunnlag = beregningsgrunnlag
        this.deaktiverteArbeidsforhold = deaktiverteArbeidsforhold
        this.inntektskilde = sykepengegrunnlag1.inntektskilde()
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