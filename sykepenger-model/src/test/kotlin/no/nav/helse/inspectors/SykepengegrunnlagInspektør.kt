package no.nav.helse.inspectors

import java.time.LocalDate
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikkVisitor
import no.nav.helse.økonomi.Inntekt

internal val Sykepengegrunnlag.inspektør get() = SykepengegrunnlagInspektør(this)

internal class SykepengegrunnlagInspektør(sykepengegrunnlag: Sykepengegrunnlag) : VilkårsgrunnlagHistorikkVisitor {
    init {
        sykepengegrunnlag.accept(this)
    }

    lateinit var sykepengegrunnlag: Inntekt
    lateinit var `6G`: Inntekt

    override fun preVisitSykepengegrunnlag(
        sykepengegrunnlag1: Sykepengegrunnlag,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Inntekt,
        overstyrtGrunnlagForSykepengegrunnlag: Inntekt?,
        grunnlagForSykepengegrunnlag: Inntekt,
        `6G`: Inntekt,
        begrensning: Sykepengegrunnlag.Begrensning,
        deaktiverteArbeidsforhold: List<String>,
        vurdertInfotrygd: Boolean
    ) {
        this.`6G` = `6G`
        this.sykepengegrunnlag = sykepengegrunnlag
    }
}