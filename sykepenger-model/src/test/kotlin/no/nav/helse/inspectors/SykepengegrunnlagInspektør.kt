package no.nav.helse.inspectors

import java.time.LocalDate
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikkVisitor
import no.nav.helse.økonomi.Inntekt
import kotlin.properties.Delegates

internal val Sykepengegrunnlag.inspektør get() = SykepengegrunnlagInspektør(this)

internal class SykepengegrunnlagInspektør(sykepengegrunnlag: Sykepengegrunnlag) : VilkårsgrunnlagHistorikkVisitor {
    lateinit var minsteinntekt: Inntekt
    var oppfyllerMinsteinntektskrav: Boolean by Delegates.notNull<Boolean>()
    lateinit var sykepengegrunnlag: Inntekt
    lateinit var maksimalDagsats: Inntekt
    var skjønnsmessigFastsattÅrsinntekt: Inntekt? = null
    lateinit var `6G`: Inntekt

    init {
        sykepengegrunnlag.accept(this)
    }

    override fun preVisitSykepengegrunnlag(
        sykepengegrunnlag1: Sykepengegrunnlag,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Inntekt,
        skjønnsmessigFastsattÅrsinntekt: Inntekt?,
        inntektsgrunnlag: Inntekt,
        maksimalDagsats: Inntekt,
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
        this.maksimalDagsats = maksimalDagsats
        this.skjønnsmessigFastsattÅrsinntekt = skjønnsmessigFastsattÅrsinntekt
    }
}