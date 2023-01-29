package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.person.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagVisitor
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag
import no.nav.helse.person.inntekt.IkkeRapportert
import no.nav.helse.person.inntekt.Inntektsopplysning
import no.nav.helse.person.inntekt.Skatteopplysning
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt

internal val ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag.inspektør get() = ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagInspektør(this)

internal class ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagInspektør(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag) : ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagVisitor {
    internal lateinit var orgnummer: String
        private set
    internal val inntektsopplysning = mutableListOf<Skatteopplysning>()
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

    override fun visitSkatteopplysning(
        skatteopplysning: Skatteopplysning,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: Skatteopplysning.Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime
    ) {
        inntektsopplysning.add(skatteopplysning)
    }
}