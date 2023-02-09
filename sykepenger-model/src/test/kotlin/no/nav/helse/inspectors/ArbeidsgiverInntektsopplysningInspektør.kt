package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysningVisitor
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.IkkeRapportert
import no.nav.helse.person.inntekt.Infotrygd
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Inntektsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt

internal val ArbeidsgiverInntektsopplysning.inspektør get() = ArbeidsgiverInntektsopplysningInspektør(this)

internal class ArbeidsgiverInntektsopplysningInspektør(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning) :
    ArbeidsgiverInntektsopplysningVisitor {
    internal lateinit var orgnummer: String
        private set
    internal lateinit var inntektsopplysning: Inntektsopplysning
        private set

    internal lateinit var refusjonsopplysninger: Refusjonsopplysninger
        private set

    init {
        arbeidsgiverInntektsopplysning.accept(this)
    }

    override fun preVisitArbeidsgiverInntektsopplysning(
        arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning,
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

    override fun preVisitSkattSykepengegrunnlag(
        skattSykepengegrunnlag: SkattSykepengegrunnlag,
        id: UUID,
        hendelseId: UUID,
        dato: LocalDate,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        this.inntektsopplysning = skattSykepengegrunnlag
    }

    override fun preVisitRefusjonsopplysninger(refusjonsopplysninger: Refusjonsopplysninger) {
        this.refusjonsopplysninger = refusjonsopplysninger
    }
}