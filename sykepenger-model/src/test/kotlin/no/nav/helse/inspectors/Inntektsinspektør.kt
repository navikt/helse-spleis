package no.nav.helse.inspectors

import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class Inntektsinspektør(historikk: Inntektshistorikk) : InntekthistorikkVisitor {
    var inntektTeller = mutableListOf<Int>()

    init {
        historikk.accept(this)
    }

    override fun preVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
        inntektTeller.clear()
    }

    override fun preVisitInnslag(innslag: Inntektshistorikk.Innslag, id: UUID) {
        inntektTeller.add(0)
    }

    override fun visitInntekt(
        inntektsopplysning: Inntektshistorikk.Inntektsopplysning,
        id: UUID,
        fom: LocalDate,
        tidsstempel: LocalDateTime
    ) {
        inntektTeller.add(inntektTeller.removeLast() + 1)
    }

    override fun visitInntektSkatt(
        id: UUID,
        fom: LocalDate,
        måned: YearMonth,
        tidsstempel: LocalDateTime
    ) {
        inntektTeller.add(inntektTeller.removeLast() + 1)
    }

    override fun visitInntektSaksbehandler(
        id: UUID,
        fom: LocalDate,
        tidsstempel: LocalDateTime
    ) {
        inntektTeller.add(inntektTeller.removeLast() + 1)
    }

    override fun visitSaksbehandler(
        saksbehandler: Inntektshistorikk.Saksbehandler,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        inntektTeller.add(inntektTeller.removeLast() + 1)
    }

    override fun visitInntektsmelding(
        inntektsmelding: Inntektshistorikk.Inntektsmelding,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        inntektTeller.add(inntektTeller.removeLast() + 1)
    }

    override fun visitInfotrygd(
        infotrygd: Inntektshistorikk.Infotrygd,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        inntektTeller.add(inntektTeller.removeLast() + 1)
    }

    override fun visitSkattSykepengegrunnlag(
        sykepengegrunnlag: Inntektshistorikk.Skatt.Sykepengegrunnlag,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: Inntektshistorikk.Skatt.Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime
    ) {
        inntektTeller.add(inntektTeller.removeLast() + 1)
    }

    override fun visitSkattSammenligningsgrunnlag(
        sammenligningsgrunnlag: Inntektshistorikk.Skatt.Sammenligningsgrunnlag,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: Inntektshistorikk.Skatt.Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime
    ) {
        inntektTeller.add(inntektTeller.removeLast() + 1)
    }
}
