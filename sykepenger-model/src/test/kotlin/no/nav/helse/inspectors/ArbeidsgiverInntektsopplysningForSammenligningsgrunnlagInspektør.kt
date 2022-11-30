package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.person.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagVisitor
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag
import no.nav.helse.person.inntekt.IkkeRapportert
import no.nav.helse.person.inntekt.Infotrygd
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Inntektsopplysning
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.SkattComposite
import no.nav.helse.økonomi.Inntekt

internal val ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag.inspektør get() = ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagInspektør(this)

internal class ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagInspektør(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag) : ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagVisitor {
    internal lateinit var orgnummer: String
        private set
    internal lateinit var inntektsopplysning: Inntektsopplysning
        private set

    init {
        arbeidsgiverInntektsopplysning.accept(this)
    }

    override fun preVisitArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(
        arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag,
        orgnummer: String
    ) {
        this.orgnummer = orgnummer
    }

    override fun visitSaksbehandler(
        saksbehandler: Saksbehandler,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        forklaring: String?,
        subsumsjon: Subsumsjon?,
        tidsstempel: LocalDateTime
    ) {
        this.inntektsopplysning = saksbehandler
    }

    override fun visitInntektsmelding(
        inntektsmelding: Inntektsmelding,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        this.inntektsopplysning = inntektsmelding
    }

    override fun visitIkkeRapportert(id: UUID, dato: LocalDate, tidsstempel: LocalDateTime) {
        this.inntektsopplysning = IkkeRapportert(id, dato, tidsstempel)
    }

    override fun visitInfotrygd(
        infotrygd: Infotrygd,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        this.inntektsopplysning = infotrygd
    }

    override fun preVisitSkatt(skattComposite: SkattComposite, id: UUID, dato: LocalDate) {
        this.inntektsopplysning = skattComposite
    }
}