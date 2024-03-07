package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.InntektsopplysningDto
import no.nav.helse.økonomi.Inntekt

internal class IkkeRapportert(
    id: UUID,
    hendelseId: UUID,
    dato: LocalDate,
    tidsstempel: LocalDateTime
) : AvklarbarSykepengegrunnlag(id, hendelseId, dato, Inntekt.INGEN, tidsstempel) {
    internal constructor(dato: LocalDate, hendelseId: UUID, tidsstempel: LocalDateTime) : this(UUID.randomUUID(), hendelseId, dato, tidsstempel)
    override fun avklarSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) =
        takeIf { this.dato == skjæringstidspunkt }

    override fun kanOverstyresAv(ny: Inntektsopplysning) = true

    override fun blirOverstyrtAv(ny: Inntektsopplysning): Inntektsopplysning {
        return ny.overstyrer(this)
    }

    override fun accept(visitor: InntektsopplysningVisitor) {
        visitor.visitIkkeRapportert(this, id, hendelseId, dato, tidsstempel)
    }

    override fun erSamme(other: Inntektsopplysning): Boolean {
        return other is IkkeRapportert && this.dato == other.dato
    }

    override fun dto() =
        InntektsopplysningDto.IkkeRapportertDto(id, hendelseId, dato, tidsstempel)
}