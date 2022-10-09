package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.person.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.ArbeidsgiverInntektsopplysningVisitor
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.økonomi.Inntekt

internal val ArbeidsgiverInntektsopplysning.inspektør get() = ArbeidsgiverInntektsopplysningInspektør(this)

internal class ArbeidsgiverInntektsopplysningInspektør(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning) : ArbeidsgiverInntektsopplysningVisitor {
    internal lateinit var inntektsopplysning: Inntektshistorikk.Inntektsopplysning
        private set

    init {
        arbeidsgiverInntektsopplysning.accept(this)
    }

    override fun visitSaksbehandler(
        saksbehandler: Inntektshistorikk.Saksbehandler,
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
        inntektsmelding: Inntektshistorikk.Inntektsmelding,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        this.inntektsopplysning = inntektsmelding
    }

    override fun visitIkkeRapportert(id: UUID, dato: LocalDate, tidsstempel: LocalDateTime) {
        this.inntektsopplysning = Inntektshistorikk.IkkeRapportert(id, dato, tidsstempel)
    }

    override fun visitInfotrygd(
        infotrygd: Inntektshistorikk.Infotrygd,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        this.inntektsopplysning = infotrygd
    }

    override fun preVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
        this.inntektsopplysning = skattComposite
    }
}