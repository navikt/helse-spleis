package no.nav.helse.inspectors

import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikkVisitor
import no.nav.helse.økonomi.Inntekt

internal val Sykepengegrunnlag.inspektør get() = SykepengegrunnlagInspektør(this)

internal class SykepengegrunnlagInspektør(sykepengegrunnlag: Sykepengegrunnlag) : VilkårsgrunnlagHistorikkVisitor {
    init {
        sykepengegrunnlag.accept(this)
    }

    lateinit var sykepengegrunnlag: Inntekt

    override fun preVisitSykepengegrunnlag(
        sykepengegrunnlag1: Sykepengegrunnlag,
        sykepengegrunnlag: Inntekt,
        grunnlagForSykepengegrunnlag: Inntekt,
        begrensning: Sykepengegrunnlag.Begrensning,
        deaktiverteArbeidsforhold: List<String>
    ) {
        this.sykepengegrunnlag = sykepengegrunnlag
    }
}