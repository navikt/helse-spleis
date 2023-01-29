package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagVisitor
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag
import no.nav.helse.person.inntekt.IkkeRapportert
import no.nav.helse.person.inntekt.Inntektsopplysning
import no.nav.helse.person.inntekt.SkattComposite
import no.nav.helse.økonomi.Inntekt

internal val ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag.inspektør get() = ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagInspektør(this)

internal class ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagInspektør(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag) : ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagVisitor {
    internal lateinit var orgnummer: String
        private set
    internal lateinit var inntektsopplysning: Inntektsopplysning
    internal lateinit var rapportertInntekt: Inntekt
        private set

    init {
        arbeidsgiverInntektsopplysning.accept(this)
    }

    override fun preVisitArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(
        arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag,
        orgnummer: String,
        rapportertInntekt: Inntekt
    ) {
        this.orgnummer = orgnummer
        this.rapportertInntekt = rapportertInntekt
    }

    override fun visitIkkeRapportert(id: UUID, dato: LocalDate, tidsstempel: LocalDateTime) {
        this.inntektsopplysning = IkkeRapportert(id, dato, tidsstempel)
    }

    override fun preVisitSkatt(skattComposite: SkattComposite, id: UUID, dato: LocalDate) {
        this.inntektsopplysning = skattComposite
    }
}