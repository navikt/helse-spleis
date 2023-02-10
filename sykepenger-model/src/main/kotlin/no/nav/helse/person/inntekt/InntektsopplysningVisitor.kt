package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.person.InntektsmeldingVisitor
import no.nav.helse.økonomi.Inntekt

internal interface InntektsopplysningVisitor : InntektsmeldingVisitor, SkatteopplysningVisitor {
    fun visitSaksbehandler(
        saksbehandler: Saksbehandler,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        forklaring: String?,
        subsumsjon: Subsumsjon?,
        tidsstempel: LocalDateTime
    ) {
    }

    fun visitIkkeRapportert(
        id: UUID,
        dato: LocalDate,
        tidsstempel: LocalDateTime
    ) {
    }

    fun visitInfotrygd(
        infotrygd: Infotrygd,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
    }

    fun preVisitSkattSykepengegrunnlag(
        skattSykepengegrunnlag: SkattSykepengegrunnlag,
        id: UUID,
        hendelseId: UUID,
        dato: LocalDate,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {}

    fun postVisitSkattSykepengegrunnlag(
        skattSykepengegrunnlag: SkattSykepengegrunnlag,
        id: UUID,
        hendelseId: UUID,
        dato: LocalDate,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {}
}